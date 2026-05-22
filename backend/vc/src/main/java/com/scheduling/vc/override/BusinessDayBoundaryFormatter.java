package com.scheduling.vc.override;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 영업일 경계 키 포맷터 — TK-13-3-1 (EP-13 ST-13-3, BR-V07).
 *
 * <p>DO-04 작업지시서 출력 시 일중 락 해제 시점 명시 — {@code YYYY-MM-DD_END}.
 * 작업자가 "다음 영업일 시작 전까지 본 셋팅 유지" 알 수 있도록.
 *
 * <p>예: 2026-03-02 (월) → {@code "2026-03-02_END"} = 화 00:00 까지 유지.
 */
public final class BusinessDayBoundaryFormatter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private BusinessDayBoundaryFormatter() {}

    /**
     * @param productionDate 영업일
     * @return {@code YYYY-MM-DD_END}
     */
    public static String formatBoundaryKey(LocalDate productionDate) {
        if (productionDate == null) {
            throw new IllegalArgumentException("productionDate 필수");
        }
        return productionDate.format(DATE_FMT) + "_END";
    }
}
