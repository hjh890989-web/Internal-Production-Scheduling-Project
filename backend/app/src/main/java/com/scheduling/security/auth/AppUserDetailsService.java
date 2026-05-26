package com.scheduling.security.auth;

import com.scheduling.security.RoleConstants;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/**
 * Sprint 10 EP-AUTH — Spring Security {@link UserDetailsService} 구현 (NFR-SEC-007).
 *
 * <p>{@link AppUserRepository} 에서 사번 lookup → Spring Security 의 {@link User} (Builder) 로 변환.
 * 잠금 상태 ({@code locked_until > now}) 는 {@link User#accountLocked} flag 로 매핑 →
 * {@code DaoAuthenticationProvider} 가 자동으로 {@code LockedException} 발생.
 *
 * <p>역할은 {@link RoleConstants#ROLE_PREFIX} 부착 ({@code ROLE_PLANNER} 형태) — Spring Security
 * {@code @PreAuthorize("hasRole('PLANNER')")} 정합.
 *
 * <p>{@link Clock} 주입 — ArchUnit {@code KstTimezoneArchTest} 정합 (BR-X04).
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;
    private final Clock clock;

    public AppUserDetailsService(AppUserRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public UserDetails loadUserByUsername(String employeeId) throws UsernameNotFoundException {
        AppUser user = repository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new UsernameNotFoundException(
                "사번 미존재: " + employeeId));

        return User.builder()
            .username(user.getEmployeeId())
            .password(user.getPinHash())
            .authorities(List.of(
                new SimpleGrantedAuthority(RoleConstants.ROLE_PREFIX + user.getRole().name())))
            .accountLocked(user.isLocked(clock.instant()))
            .disabled(false)
            .accountExpired(false)
            .credentialsExpired(false)
            .build();
    }
}
