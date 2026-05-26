package com.scheduling.security.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sprint 10 EP-AUTH AppUserDetailsService 단위 test (TK-AUTH-2-4).
 *
 * <p>3 cases — 정상 lookup + 미존재 사번 + 잠긴 사용자 (accountLocked flag).
 */
class AppUserDetailsServiceTest {

    private static final String BCRYPT_60 =
        "$2a$12$01234567890123456789012345678901234567890123456789012";
    private static final Instant NOW = Instant.parse("2026-05-27T10:00:00Z");
    private final Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("정상 — findByEmployeeId 후 UserDetails 반환 + ROLE_PLANNER 부착 + 미잠금")
    void load_existing_user_returns_user_details() {
        AppUserRepository repo = mock(AppUserRepository.class);
        AppUser user = new AppUser("12345678", BCRYPT_60, AppUser.Role.PLANNER);
        when(repo.findByEmployeeId(eq("12345678"))).thenReturn(Optional.of(user));

        AppUserDetailsService service = new AppUserDetailsService(repo, fixedClock);
        UserDetails details = service.loadUserByUsername("12345678");

        assertThat(details.getUsername()).isEqualTo("12345678");
        assertThat(details.getPassword()).isEqualTo(BCRYPT_60);
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_PLANNER");
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("미존재 사번 — UsernameNotFoundException")
    void load_missing_user_throws() {
        AppUserRepository repo = mock(AppUserRepository.class);
        when(repo.findByEmployeeId(eq("99999999"))).thenReturn(Optional.empty());

        AppUserDetailsService service = new AppUserDetailsService(repo, fixedClock);

        assertThatThrownBy(() -> service.loadUserByUsername("99999999"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("사번 미존재");
    }

    @Test
    @DisplayName("잠긴 사용자 (locked_until 미래) — accountLocked=true (DaoAuthenticationProvider LockedException 진입점)")
    void load_locked_user_marks_locked() {
        AppUserRepository repo = mock(AppUserRepository.class);
        AppUser user = new AppUser("12345678", BCRYPT_60, AppUser.Role.STK_USER);
        user.lock(NOW.plusSeconds(600));   // 10분 후 해제 예정 (현재 시점 잠금 활성)
        when(repo.findByEmployeeId(eq("12345678"))).thenReturn(Optional.of(user));

        AppUserDetailsService service = new AppUserDetailsService(repo, fixedClock);
        UserDetails details = service.loadUserByUsername("12345678");

        assertThat(details.isAccountNonLocked()).isFalse();
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_STK_USER");
    }
}
