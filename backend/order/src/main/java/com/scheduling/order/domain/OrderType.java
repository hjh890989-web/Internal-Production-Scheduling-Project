package com.scheduling.order.domain;

/**
 * 표준 수주 분류 — TK-01-2-1 (SRS §6.2.4 ORDER.order_type).
 *
 * <p>SourceType (워크북 출처) 과 별개 — 매핑 후 표준 도메인 분류.
 * 룰셋 YAML 의 {@code order_type_default} 가 SourceType → OrderType 매핑.
 */
public enum OrderType {
    /** 월별 예상 — 비확정, 우선순위 큐 후보 */
    FORECAST,
    /** 주간 계획 — 단기 forecast 또는 임시 발주 */
    WEEKLY,
    /** KD 발주 — 별도 ERP 채널, capa 부족 시 보충 (BR-V13) */
    KD,
    /** 확정 발주 — 납기 강제 (BR-X01 D-2 hard) */
    CONFIRMED
}
