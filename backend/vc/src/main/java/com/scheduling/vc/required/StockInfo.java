package com.scheduling.vc.required;

/**
 * 재고 정보 — TK-05-3-1 (REQ-FUNC-VC-009).
 *
 * <p>master.product 테이블이 아직 미존재 (Sprint 2 baseline). caller 가 외부 ERP /
 * MES 또는 in-memory stub 으로 hose_id → StockInfo 매핑 제공.
 *
 * <p>Phase 2+ master.product 도입 시 ProductRepository 로 대체 가능 (calculator 시그니처 유지).
 *
 * @param targetStock   목표 재고
 * @param currentStock  현재 재고
 */
public record StockInfo(int targetStock, int currentStock) {
    public StockInfo {
        if (targetStock < 0 || currentStock < 0) {
            throw new IllegalArgumentException(
                "StockInfo: targetStock/currentStock 음수 불가 (target=%d, current=%d)"
                    .formatted(targetStock, currentStock));
        }
    }

    /** 재고 갭 (target − current). 음수 가능 (잉여). */
    public int stockGap() {
        return targetStock - currentStock;
    }
}
