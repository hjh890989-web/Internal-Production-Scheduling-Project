package com.scheduling.master.api;

/**
 * 품번 재고 요약 — TK-08-3-1.
 *
 * @param hoseId        품번
 * @param targetStock   안전재고 (목표)
 * @param currentStock  현재고
 */
public record ProductInventorySummary(
    String hoseId,
    int targetStock,
    int currentStock
) {
    /** target - current — 양수면 부족, 음수면 충분. */
    public int shortage() {
        return targetStock - currentStock;
    }
}
