package com.scheduling.security.auth;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Sprint 12 EP-MASTER-UI 사용자 관리 Service (TK-MASTER-2-2, IT_OPS 전용).
 *
 * <p>{@code @Auditable} BR-X02 — 모든 mutation 이 audit_log 에 actor=IT_OPS 사번 + 변경 jsonb 기록.
 * pin_hash 컬럼은 V036 audit trigger 가 jsonb 에서 제외 (NFR-SEC-005).
 */
@Service
public class UserAdminService {

    private static final Logger log = LoggerFactory.getLogger(UserAdminService.class);

    /** Sprint 22 ST-SEC-4 — reset 시 강제 만료 폭 (30일 정책보다 크게 → 첫 로그인 즉시 pinExpired). */
    private static final Duration FORCE_EXPIRE_OFFSET = Duration.ofDays(31);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserAdminService(AppUserRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public List<AppUser> list() {
        return repository.findAll();
    }

    @Auditable("EP-MASTER-UI 사용자 추가 (IT_OPS)")
    @Transactional
    public AppUser create(String employeeId, String rawPin, AppUser.Role role) {
        if (repository.findByEmployeeId(employeeId).isPresent()) {
            throw new EntityExistsException("사번 중복: " + employeeId);
        }
        AppUser user = new AppUser(employeeId, passwordEncoder.encode(rawPin), role);
        log.info("EP-MASTER-UI user create — employee_id={} role={}", employeeId, role);
        return repository.save(user);
    }

    /**
     * IT_OPS PIN reset — 임시 PIN 적용 + 강제 만료 (ST-SEC-4).
     *
     * <p>last_pin_change_at = now - 31일 강제 set → 대상 사용자 첫 로그인 시 pinExpired=true →
     * 강제 변경 화면. 임시 PIN 그대로 운영 사용 방지 (NFR-SEC-007).
     */
    @Auditable("EP-MASTER-UI 사용자 PIN reset (IT_OPS, 강제 만료)")
    @Transactional
    public void resetPin(String employeeId, String newRawPin) {
        AppUser user = loadUser(employeeId);
        Instant forcedExpiry = clock.instant().minus(FORCE_EXPIRE_OFFSET);
        user.changePin(passwordEncoder.encode(newRawPin), forcedExpiry);
        repository.save(user);
        log.info("EP-MASTER-UI user PIN reset (강제 만료) — employee_id={}", employeeId);
    }

    @Auditable("EP-MASTER-UI 사용자 잠금 해제 (IT_OPS)")
    @Transactional
    public void unlock(String employeeId) {
        AppUser user = loadUser(employeeId);
        user.recordSuccess();   // failed_attempts=0 + lockedUntil=null
        repository.save(user);
        log.info("EP-MASTER-UI user unlock — employee_id={}", employeeId);
    }

    @Auditable("EP-MASTER-UI 사용자 삭제 (IT_OPS)")
    @Transactional
    public void delete(String employeeId) {
        AppUser user = loadUser(employeeId);
        repository.delete(user);
        log.info("EP-MASTER-UI user delete — employee_id={}", employeeId);
    }

    private AppUser loadUser(String employeeId) {
        return repository.findByEmployeeId(employeeId)
            .orElseThrow(() -> new EntityNotFoundException("사번 미존재: " + employeeId));
    }
}
