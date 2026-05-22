package com.scheduling.integration;

import com.scheduling.vc.domain.RotationSlot;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import com.scheduling.vc.override.IntraDayOverrideService;
import com.scheduling.vc.rule.IntraDayLockRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EP-13 ST-13-1+2+4 IT — 일중 락 + RuleEngine + override (BR-V07).
 *
 * <p>검증:
 * <ul>
 *   <li>DB trigger {@code trg_vc_intra_day_lock} — 같은 (machine,slot,date) 다른 angle INSERT 차단</li>
 *   <li>{@link IntraDayLockRule} — Allocator pre-check 로 다른 angle 검출</li>
 *   <li>override_reason + override_by 비-NULL → DB 통과 (사용자 명시 override)</li>
 *   <li>{@link IntraDayOverrideService} — applyOverride + audit 자동 발행</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntraDayLockIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
    }

    @Autowired private IntraDayLockRule rule;
    @Autowired private IntraDayOverrideService overrideService;
    @Autowired private VcScheduleRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private static final LocalDate PROD = LocalDate.of(2026, 6, 2);
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private VcSchedule save(String angleId, short rotation) {
        VcSchedule s = new VcSchedule(UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, rotation, angleId, 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);
        return repository.save(s);
    }

    @Test
    @DisplayName("같은 (machine,slot,date) 동일 angle 추가 → IntraDayLockRule pass + DB 통과")
    void same_angle_passes() {
        save("ANG-A", (short) 5);

        RotationSlot slot = new RotationSlot(PROD, "LP-01", 6, 1);
        assertThat(rule.validate(slot, "ANG-A")).isTrue();

        save("ANG-A", (short) 6);     // 정상 INSERT
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("RuleEngine — 다른 angle 검출 → false (Allocator pre-check)")
    void rule_engine_detects_different_angle() {
        save("ANG-A", (short) 5);

        RotationSlot slot = new RotationSlot(PROD, "LP-01", 6, 1);
        assertThat(rule.validate(slot, "ANG-B")).isFalse();
    }

    @Test
    @DisplayName("DB trigger — 다른 angle INSERT (override_reason 없음) → reject")
    void db_trigger_blocks_different_angle_without_override() {
        save("ANG-A", (short) 5);

        VcSchedule s2 = new VcSchedule(UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 6, "ANG-B", 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);

        assertThatThrownBy(() -> repository.save(s2))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("일중 앵글 교체 차단");
    }

    @Test
    @DisplayName("DB trigger — override_reason + override_by 비-NULL → 통과 (override)")
    void db_trigger_passes_with_override() {
        save("ANG-A", (short) 5);

        // 같은 slot 의 또 다른 row 를 다른 angle 로 + override 사유 입력
        VcSchedule s2 = new VcSchedule(UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 6, "ANG-B", 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);
        s2.applyOverride("긴급 LOT 변경", "planner-001", T0);

        VcSchedule saved = repository.save(s2);
        assertThat(saved.getOverrideReason()).isEqualTo("긴급 LOT 변경");
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("OverrideService.applyOverride — DB trigger 통과 + audit reason 캡쳐")
    void override_service_captures_audit() {
        save("ANG-A", (short) 5);

        // 같은 slot 에 다른 angle row 를 직접 추가 (override 사유 입력 후)
        VcSchedule second = new VcSchedule(UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 6, "ANG-B", 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);
        second.applyOverride("긴급", "planner-001", T0);
        repository.save(second);

        // 이제 second 에 다른 사유로 OverrideService 호출
        overrideService.applyOverride(second.getVcScheduleId(), "재계획 사유", "planner-002");

        VcSchedule reloaded = repository.findById(second.getVcScheduleId()).orElseThrow();
        assertThat(reloaded.getOverrideReason()).isEqualTo("재계획 사유");
        assertThat(reloaded.getOverrideBy()).isEqualTo("planner-002");

        // audit row 확인 — @Auditable reason 캡쳐
        String reason = jdbc.queryForObject(
            "SELECT reason FROM audit.schedule_audit_log "
                + "WHERE row_pk = ? AND action = 'UPDATE' "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class, second.getVcScheduleId().toString());
        assertThat(reason).contains("BR-V07");
    }
}
