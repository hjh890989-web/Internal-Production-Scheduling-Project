package com.scheduling.integration;

import com.scheduling.ex.confirm.ExCandidateConfirmationService;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
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
 * EP-10 ST-10-2 IT — EX candidate Confirm 게이트 (BR-X01).
 *
 * <p>검증:
 * <ul>
 *   <li>SCHEDULED → CONFIRMED 정상 전이</li>
 *   <li>DB trigger {@code trg_ex_candidate_transition} → PENDING → CONFIRMED 차단</li>
 *   <li>DB trigger → CONFIRMED 전이 시 confirmed_at/by 누락 reject</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExConfirmGateIT {

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

    @Autowired private ExCandidateConfirmationService service;
    @Autowired private ExScheduleCandidateRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private static final LocalDate VC_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate EX_DEADLINE = LocalDate.of(2026, 5, 29);
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private ExScheduleCandidate saveCandidate(CandidateStatus status) {
        ExScheduleCandidate c = new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            UUID.randomUUID(), VC_DATE, EX_DEADLINE, 2531,
            status, T0, T0);
        return repository.save(c);
    }

    @Test
    @DisplayName("Planner confirm SCHEDULED → CONFIRMED + audit 필드 set")
    void planner_confirm_scheduled_success() {
        ExScheduleCandidate c = saveCandidate(CandidateStatus.SCHEDULED);

        service.confirm(c.getExCandidateId(), "planner-001");

        ExScheduleCandidate reloaded = repository.findById(c.getExCandidateId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CandidateStatus.CONFIRMED);
        assertThat(reloaded.getConfirmedBy()).isEqualTo("planner-001");
        assertThat(reloaded.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("DB trigger — PENDING → CONFIRMED 직접 update 차단")
    void direct_db_pending_to_confirmed_blocked() {
        ExScheduleCandidate c = saveCandidate(CandidateStatus.PENDING);
        UUID id = c.getExCandidateId();

        assertThatThrownBy(() -> jdbc.update(
            "UPDATE app.ex_schedule_candidate "
                + "SET status = 'CONFIRMED', confirmed_at = NOW(), confirmed_by = 'x' "
                + "WHERE ex_candidate_id = ?", id))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("invalid EX candidate transition");
    }

    @Test
    @DisplayName("DB trigger — SCHEDULED → CONFIRMED 인데 confirmed_at/by 누락 reject")
    void direct_db_confirmed_without_audit_blocked() {
        ExScheduleCandidate c = saveCandidate(CandidateStatus.SCHEDULED);
        UUID id = c.getExCandidateId();

        assertThatThrownBy(() -> jdbc.update(
            "UPDATE app.ex_schedule_candidate SET status = 'CONFIRMED' WHERE ex_candidate_id = ?", id))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("confirmed_at + confirmed_by 필수");
    }

    @Test
    @DisplayName("PENDING 상태 service 확정 시도 → IllegalStateException")
    void service_pending_rejects_confirm() {
        ExScheduleCandidate c = saveCandidate(CandidateStatus.PENDING);

        assertThatThrownBy(() -> service.confirm(c.getExCandidateId(), "planner-001"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SCHEDULED");
    }
}
