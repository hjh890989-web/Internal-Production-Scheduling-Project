package com.scheduling.integration;

import com.scheduling.vc.confirm.VcScheduleConfirmationService;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
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
 * EP-10 ST-10-1 IT — VC Confirm 게이트 (BR-X01).
 *
 * <p>검증:
 * <ul>
 *   <li>VcScheduleConfirmationService.confirm → CANDIDATE → CONFIRMED 정상 + audit 필드</li>
 *   <li>DB trigger {@code trg_vc_schedule_transition} → CANDIDATE → DONE 직접 update 차단</li>
 *   <li>DB trigger → CONFIRMED 전이 시 confirmed_at/by 누락 시 reject</li>
 *   <li>이미 CONFIRMED 인 row 재확정 시도 → IllegalStateException</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VcConfirmGateIT {

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

    @Autowired private VcScheduleConfirmationService service;
    @Autowired private VcScheduleRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private static final LocalDate PROD = LocalDate.of(2026, 6, 1);
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private VcSchedule saveCandidate(String hose, String machine, short slot, short rot) {
        VcSchedule s = new VcSchedule(UUID.randomUUID(), hose, machine, slot, PROD, rot,
            "ANG-A", 100, VcScheduleStatus.CANDIDATE, "", T0, T0);
        return repository.save(s);
    }

    @Test
    @DisplayName("Planner confirm → CANDIDATE → CONFIRMED + audit 필드 set")
    void planner_confirm_success() {
        VcSchedule s = saveCandidate("29673-2R060", "LP-01", (short) 1, (short) 5);

        service.confirm(s.getVcScheduleId(), "planner-001");

        VcSchedule reloaded = repository.findById(s.getVcScheduleId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(VcScheduleStatus.CONFIRMED);
        assertThat(reloaded.getConfirmedBy()).isEqualTo("planner-001");
        assertThat(reloaded.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("DB trigger — CANDIDATE → DONE 직접 update 차단")
    void direct_db_candidate_to_done_blocked() {
        VcSchedule s = saveCandidate("29673-2R060", "LP-01", (short) 2, (short) 5);
        UUID id = s.getVcScheduleId();

        assertThatThrownBy(() -> jdbc.update(
            "UPDATE app.vc_schedule SET status = 'DONE' WHERE vc_schedule_id = ?", id))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("invalid VC status transition");
    }

    @Test
    @DisplayName("DB trigger — CONFIRMED 전이 시 confirmed_at/by 누락 reject")
    void direct_db_confirmed_without_audit_blocked() {
        VcSchedule s = saveCandidate("29673-2R060", "LP-01", (short) 3, (short) 5);
        UUID id = s.getVcScheduleId();

        assertThatThrownBy(() -> jdbc.update(
            "UPDATE app.vc_schedule SET status = 'CONFIRMED' WHERE vc_schedule_id = ?", id))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("confirmed_at + confirmed_by 필수");
    }

    @Test
    @DisplayName("이미 CONFIRMED 인 row 재확정 시도 → IllegalStateException")
    void already_confirmed_rejects_reconfirm() {
        VcSchedule s = saveCandidate("29673-2R060", "LP-01", (short) 4, (short) 5);
        service.confirm(s.getVcScheduleId(), "planner-001");

        assertThatThrownBy(() -> service.confirm(s.getVcScheduleId(), "planner-002"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BR-X01");
    }

    @Test
    @DisplayName("Batch 확정 — N rows + 이벤트 발행 (rows 수 반환)")
    void batch_confirm_returns_count() {
        VcSchedule a = saveCandidate("29673-2R060", "LP-01", (short) 5, (short) 5);
        VcSchedule b = saveCandidate("29673-2R060", "LP-02", (short) 1, (short) 5);
        UUID batchId = UUID.randomUUID();

        int count = service.confirmBatch(
            java.util.List.of(a.getVcScheduleId(), b.getVcScheduleId()),
            "planner-001", batchId);

        assertThat(count).isEqualTo(2);
        assertThat(repository.findById(a.getVcScheduleId()).orElseThrow().getStatus())
            .isEqualTo(VcScheduleStatus.CONFIRMED);
        assertThat(repository.findById(b.getVcScheduleId()).orElseThrow().getStatus())
            .isEqualTo(VcScheduleStatus.CONFIRMED);
    }
}
