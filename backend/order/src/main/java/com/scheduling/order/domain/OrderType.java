package com.scheduling.order.domain;

/**
 * 표준 수주 분류 + 우선순위 — TK-01-2-1 / TK-02-2-1 (BR-O01).
 *
 * <p><b>중요</b>: enum 정의 순서가 곧 우선순위 ordinal — 정의 변경 시 모든 비교 영향.
 * BR-O01: <b>Confirmed > Weekly > KD > Forecast</b> (ordinal 클수록 강함).
 *
 * <pre>
 *   FORECAST(0)  : 월별 예상       — 가장 약함
 *   KD(1)        : KD 발주         — 보충 (BR-V13)
 *   WEEKLY(2)    : 주간 발주
 *   CONFIRMED(3) : 확정            — 가장 강함 (BR-X01 D-2 hard)
 * </pre>
 *
 * <p>DB 저장: {@code @Enumerated(EnumType.STRING)} — NAME 저장, ordinal 은 메모리 비교만.
 * 정책 변경 시 enum 정의 순서만 수정 (코드 if-else 무관).
 */
public enum OrderType {
    /** 월별 예상 — 비확정, 우선순위 큐 후보 (가장 약함) */
    FORECAST("월별 예상"),
    /** KD 발주 — 별도 ERP 채널, capa 부족 시 보충 (BR-V13) */
    KD("KD 발주"),
    /** 주간 계획 — 단기 forecast 또는 임시 발주 */
    WEEKLY("주간 계획"),
    /** 확정 발주 — 납기 강제 (BR-X01 D-2 hard, 가장 강함) */
    CONFIRMED("확정 발주");

    private final String displayName;

    OrderType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** BR-O01 우선순위 ordinal — 클수록 강함. */
    public int precedenceRank() {
        return ordinal();
    }

    /** {@code this} 가 {@code other} 보다 강한지 (ordinal 비교). */
    public boolean isStrongerThan(OrderType other) {
        return this.ordinal() > other.ordinal();
    }

    /** {@code this} 가 {@code other} 보다 약하지 않은지 (≥). */
    public boolean isAtLeastAsStrongAs(OrderType other) {
        return this.ordinal() >= other.ordinal();
    }
}
