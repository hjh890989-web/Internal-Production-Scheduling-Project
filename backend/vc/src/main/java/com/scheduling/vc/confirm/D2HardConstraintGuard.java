package com.scheduling.vc.confirm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Sprint 16 BR-X07 D-2 hard 제약 service-level guard — TK-CONFIRM-1-2.
 *
 * <p>{@code production_date - today < 2} → 신규 추가 차단. DB trigger
 * {@code trg_vc_schedule_d2_hard} (V041) 와 이중 안전망 — 본 클래스는 서비스 레이어에서
 * 친화적 한국어 메시지로 사전 차단 (사용자 UX), DB trigger 는 최후 방어선.
 *
 * <p>Clock 주입 (BR-X04 KST) — {@code LocalDate.now(clock)} 으로 호스트 OS 시각 의존 차단.
 */
@Component
@Profile("with-infra")
public class D2HardConstraintGuard {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int D2_MIN_DAYS = 2;

    private final Clock clock;

    public D2HardConstraintGuard(Clock clock) {
        this.clock = clock;
    }

    /** true = D-2 hard 통과 (D-3 이상), false = 차단 대상 (D-2/D-1/D-0/과거). */
    public boolean fits(LocalDate productionDate) {
        if (productionDate == null) return false;
        LocalDate today = LocalDate.now(clock.withZone(KST));
        return productionDate.toEpochDay() - today.toEpochDay() >= D2_MIN_DAYS;
    }

    /**
     * 친화적 메시지 throw — Controller/Service 진입점에서 호출.
     *
     * @throws D2HardConstraintException production_date 가 D-2 hard 위반 시 (HTTP 423 매핑)
     */
    public void enforce(LocalDate productionDate) {
        if (productionDate == null) {
            throw new IllegalArgumentException("production_date 필수 (BR-X07)");
        }
        LocalDate today = LocalDate.now(clock.withZone(KST));
        long gap = productionDate.toEpochDay() - today.toEpochDay();
        if (gap < D2_MIN_DAYS) {
            throw new D2HardConstraintException(productionDate, today, gap);
        }
    }

    /**
     * UPDATE 시 게이트 — D-0 (오늘) 또는 과거 production_date 차단 (BR-X01 게이트).
     * BR-V07 trg_vc_intra_day_lock 의 service-level 친화 메시지 사전 차단.
     *
     * @throws D2HardConstraintException production_date 가 오늘 이전이거나 오늘 (UPDATE 시 D-0 락)
     */
    public void enforceUpdate(LocalDate productionDate) {
        if (productionDate == null) {
            throw new IllegalArgumentException("production_date 필수 (BR-X01)");
        }
        LocalDate today = LocalDate.now(clock.withZone(KST));
        long gap = productionDate.toEpochDay() - today.toEpochDay();
        if (gap <= 0) {
            throw new D2HardConstraintException(productionDate, today, gap);
        }
    }
}
