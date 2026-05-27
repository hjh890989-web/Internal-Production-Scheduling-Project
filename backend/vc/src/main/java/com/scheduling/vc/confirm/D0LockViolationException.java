package com.scheduling.vc.confirm;

import java.time.LocalDate;

/**
 * Sprint 17 BR-V07 D-0 (당일) 락 위반 — production_date == 오늘 row UPDATE 시도.
 *
 * <p>HTTP 423 Locked 매핑 (VcConfirmExceptionHandler). override 경로 (override_reason+override_by
 * 갱신) 만 예외 — DB trigger {@code trg_vc_schedule_d0_lock} 와 이중 안전망.
 */
public class D0LockViolationException extends RuntimeException {

    private final LocalDate productionDate;
    private final LocalDate today;

    public D0LockViolationException(LocalDate productionDate, LocalDate today) {
        super(String.format(
            "BR-V07 D-0 (당일) 락 위반: production_date=%s 가 오늘(%s) — 수정 차단 (일중 교체는 override_reason+override_by 갱신 필수)",
            productionDate, today));
        this.productionDate = productionDate;
        this.today = today;
    }

    public LocalDate getProductionDate() { return productionDate; }
    public LocalDate getToday() { return today; }
}
