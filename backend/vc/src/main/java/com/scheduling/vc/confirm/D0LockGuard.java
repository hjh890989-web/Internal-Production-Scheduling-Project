package com.scheduling.vc.confirm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Sprint 17 BR-V07 D-0 (당일) 락 service-level guard — TK-DAY-LOCK-1-2.
 *
 * <p>{@code production_date == today} → row UPDATE 차단 (override_reason 경로 예외).
 * DB trigger {@code trg_vc_schedule_d0_lock} (V043) 와 이중 안전망 — 본 클래스는 서비스 레이어에서
 * 친화적 한국어 메시지로 사전 차단 (사용자 UX), DB trigger 는 최후 방어선.
 *
 * <p>Clock 주입 (BR-X04 KST) — {@code LocalDate.now(clock)} 으로 호스트 OS 시각 의존 차단.
 */
@Component
@Profile("with-infra")
public class D0LockGuard {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Clock clock;

    public D0LockGuard(Clock clock) {
        this.clock = clock;
    }

    /** true = D-0 락 적용 대상 (production_date == 오늘), false = 미적용 (D-1 이후 또는 과거). */
    public boolean isLocked(LocalDate productionDate) {
        if (productionDate == null) return false;
        return productionDate.equals(LocalDate.now(clock.withZone(KST)));
    }

    /**
     * D-0 lock enforcement — UPDATE 진입점에서 호출.
     *
     * @throws D0LockViolationException production_date == 오늘 이고 override 경로가 아닐 시
     */
    public void enforce(LocalDate productionDate, boolean isOverridePath) {
        if (productionDate == null) {
            throw new IllegalArgumentException("production_date 필수 (BR-V07)");
        }
        LocalDate today = LocalDate.now(clock.withZone(KST));
        if (productionDate.equals(today) && !isOverridePath) {
            throw new D0LockViolationException(productionDate, today);
        }
    }
}
