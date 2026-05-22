package com.scheduling.ex.required;

import com.scheduling.master.api.ProductInventoryLookup;
import com.scheduling.master.api.ProductInventorySummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ExtrusionDemandCalculator 단위 — TK-08-3-1·2 (REQ-FUNC-EX-010).
 *
 * <p>Q_ext = max(0, Q_vc + target − current) — 4 시나리오 검증.
 */
class ExtrusionDemandCalculatorTest {

    private ProductInventoryLookup lookup;
    private ExtrusionDemandCalculator calc;

    @BeforeEach
    void setUp() {
        lookup = mock(ProductInventoryLookup.class);
        calc = new ExtrusionDemandCalculator(lookup);
    }

    // ---------- 4 시나리오 (TK-08-3-2) ----------

    @Test
    @DisplayName("시나리오 1 — 충분한 재고 (current > target) → Q_ext = max(0, Q_vc - 차감)")
    void sufficient_stock_reduces_demand() {
        // Q_vc=100, target=50, current=80 → 100 + 50 − 80 = 70
        assertThat(calc.computeQExt(100, 50, 80)).isEqualTo(70);
        // Q_vc=50, target=20, current=200 → max(0, 50 + 20 - 200) = max(0, -130) = 0
        assertThat(calc.computeQExt(50, 20, 200)).isZero();
    }

    @Test
    @DisplayName("시나리오 2 — target 도달 (current = target) → Q_ext = Q_vc")
    void target_reached_yields_q_vc() {
        assertThat(calc.computeQExt(100, 50, 50)).isEqualTo(100);
        assertThat(calc.computeQExt(0, 100, 100)).isZero();
    }

    @Test
    @DisplayName("시나리오 3 — current 부족 (current < target) → Q_ext = Q_vc + 보충")
    void shortage_increases_demand() {
        // Q_vc=100, target=200, current=50 → 100 + 200 − 50 = 250
        assertThat(calc.computeQExt(100, 200, 50)).isEqualTo(250);
        // Q_vc=0, target=500, current=100 → 0 + 500 − 100 = 400
        assertThat(calc.computeQExt(0, 500, 100)).isEqualTo(400);
    }

    @Test
    @DisplayName("시나리오 4 — 음수 입력 → IllegalArgumentException")
    void negative_inputs_rejected() {
        assertThatThrownBy(() -> calc.computeQExt(-1, 100, 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.computeQExt(100, -1, 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.computeQExt(100, 100, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- computeForHose (재고 마스터 lookup) ----------

    @Test
    @DisplayName("computeForHose — 재고 마스터 등록 hose → lookup 결과 적용")
    void compute_for_hose_uses_inventory() {
        when(lookup.findById(eq("29673-2R060"))).thenReturn(Optional.of(
            new ProductInventorySummary("29673-2R060", 500, 100)));

        // Q_vc=200, target=500, current=100 → 200 + 500 − 100 = 600
        assertThat(calc.computeForHose("29673-2R060", 200)).isEqualTo(600);
    }

    @Test
    @DisplayName("computeForHose — 재고 미등록 hose → Q_ext = Q_vc (보수적)")
    void compute_for_hose_no_inventory_yields_q_vc() {
        when(lookup.findById(eq("UNKNOWN"))).thenReturn(Optional.empty());
        assertThat(calc.computeForHose("UNKNOWN", 150)).isEqualTo(150);
    }

    @Test
    @DisplayName("ProductInventorySummary.shortage — target - current 부호")
    void inventory_summary_shortage() {
        ProductInventorySummary s1 = new ProductInventorySummary("A", 100, 30);   // 부족 70
        ProductInventorySummary s2 = new ProductInventorySummary("B", 50, 50);    // 정확
        ProductInventorySummary s3 = new ProductInventorySummary("C", 20, 100);   // 충분 -80

        assertThat(s1.shortage()).isEqualTo(70);
        assertThat(s2.shortage()).isZero();
        assertThat(s3.shortage()).isEqualTo(-80);
    }
}
