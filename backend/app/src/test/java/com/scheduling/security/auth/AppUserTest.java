package com.scheduling.security.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 10 EP-AUTH AppUser 도메인 불변 단위 test (NFR-SEC-007 정합).
 */
class AppUserTest {

    // BCrypt 표준 60 char: "$2a$12$" (7) + 22 salt + 31 hash = 60
    private static final String BCRYPT_SAMPLE_60 =
        "$2a$12$01234567890123456789012345678901234567890123456789012";

    @Test
    @DisplayName("employee_id 8자리 숫자 + pin_hash 60 char + role 정상 시 생성 OK")
    void happy_path() {
        AppUser user = new AppUser("12345678", BCRYPT_SAMPLE_60, AppUser.Role.PLANNER);

        assertThat(user.getEmployeeId()).isEqualTo("12345678");
        assertThat(user.getPinHash()).isEqualTo(BCRYPT_SAMPLE_60);
        assertThat(user.getRole()).isEqualTo(AppUser.Role.PLANNER);
        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("employee_id 7자리 — IllegalArgumentException")
    void short_employee_id_rejected() {
        assertThatThrownBy(() -> new AppUser("1234567", BCRYPT_SAMPLE_60, AppUser.Role.PLANNER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사번 8자리");
    }

    @Test
    @DisplayName("employee_id 알파벳 포함 — IllegalArgumentException (regex enforce)")
    void alpha_employee_id_rejected() {
        assertThatThrownBy(() -> new AppUser("1234567A", BCRYPT_SAMPLE_60, AppUser.Role.PLANNER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사번 8자리");
    }

    @Test
    @DisplayName("pin_hash null/짧음 — IllegalArgumentException")
    void invalid_pin_hash_rejected() {
        assertThatThrownBy(() -> new AppUser("12345678", null, AppUser.Role.PLANNER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BCrypt 60 char");

        assertThatThrownBy(() -> new AppUser("12345678", "short", AppUser.Role.PLANNER))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("role null — IllegalArgumentException")
    void null_role_rejected() {
        assertThatThrownBy(() -> new AppUser("12345678", BCRYPT_SAMPLE_60, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("role");
    }

    @Test
    @DisplayName("recordFailure → failedAttempts +1, 누적 가능")
    void record_failure_increments() {
        AppUser user = new AppUser("12345678", BCRYPT_SAMPLE_60, AppUser.Role.STK_USER);

        user.recordFailure();
        user.recordFailure();
        user.recordFailure();

        assertThat(user.getFailedAttempts()).isEqualTo((short) 3);
    }

    @Test
    @DisplayName("recordSuccess → failedAttempts 0 + lockedUntil null (잠금 해제)")
    void record_success_resets() {
        AppUser user = new AppUser("12345678", BCRYPT_SAMPLE_60, AppUser.Role.PLANNER);
        user.recordFailure();
        user.recordFailure();
        user.lock(Instant.now().plusSeconds(600));

        user.recordSuccess();

        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("lock(until) — 정상 + null 거부")
    void lock_validation() {
        AppUser user = new AppUser("12345678", BCRYPT_SAMPLE_60, AppUser.Role.PLANNER);
        Instant future = Instant.now().plusSeconds(600);

        user.lock(future);
        assertThat(user.getLockedUntil()).isEqualTo(future);

        assertThatThrownBy(() -> user.lock(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isLocked — null/과거/미래 3 cases")
    void is_locked_three_cases() {
        AppUser user = new AppUser("12345678", BCRYPT_SAMPLE_60, AppUser.Role.PLANNER);
        Instant now = Instant.parse("2026-05-27T10:00:00Z");

        // null → false
        assertThat(user.isLocked(now)).isFalse();

        // 과거 → false (잠금 만료)
        user.lock(Instant.parse("2026-05-27T09:50:00Z"));
        assertThat(user.isLocked(now)).isFalse();

        // 미래 → true (잠금 활성)
        user.lock(Instant.parse("2026-05-27T10:10:00Z"));
        assertThat(user.isLocked(now)).isTrue();
    }

    @Test
    @DisplayName("changePin → 신 hash + failedAttempts/lockedUntil reset")
    void change_pin_resets_lock() {
        AppUser user = new AppUser("12345678", BCRYPT_SAMPLE_60, AppUser.Role.PLANNER);
        user.recordFailure();
        user.recordFailure();
        user.lock(Instant.now().plusSeconds(600));

        String newHash = "$2a$12$98765432109876543210987654321098765432109876543210987";
        user.changePin(newHash);

        assertThat(user.getPinHash()).isEqualTo(newHash);
        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }
}
