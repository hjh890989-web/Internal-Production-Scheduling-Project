package com.scheduling.vc.required;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RequiredQuantityCalculator 진리표 — TK-05-3-1 (REQ-FUNC-VC-009).
 *
 * <p>공식 Q_required = max(0, Q_net + target − current) 의 4 케이스 + 호라이즌·합산·예외.
 */
class RequiredQuantityCalculatorTest {

    private static final LocalDate FROM = LocalDate.of(2026, 2, 16);
    private static final LocalDate TO = LocalDate.of(2026, 2, 22);
    private static final LocalDate IN = LocalDate.of(2026, 2, 18);

    private final RequiredQuantityCalculator calc = new RequiredQuantityCalculator();

    private OrderInput order(String hose, LocalDate date, int qty) {
        return new OrderInput(UUID.randomUUID(), hose, date, qty);
    }

    // ---------- TC-VC-009 4 케이스 ----------

    @Test
    @DisplayName("Case 1 동등 — target=100, current=100, qNet=50 → Q_required=50")
    void case1_equal() {
        var result = calc.calculate(
            List.of(order("A", IN, 50)),
            Map.of("A", new StockInfo(100, 100)),
            FROM, TO);

        assertThat(result.get("A").qRequired()).isEqualTo(50);
        assertThat(result.get("A").qNet()).isEqualTo(50);
    }

    @Test
    @DisplayName("Case 2 잉여 — target=100, current=200, qNet=50 → Q_required=0 (max 클램프)")
    void case2_excess_clamp_to_zero() {
        var result = calc.calculate(
            List.of(order("A", IN, 50)),
            Map.of("A", new StockInfo(100, 200)),
            FROM, TO);

        // qRequired=0 이므로 result 미포함 (needsProduction=false)
        assertThat(result).doesNotContainKey("A");
    }

    @Test
    @DisplayName("Case 3 부족 — target=100, current=50, qNet=80 → Q_required=130")
    void case3_shortage() {
        var result = calc.calculate(
            List.of(order("A", IN, 80)),
            Map.of("A", new StockInfo(100, 50)),
            FROM, TO);

        assertThat(result.get("A").qRequired()).isEqualTo(130);
    }

    @Test
    @DisplayName("Case 4 목표 0 — target=0, current=0, qNet=50 → Q_required=50")
    void case4_zero_target() {
        var result = calc.calculate(
            List.of(order("A", IN, 50)),
            Map.of("A", new StockInfo(0, 0)),
            FROM, TO);

        assertThat(result.get("A").qRequired()).isEqualTo(50);
    }

    // ---------- 추가 케이스 ----------

    @Test
    @DisplayName("호라이즌 외 수주 — 계산 미포함 (납기 < from 또는 > to)")
    void out_of_horizon_excluded() {
        var result = calc.calculate(List.of(
            order("A", LocalDate.of(2026, 2, 15), 50),    // FROM 이전
            order("A", LocalDate.of(2026, 2, 23), 50)     // TO 이후
        ), Map.of("A", new StockInfo(100, 100)), FROM, TO);

        assertThat(result).isEmpty();   // qNet=0 → qRequired=0
    }

    @Test
    @DisplayName("같은 hose 다수 수주 — Q_net 합산")
    void same_hose_orders_summed() {
        var result = calc.calculate(List.of(
            order("A", IN, 30),
            order("A", IN, 20),
            order("A", IN, 10)
        ), Map.of("A", new StockInfo(100, 100)), FROM, TO);

        assertThat(result.get("A").qNet()).isEqualTo(60);
        assertThat(result.get("A").qRequired()).isEqualTo(60);
    }

    @Test
    @DisplayName("호라이즌 경계 (from/to inclusive) — 포함")
    void horizon_boundaries_inclusive() {
        var result = calc.calculate(List.of(
            order("A", FROM, 10),
            order("A", TO, 20)
        ), Map.of("A", new StockInfo(0, 0)), FROM, TO);

        assertThat(result.get("A").qNet()).isEqualTo(30);
    }

    @Test
    @DisplayName("StockInfo 누락 → NoSuchElementException")
    void missing_stock_info_throws() {
        assertThatThrownBy(() -> calc.calculate(
            List.of(order("MISSING", IN, 50)),
            Map.of(), FROM, TO))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("MISSING");
    }

    @Test
    @DisplayName("음수 qty → IllegalArgumentException")
    void negative_qty_rejected() {
        assertThatThrownBy(() -> calc.calculate(
            List.of(order("A", IN, -10)),
            Map.of("A", new StockInfo(0, 0)), FROM, TO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("음수");
    }

    @Test
    @DisplayName("빈 / null orders → 빈 결과")
    void empty_input_returns_empty() {
        assertThat(calc.calculate(List.of(), Map.of(), FROM, TO)).isEmpty();
        assertThat(calc.calculate(null, Map.of(), FROM, TO)).isEmpty();
    }

    @Test
    @DisplayName("horizonTo < horizonFrom → IllegalArgumentException")
    void reversed_horizon_rejected() {
        assertThatThrownBy(() -> calc.calculate(
            List.of(order("A", IN, 10)),
            Map.of("A", new StockInfo(0, 0)), TO, FROM))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("calculateQuantities — 단순 Map<String, Integer> 출력")
    void calculate_quantities_simple_format() {
        var simple = calc.calculateQuantities(List.of(
            order("A", IN, 50), order("B", IN, 80)),
            Map.of("A", new StockInfo(100, 100), "B", new StockInfo(100, 50)),
            FROM, TO);

        assertThat(simple).containsEntry("A", 50).containsEntry("B", 130);
    }

    @Test
    @DisplayName("groupByHose — hose_id 별 OrderInput 그룹 (linkedOrderIds 추적용)")
    void group_by_hose() {
        var a1 = order("A", IN, 10);
        var a2 = order("A", IN, 20);
        var b1 = order("B", IN, 30);

        var grouped = calc.groupByHose(List.of(a1, a2, b1));

        assertThat(grouped.get("A")).containsExactly(a1, a2);
        assertThat(grouped.get("B")).containsExactly(b1);
    }

    @Test
    @DisplayName("StockInfo — 음수 target/current → IllegalArgumentException (정합)")
    void stock_info_negative_rejected() {
        assertThatThrownBy(() -> new StockInfo(-1, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StockInfo(0, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
