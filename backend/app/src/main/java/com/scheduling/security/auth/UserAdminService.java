package com.scheduling.security.auth;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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

    @Auditable("EP-MASTER-UI 사용자 PIN reset (IT_OPS)")
    @Transactional
    public void resetPin(String employeeId, String newRawPin) {
        AppUser user = loadUser(employeeId);
        user.changePin(passwordEncoder.encode(newRawPin));
        repository.save(user);
        log.info("EP-MASTER-UI user PIN reset — employee_id={}", employeeId);
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
