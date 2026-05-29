package com.scheduling.security.auth;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Sprint 22 ST-SEC-2 — PIN 30일 만료 정책 + 자가 변경 (NFR-SEC-007).
 *
 * <p>{@link #isPinExpired(String)} — 로그인 응답 {@code pinExpired} flag 산출.
 * {@link #changeOwnPin(String, String)} — 강제 변경 화면 / 자가 변경 시 PIN 갱신 + 30일 clock reset.
 *
 * <p>IT_OPS 의 타인 PIN reset (강제 만료) 은 {@link UserAdminService#resetPin} 가 담당 — 본 서비스는
 * 인증된 본인의 PIN 변경만. {@link Clock} 주입 (BR-X04).
 */
@Service
public class PinPolicyService {

    private static final Logger log = LoggerFactory.getLogger(PinPolicyService.class);

    /** NFR-SEC-007 — PIN 최대 유효 기간 30일. */
    public static final Duration MAX_PIN_AGE = Duration.ofDays(30);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PinPolicyService(AppUserRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** 사용자의 PIN 이 30일 초과 미변경 상태인지 — 미존재 사번은 false (로그인 흐름이 별도 401 처리). */
    public boolean isPinExpired(String employeeId) {
        return repository.findByEmployeeId(employeeId)
            .map(u -> u.isPinExpired(clock.instant(), MAX_PIN_AGE))
            .orElse(false);
    }

    /** 인증된 본인의 PIN 변경 — last_pin_change_at = now (30일 clock reset). */
    @Auditable("자가 PIN 변경 (30일 만료 강제 변경 포함)")
    @Transactional
    public void changeOwnPin(String employeeId, String newRawPin) {
        AppUser user = repository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new EntityNotFoundException("사번 미존재: " + employeeId));
        user.changePin(passwordEncoder.encode(newRawPin), clock.instant());
        repository.save(user);
        log.info("EP-SEC-HARDEN 자가 PIN 변경 — employee_id={}", employeeId);
    }
}
