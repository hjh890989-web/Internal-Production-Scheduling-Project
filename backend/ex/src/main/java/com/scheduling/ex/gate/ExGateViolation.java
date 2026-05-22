package com.scheduling.ex.gate;

/**
 * 압출 검증 게이트 위반 — TK-EX11-1-1·2 (REQ-FUNC-EX-011).
 *
 * <p>{@link ExtrusionValidationGate} 가 반환하는 단일 위반 사유. {@link Category}
 * 는 EP-EX12 대안 생성기의 분류 입력.
 *
 * @param category  위반 카테고리 (CUMULATIVE_YIELD_SHORT / SHIFT_CAPACITY_EXCEEDED)
 * @param reason    한국어 사유 (UI 표시)
 * @param targetQty 목표 수량 (Q_ext)
 * @param actual    실측 누계 (yield 합산 또는 shift min)
 */
public record ExGateViolation(
    Category category,
    String reason,
    int targetQty,
    int actual
) {
    public enum Category {
        /** 누적 yield 가 Q_ext 미충족 (BR-E10). */
        CUMULATIVE_YIELD_SHORT,
        /** shift effective_min 초과 (BR-E04). */
        SHIFT_CAPACITY_EXCEEDED
    }

    /** BR-E10 — 누적 yield ≥ Q_ext 미충족. */
    public static ExGateViolation yieldShort(int qExt, int cumulativeYield) {
        return new ExGateViolation(Category.CUMULATIVE_YIELD_SHORT,
            "누적 yield %d < Q_ext %d (BR-E10)".formatted(cumulativeYield, qExt),
            qExt, cumulativeYield);
    }

    /** BR-E04 — shift effective_min 초과. */
    public static ExGateViolation shiftCapacityExceeded(int effectiveMin, int actualMin) {
        return new ExGateViolation(Category.SHIFT_CAPACITY_EXCEEDED,
            "shift actualMin %d > effective_min %d (BR-E04)".formatted(actualMin, effectiveMin),
            effectiveMin, actualMin);
    }
}
