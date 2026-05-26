package com.scheduling.security.auth;

import com.scheduling.security.RoleConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Sprint 10 EP-AUTH — 사번+PIN 인증 사용자 (NFR-SEC-007).
 *
 * <p>사번 8자리 숫자 PK + PIN 4자리 BCrypt(strength=12) hash + 4 역할 (PLANNER/STK_USER/IT_OPS/READ_ONLY).
 * 5회 실패 시 {@code locked_until = now() + 10min} 설정 — {@link AppUserDetailsService} 가 {@code LockedException} 발생.
 *
 * <p>V036 스키마 (table {@code app.user_account}) — pin_hash 컬럼은 audit jsonb 에서 제외 (NFR-SEC-005).
 *
 * @see RoleConstants role 명 정합
 */
@Entity
@Table(name = "user_account", schema = "app")
public class AppUser {

    @Id
    @Column(name = "employee_id", nullable = false, updatable = false, length = 8)
    private String employeeId;

    @Column(name = "pin_hash", nullable = false, length = 60)
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "failed_attempts", nullable = false)
    private short failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {}

    public AppUser(String employeeId, String pinHash, Role role) {
        if (employeeId == null || !employeeId.matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("employee_id 사번 8자리 숫자 필수 (현재: " + employeeId + ")");
        }
        if (pinHash == null || pinHash.length() != 60) {
            throw new IllegalArgumentException("pin_hash BCrypt 60 char 필수");
        }
        if (role == null) {
            throw new IllegalArgumentException("role 필수");
        }
        this.employeeId = employeeId;
        this.pinHash = pinHash;
        this.role = role;
        this.failedAttempts = 0;
    }

    /** 로그인 실패 — 카운터 +1. 5회 도달 시 호출자(LoginAttemptService) 가 {@link #lock} 호출. */
    public void recordFailure() {
        this.failedAttempts = (short) (this.failedAttempts + 1);
    }

    /** 로그인 성공 — 카운터 초기화 + 잠금 해제. */
    public void recordSuccess() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    /** 5회 실패 시 잠금 — now + 10min (LoginAttemptService 결정). */
    public void lock(Instant until) {
        if (until == null) {
            throw new IllegalArgumentException("lock until 필수");
        }
        this.lockedUntil = until;
    }

    /** 현재 잠긴 상태인지 — locked_until 이 future 면 true. */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** PIN 변경 — IT_OPS UI 또는 비밀번호 재설정 flow. */
    public void changePin(String newPinHash) {
        if (newPinHash == null || newPinHash.length() != 60) {
            throw new IllegalArgumentException("pin_hash BCrypt 60 char 필수");
        }
        this.pinHash = newPinHash;
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public String getEmployeeId() { return employeeId; }
    public String getPinHash() { return pinHash; }
    public Role getRole() { return role; }
    public short getFailedAttempts() { return failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** RBAC 4 역할 — {@link RoleConstants} 와 명칭 정합. */
    public enum Role {
        PLANNER,
        STK_USER,
        IT_OPS,
        READ_ONLY
    }
}
