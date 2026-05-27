package com.scheduling.integration;

import com.scheduling.vc.confirm.D2HardConstraintException;
import com.scheduling.vc.confirm.D2HardConstraintGuard;
import com.scheduling.vc.confirm.DualReviewConflictException;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 16 EP-CONFIRM IT — BR-X07 D-2 hard + BR-X05 dual-review (TK-CONFIRM-1-3 + 2-3 + 6-1).
 *
 * <p>검증:
 * <ul>
 *   <li>V041 trg_vc_schedule_d2_hard — production_date - now &lt; 2 일 INSERT 차단</li>
 *   <li>D2HardConstraintGuard.enforce() — 친화 한국어 메시지 throw</li>
 *   <li>BR-X05 — createdBy == plannerId 시 confirm 거부</li>
 *   <li>BR-X05 — createdBy ≠ plannerId 시 confirm 정상</li>
 *   <li>V041 CONFIRMED immutable — CONFIRMED row 의 planned_qty UPDATE 차단</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConfirmGateD2DualReviewIT {

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
    @Autowired private D2HardConstraintGuard d2Guard;
    @Autowired private VcScheduleRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private static final Instant T0 = Instant.parse("2026-05-27T00:00:00Z");

    @BeforeEach
    void clean() {
        // V039 sample row + 본 IT 누적 데이터 삭제
        jdbc.update("DELETE FROM app.vc_schedule");
    }

    private VcSchedule buildSchedule(LocalDate productionDate, short slot, short rot) {
        return new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            slot, productionDate, rot,
            "ANG-X07", 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);
    }

    // =========================================================================
    // BR-X07 D-2 hard (TK-CONFIRM-1-3) — DB trigger
    // =========================================================================

    @Test
    @DisplayName("BR-X07 — production_date = today+3 (D-3) INSERT 통과")
    void d3_insert_allowed() {
        VcSchedule s = buildSchedule(LocalDate.now().plusDays(3), (short) 1, (short) 5);
        assertThatCode(() -> repository.save(s)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BR-X07 — production_date = today+10 (D-10 far future) INSERT 통과")
    void d10_insert_allowed() {
        VcSchedule s = buildSchedule(LocalDate.now().plusDays(10), (short) 2, (short) 5);
        assertThatCode(() -> repository.save(s)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BR-X07 — production_date = today+1 (D-1) INSERT 차단 (trigger RAISE)")
    void d1_insert_blocked_by_trigger() {
        VcSchedule s = buildSchedule(LocalDate.now().plusDays(1), (short) 3, (short) 5);
        assertThatThrownBy(() -> {
            repository.save(s);
            repository.flush();
        })
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("BR-X07 D-2 hard");
    }

    @Test
    @DisplayName("BR-X07 — production_date = today (D-0) INSERT 차단")
    void d0_insert_blocked_by_trigger() {
        VcSchedule s = buildSchedule(LocalDate.now(), (short) 4, (short) 5);
        assertThatThrownBy(() -> {
            repository.save(s);
            repository.flush();
        })
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("BR-X07 D-2 hard");
    }

    // =========================================================================
    // BR-X07 service-level guard (TK-CONFIRM-1-2)
    // =========================================================================

    @Test
    @DisplayName("D2HardConstraintGuard.enforce — D-1 production 친화 메시지 throw")
    void guard_d1_throws_friendly_message() {
        assertThatThrownBy(() -> d2Guard.enforce(LocalDate.now().plusDays(1)))
            .isInstanceOf(D2HardConstraintException.class)
            .hasMessageContaining("BR-X07 D-2 hard 제약 위반")
            .hasMessageContaining("D-2 (2일) 이상만 신규 추가 가능");
    }

    @Test
    @DisplayName("D2HardConstraintGuard.enforceUpdate — D-0 (오늘) UPDATE 차단")
    void guard_update_d0_blocked() {
        assertThatThrownBy(() -> d2Guard.enforceUpdate(LocalDate.now()))
            .isInstanceOf(D2HardConstraintException.class)
            .hasMessageContaining("BR-X07 D-2 hard 제약 위반");
    }

    @Test
    @DisplayName("D2HardConstraintGuard.enforceUpdate — D-1 (내일) UPDATE 통과 (BR-X01 게이트)")
    void guard_update_d1_allowed() {
        assertThatCode(() -> d2Guard.enforceUpdate(LocalDate.now().plusDays(1)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("D2HardConstraintGuard.enforceUpdate — 과거 production_date 차단")
    void guard_update_past_blocked() {
        assertThatThrownBy(() -> d2Guard.enforceUpdate(LocalDate.now().minusDays(1)))
            .isInstanceOf(D2HardConstraintException.class);
    }

    @Test
    @DisplayName("D2HardConstraintGuard.fits — D-3 true / D-1 false")
    void guard_fits_boundary() {
        assertThat(d2Guard.fits(LocalDate.now().plusDays(3))).isTrue();
        assertThat(d2Guard.fits(LocalDate.now().plusDays(2))).isTrue();
        assertThat(d2Guard.fits(LocalDate.now().plusDays(1))).isFalse();
        assertThat(d2Guard.fits(LocalDate.now())).isFalse();
    }

    // =========================================================================
    // BR-X05 dual-review (TK-CONFIRM-2-3)
    // =========================================================================

    @Test
    @DisplayName("BR-X05 — createdBy == plannerId 시 confirm 거부 (작성자 ≠ 승인자 강제)")
    void same_actor_confirm_rejected() {
        VcSchedule s = buildSchedule(LocalDate.now().plusDays(5), (short) 5, (short) 5);
        s.assignCreatedBy("00000001");
        repository.save(s);

        assertThatThrownBy(() -> confirmationService.confirm(s.getVcScheduleId(), "00000001"))
            .isInstanceOf(DualReviewConflictException.class)
            .hasMessageContaining("BR-X05 dual-review")
            .hasMessageContaining("00000001");
    }

    @Test
    @DisplayName("BR-X05 — createdBy ≠ plannerId 시 confirm 통과 + CONFIRMED 전이")
    void different_actor_confirm_succeeds() {
        VcSchedule s = buildSchedule(LocalDate.now().plusDays(5), (short) 6, (short) 5);
        s.assignCreatedBy("00000001");
        repository.save(s);

        confirmationService.confirm(s.getVcScheduleId(), "00000002");

        VcSchedule reloaded = repository.findById(s.getVcScheduleId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(VcScheduleStatus.CONFIRMED);
        assertThat(reloaded.getConfirmedBy()).isEqualTo("00000002");
        assertThat(reloaded.getCreatedBy()).isEqualTo("00000001");
    }

    @Test
    @DisplayName("BR-X05 — createdBy = NULL (legacy row) 시 confirm 통과 (Sprint 16 이전 데이터)")
    void legacy_null_created_by_allows_confirm() {
        VcSchedule s = buildSchedule(LocalDate.now().plusDays(5), (short) 7, (short) 5);
        repository.save(s);     // createdBy 미설정 → NULL

        assertThatCode(() -> confirmationService.confirm(s.getVcScheduleId(), "00000001"))
            .doesNotThrowAnyException();
        assertThat(repository.findById(s.getVcScheduleId()).orElseThrow().getStatus())
            .isEqualTo(VcScheduleStatus.CONFIRMED);
    }

    // =========================================================================
    // BR-X01 CONFIRMED immutable (V041 trg_vc_schedule_confirmed_immutable)
    // =========================================================================

    @Test
    @DisplayName("BR-X01 — CONFIRMED row 의 planned_qty UPDATE → trigger 차단")
    void confirmed_planned_qty_update_blocked() {
        VcSchedule s = buildSchedule(LocalDate.now().plusDays(5), (short) 8, (short) 5);
        s.assignCreatedBy("00000001");
        repository.save(s);
        confirmationService.confirm(s.getVcScheduleId(), "00000002");
        UUID id = s.getVcScheduleId();

        assertThatThrownBy(() -> jdbc.update(
            "UPDATE app.vc_schedule SET planned_qty = 999 WHERE vc_schedule_id = ?", id))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("BR-X01 CONFIRMED 스케줄 immutable");
    }
}
