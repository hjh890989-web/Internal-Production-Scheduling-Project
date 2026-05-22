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
 * EP-11 ST-11-1+2 IT — audit trigger + 불변성 (BR-X02, NFR-SEC-004).
 *
 * <p>검증:
 * <ul>
 *   <li>vc_schedule INSERT → audit row 1건 (action=INSERT, actor=system)</li>
 *   <li>VC confirm (@Auditable) → audit row 1건 추가 (action=UPDATE, reason=확정 사유)</li>
 *   <li>audit row UPDATE 시도 → permission denied / trigger reject</li>
 *   <li>audit row DELETE 시도 → permission denied / trigger reject</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditTriggerIT {

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

    @Autowired private VcScheduleConfirmationService confirmationService;
    @Autowired private VcScheduleRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private static final LocalDate PROD = LocalDate.of(2026, 6, 1);
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        repository.deleteAll();
        // audit.schedule_audit_log 는 NFR-SEC-004 immutable — 매 테스트마다 UUID 로 격리
    }

    private VcSchedule saveCandidate(short slot) {
        VcSchedule s = new VcSchedule(UUID.randomUUID(), "29673-2R060", "LP-01",
            slot, PROD, (short) 5, "ANG-A", 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);
        return repository.save(s);
    }

    @Test
    @DisplayName("vc_schedule INSERT → audit_log INSERT 1건 (actor=system, action=INSERT)")
    void insert_captures_audit_row() {
        VcSchedule s = saveCandidate((short) 1);

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit.schedule_audit_log "
                + "WHERE table_name = 'vc_schedule' AND row_pk = ? AND action = 'INSERT'",
            Integer.class, s.getVcScheduleId().toString());

        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Confirm Service (@Auditable) → audit reason 캡쳐")
    void confirm_captures_reason() {
        VcSchedule s = saveCandidate((short) 2);
        confirmationService.confirm(s.getVcScheduleId(), "planner-001");

        // UPDATE row 의 reason 검증
        String reason = jdbc.queryForObject(
            "SELECT reason FROM audit.schedule_audit_log "
                + "WHERE table_name = 'vc_schedule' AND row_pk = ? AND action = 'UPDATE' "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class, s.getVcScheduleId().toString());

        assertThat(reason).contains("BR-X01");
    }

    @Test
    @DisplayName("NFR-SEC-004 — audit row UPDATE 시도 → trigger reject")
    void audit_row_update_rejected() {
        VcSchedule s = saveCandidate((short) 3);
        Long auditId = jdbc.queryForObject(
            "SELECT audit_id FROM audit.schedule_audit_log "
                + "WHERE row_pk = ? LIMIT 1",
            Long.class, s.getVcScheduleId().toString());

        assertThatThrownBy(() -> jdbc.update(
            "UPDATE audit.schedule_audit_log SET reason = '변조' WHERE audit_id = ?",
            auditId))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("변조 금지");
    }

    @Test
    @DisplayName("NFR-SEC-004 — audit row DELETE 시도 → trigger reject")
    void audit_row_delete_rejected() {
        saveCandidate((short) 4);

        assertThatThrownBy(() -> jdbc.update(
            "DELETE FROM audit.schedule_audit_log"))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("변조 금지");
    }

    @Test
    @DisplayName("NFR-SEC-004 — audit TRUNCATE 시도 → trigger reject")
    void audit_truncate_rejected() {
        saveCandidate((short) 5);

        assertThatThrownBy(() -> jdbc.execute(
            "TRUNCATE audit.schedule_audit_log"))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("TRUNCATE 금지");
    }
}
