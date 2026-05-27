package com.scheduling.vc.confirm;

import java.time.LocalDate;

/**
 * Sprint 16 BR-X07 D-2 hard 제약 위반 — production_date 가 D-2 이전 (today + 2 미만).
 *
 * <p>HTTP 423 Locked 매핑 (VcExceptionHandler). Frontend 가 본 코드로 "D-2 이후 추가 불가" 안내.
 */
public class D2HardConstraintException extends RuntimeException {

    private final LocalDate productionDate;
    private final LocalDate today;
    private final long gapDays;

    public D2HardConstraintException(LocalDate productionDate, LocalDate today, long gapDays) {
        super(String.format(
            "BR-X07 D-2 hard 제약 위반: production_date=%s 가 오늘(%s) 기준 %d일 — D-2 (2일) 이상만 신규 추가 가능",
            productionDate, today, gapDays));
        this.productionDate = productionDate;
        this.today = today;
        this.gapDays = gapDays;
    }

    public LocalDate getProductionDate() { return productionDate; }
    public LocalDate getToday() { return today; }
    public long getGapDays() { return gapDays; }
}
