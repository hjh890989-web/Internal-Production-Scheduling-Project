package com.scheduling.security.auth;

import com.scheduling.audit.aop.Auditable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Sprint 10 EP-AUTH 잠금 정책 Service (ST-AUTH-3, NFR-SEC-007).
 *
 * <p>5회 연속 실패 시 {@code locked_until = now + 10min} 설정 → {@link AppUserDetailsService} 가
 * 다음 lookup 시 {@code accountLocked=true} 로 매핑 → {@code DaoAuthenticationProvider} 가
 * {@code LockedException} 발생.
 *
 * <p>{@link AuthController} (ST-AUTH-4) 가 인증 결과에 따라:
 * <ul>
 *   <li>성공 — {@link #recordSuccess} (카운터 + lockedUntil reset)</li>
 *   <li>실패 — {@link #recordFailure} (카운터 +1, 5회 도달 시 자동 lock)</li>
 * </ul>
 *
 * <p>@Auditable — BR-X02 audit (audit_log 에 actor=사번 + 변경 jsonb 자동 기록).
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    /** NFR-SEC-007 — 5회 실패 임계치. */
    public static final short MAX_FAILED_ATTEMPTS = 5;

    /** NFR-SEC-007 — 잠금 시간 10분. */
    public static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final AppUserRepository repository;
    private final Clock clock;

    public LoginAttemptService(AppUserRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * 로그인 실패 — 카운터 +1. 5회 도달 시 {@code lockedUntil = now + 10min} 자동 설정.
     *
     * @param employeeId 사번 (미존재 시 noop — UserDetailsService 가 UsernameNotFoundException 발생 이미 처리)
     */
    @Auditable("EP-AUTH 로그인 실패 카운터 증가 (NFR-SEC-007)")
    @Transactional
    public void recordFailure(String employeeId) {
        repository.findByEmployeeId(employeeId).ifPresent(user -> {
            user.recordFailure();
            if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.lock(clock.instant().plus(LOCK_DURATION));
                log.warn("EP-AUTH lockout — employee_id={} failed_attempts={} locked_until={}",
                    employeeId, user.getFailedAttempts(), user.getLockedUntil());
            }
            repository.save(user);
        });
    }

    /** 로그인 성공 — 카운터 0 + lockedUntil null reset. */
    @Auditable("EP-AUTH 로그인 성공 카운터 reset")
    @Transactional
    public void recordSuccess(String employeeId) {
        repository.findByEmployeeId(employeeId).ifPresent(user -> {
            if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
                user.recordSuccess();
                repository.save(user);
            }
        });
    }
}
