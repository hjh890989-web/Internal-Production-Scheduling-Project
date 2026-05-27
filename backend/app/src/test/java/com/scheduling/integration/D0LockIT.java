package com.scheduling.integration;

import com.scheduling.vc.confirm.D0LockGuard;
import com.scheduling.vc.confirm.D0LockViolationException;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 17 EP-DAY-LOCK IT — BR-V07 D-0 락 강화 (TK-DAY-LOCK-1-3).
 *
 * <p>검증:
 * <ul>
 *   <li>V043 trg_vc_schedule_d0_lock — production_date == today UPDATE 차단</li>
 *   <li>override 경로 (override_reason+by 갱신) → 통과 (BR-V07 정합)</li>
 *   <li>D-1 UPDATE → 통과 (D-0 만 락)</li>
 *   <li>D0LockGuard.enforce — 친화 메시지 D0LockViolationException</li>
 *   <li>D0LockGuard.isLocked — boundary</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class D0LockIT {

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

    @Autowired private VcScheduleRepository repository;
    @Autowired private D0LockGuard guard;
    @Autowired private JdbcTemplate jdbc;

    private static final Instant T0 = Instant.parse("2026-05-27T00:00:00Z");

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM app.vc_schedule");
    }

    /**
     * D-0 row 직접 INSERT — V041 D-2 hard trigger 우회 (BEFORE INSERT). SQL UPDATE 로 강제 set
     * 하면 V041 confirmed_immutable 등 다른 trigger 영향 — 본 IT 는 INSERT 시점 trigger 회피
     * 위해 jdbc 로 직접 INSERT 후 UPDATE 시도.
     *
     * <p>V041 trigger 는 INSERT 시 차단 — 따라서 D-0 row 를 직접 만들기 어려움. trigger 일시 비활성
     * + INSERT + 재활성 패턴 사용.
     */
    private UUID insertD0Row() {
        // V041 D-2 hard trigger 일시 비활성 → D-0 row INSERT → trigger 복구
        jdbc.update("ALTER TABLE app.vc_schedule DISABLE TRIGGER trg_vc_schedule_d2_hard");
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO app.vc_schedule (vc_schedule_id, hose_id, machine_id, slot_position,
                production_date, rotation_no, angle_id, planned_qty, status, linked_order_ids,
                created_at, updated_at)
            VALUES (?, '29673-2R060', 'LP-01', 1, CURRENT_DATE, 5, 'ANG-D0', 100,
                'CANDIDATE', '', now(), now())
            """, id);
        jdbc.update("ALTER TABLE app.vc_schedule ENABLE TRIGGER trg_vc_schedule_d2_hard");
        return id;
    }

    private UUID insertD1Row() {
        UUID id = UUID.randomUUID();
        VcSchedule s = new VcSchedule(id, "29673-2R060", "LP-02", (short) 1,
            LocalDate.now().plusDays(1), (short) 5,
            "ANG-D1", 100, VcScheduleStatus.CANDIDATE, "", T0, T0);
        // D-1 (gap=1) 은 V041 D-2 hard trigger 차단 → 우회 INSERT
        jdbc.update("ALTER TABLE app.vc_schedule DISABLE TRIGGER trg_vc_schedule_d2_hard");
        repository.saveAndFlush(s);
        jdbc.update("ALTER TABLE app.vc_schedule ENABLE TRIGGER trg_vc_schedule_d2_hard");
        return id;
    }

    // =========================================================================
    // V043 DB trigger
    // =========================================================================

    @Test
    @DisplayName("BR-V07 — D-0 row planned_qty UPDATE → trg_vc_schedule_d0_lock 차단")
    void d0_planned_qty_update_blocked() {
        UUID id = insertD0Row();
        assertThatThrownBy(() -> jdbc.update(
            "UPDATE app.vc_schedule SET planned_qty = 200 WHERE vc_schedule_id = ?", id))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("BR-V07 D-0 (당일) 락");
    }

    @Test
    @DisplayName("BR-V07 — D-0 row override 경로 (override_reason+by 갱신) → 통과")
    void d0_override_path_allowed() {
        UUID id = insertD0Row();
        assertThatCode(() -> jdbc.update(
            "UPDATE app.vc_schedule SET override_reason = '일중 앵글 교체', override_by = '00000001' "
                + "WHERE vc_schedule_id = ?", id))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("BR-V07 — D-1 (내일) row planned_qty UPDATE → 통과 (D-0 만 락)")
    void d1_update_allowed() {
        UUID id = insertD1Row();
        assertThatCode(() -> jdbc.update(
            "UPDATE app.vc_schedule SET planned_qty = 200 WHERE vc_schedule_id = ?", id))
            .doesNotThrowAnyException();
    }

    // =========================================================================
    // D0LockGuard service-level
    // =========================================================================

    @Test
    @DisplayName("D0LockGuard.isLocked — D-0 true / D-1 false / 과거 false")
    void guard_is_locked_boundary() {
        org.assertj.core.api.Assertions.assertThat(guard.isLocked(LocalDate.now())).isTrue();
        org.assertj.core.api.Assertions.assertThat(guard.isLocked(LocalDate.now().plusDays(1))).isFalse();
        org.assertj.core.api.Assertions.assertThat(guard.isLocked(LocalDate.now().minusDays(1))).isFalse();
        org.assertj.core.api.Assertions.assertThat(guard.isLocked(null)).isFalse();
    }

    @Test
    @DisplayName("D0LockGuard.enforce — D-0 + non-override 시 D0LockViolationException")
    void guard_enforce_d0_throws() {
        assertThatThrownBy(() -> guard.enforce(LocalDate.now(), false))
            .isInstanceOf(D0LockViolationException.class)
            .hasMessageContaining("BR-V07 D-0 (당일) 락 위반")
            .hasMessageContaining("override_reason+override_by");
    }

    @Test
    @DisplayName("D0LockGuard.enforce — D-0 + override 경로 → 통과")
    void guard_enforce_d0_override_allowed() {
        assertThatCode(() -> guard.enforce(LocalDate.now(), true))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("D0LockGuard.enforce — D-1 + non-override → 통과 (D-0 만 락)")
    void guard_enforce_d1_allowed() {
        assertThatCode(() -> guard.enforce(LocalDate.now().plusDays(1), false))
            .doesNotThrowAnyException();
    }
}
