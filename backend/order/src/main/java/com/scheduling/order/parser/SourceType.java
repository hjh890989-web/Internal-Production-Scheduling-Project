package com.scheduling.order.parser;

/**
 * 수주 엑셀 워크북 출처 4종 분류 — TK-01-1-2 (REQ-FUNC-OC-002).
 *
 * <p>UNRECOGNIZED = confidence < 0.5 — TK-01-2-1 매핑이 사용자에게 확인 요청.
 */
public enum SourceType {
    MONTHLY_FORECAST("월별 예상 발주량"),
    WEEKLY_PLAN("주간 계획"),
    CONFIRMED_ORDER("확정 발주"),
    KD_ORDER("KD 발주"),
    UNRECOGNIZED("미식별");

    private final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
