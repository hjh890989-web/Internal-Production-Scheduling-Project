package com.scheduling.ex.required;

import com.scheduling.master.api.ProductInventoryLookup;
import com.scheduling.master.api.ProductInventorySummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 압출 필요 수량 Q_ext 계산 — TK-08-3-1 (EP-08 ST-08-3, REQ-FUNC-EX-010).
 *
 * <p><b>공식</b>: {@code Q_ext = max(0, Q_vc + target_stock − current_stock)}
 *
 * <p><b>4 시나리오</b>:
 * <ol>
 *   <li>충분한 재고 (current ≥ target) → Q_ext = max(0, Q_vc − (current − target))</li>
 *   <li>target 도달 (current = target) → Q_ext = Q_vc</li>
 *   <li>current 부족 (current < target) → Q_ext = Q_vc + (target − current)</li>
 *   <li>음수 입력 → IllegalArgumentException</li>
 * </ol>
 *
 * <p>재고 마스터 미등록 hose → target=0, current=0 가정 → Q_ext = Q_vc.
 */
@Component
@Profile("with-infra")
public class ExtrusionDemandCalculator {

    private final ProductInventoryLookup inventoryLookup;

    public ExtrusionDemandCalculator(ProductInventoryLookup inventoryLookup) {
        this.inventoryLookup = inventoryLookup;
    }

    /**
     * Pure 수식 — 단위 테스트 격리 (REQ-FUNC-EX-010).
     */
    public int computeQExt(int qVc, int targetStock, int currentStock) {
        if (qVc < 0) {
            throw new IllegalArgumentException("qVc ≥ 0: " + qVc);
        }
        if (targetStock < 0 || currentStock < 0) {
            throw new IllegalArgumentException(
                "stock ≥ 0: target=%d, current=%d".formatted(targetStock, currentStock));
        }
        long raw = (long) qVc + targetStock - currentStock;
        return raw < 0 ? 0 : (int) Math.min(raw, Integer.MAX_VALUE);
    }

    /**
     * 마스터 조회 + 계산 — hose 별 재고 자동 lookup.
     *
     * @return Q_ext (재고 미등록 시 Q_vc 그대로)
     */
    public int computeForHose(String hoseId, int qVc) {
        Optional<ProductInventorySummary> inv = inventoryLookup.findById(hoseId);
        if (inv.isEmpty()) {
            return qVc;     // 재고 미등록 — 보수적 (Q_ext = Q_vc, 추가 보충 0)
        }
        return computeQExt(qVc, inv.get().targetStock(), inv.get().currentStock());
    }
}
