package com.scheduling.ex.yield;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * YieldFormula 단위 — TK-08-2-1·2·3 (BR-E05).
 *
 * <p>핵심 검증: 29673-2R060 reference case = 2,531 (Sprint 3 DoD).
 */
class YieldFormulaTest {

    private final YieldFormula formula = new YieldFormula();

    // ---------- BR-E05 reference case ----------

    @Test
    @DisplayName("BR-E05 reference — 29673-2R060 주간전반 (speed=14.06, length=1000, min=180) = 2,531")
    void br_e05_reference_29673_2R060_yields_2531() {
        long yield = formula.compute(new BigDecimal("14.060"), 180, 1000);
        assertThat(yield).as("BR-E05 골든 케이스 — Sprint 3 단일 최중요 검증").isEqualTo(2531L);
    }

    @Test
    @DisplayName("Round-half-up 검증 — 14.0583 → 2530 (2530.494) / 14.06 → 2531 (2530.8)")
    void round_half_up_boundary() {
        // 14.0583 × 180 × 1000 / 1000 = 2530.494 → round → 2530
        assertThat(formula.compute(new BigDecimal("14.0583"), 180, 1000)).isEqualTo(2530L);
        // 14.0584 × 180 × 1000 / 1000 = 2530.512 → round → 2531
        assertThat(formula.compute(new BigDecimal("14.0584"), 180, 1000)).isEqualTo(2531L);
        // 14.06 × 180 × 1000 / 1000 = 2530.8 → round → 2531
        assertThat(formula.compute(new BigDecimal("14.06"), 180, 1000)).isEqualTo(2531L);
    }

    // ---------- 정상 케이스 ----------

    @Test
    @DisplayName("speed=15, min=180, length=1000 → 15 × 180 = 2700")
    void simple_yield_2700() {
        long yield = formula.compute(new BigDecimal("15"), 180, 1000);
        assertThat(yield).isEqualTo(2700L);
    }

    @Test
    @DisplayName("speed=20, min=180, length=500 → 20 × 180 × 1000 / 500 = 7200")
    void short_length_higher_yield() {
        long yield = formula.compute(new BigDecimal("20"), 180, 500);
        assertThat(yield).isEqualTo(7200L);
    }

    // ---------- 단위 가드 (BR-E05 TK-08-2-3) ----------

    @Test
    @DisplayName("speed > 200 m/min → UnitMismatchException (mm/min 입력 의심)")
    void excessive_speed_unit_mismatch() {
        assertThatThrownBy(() -> formula.compute(new BigDecimal("250"), 180, 1000))
            .isInstanceOf(UnitMismatchException.class)
            .hasMessageContaining("speed");
    }

    @Test
    @DisplayName("length > 100,000 mm → UnitMismatchException (μm 입력 의심)")
    void excessive_length_unit_mismatch() {
        assertThatThrownBy(() -> formula.compute(new BigDecimal("14"), 180, 200_000))
            .isInstanceOf(UnitMismatchException.class)
            .hasMessageContaining("length");
    }

    // ---------- 0·음수·null 방어 ----------

    @Test
    @DisplayName("speed = 0 → IllegalArgumentException")
    void zero_speed_rejected() {
        assertThatThrownBy(() -> formula.compute(BigDecimal.ZERO, 180, 1000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("speed null → IllegalArgumentException")
    void null_speed_rejected() {
        assertThatThrownBy(() -> formula.compute(null, 180, 1000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("effectiveMin ≤ 0 → IllegalArgumentException")
    void zero_or_negative_min_rejected() {
        assertThatThrownBy(() -> formula.compute(new BigDecimal("14"), 0, 1000))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> formula.compute(new BigDecimal("14"), -180, 1000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("length ≤ 0 → IllegalArgumentException")
    void zero_or_negative_length_rejected() {
        assertThatThrownBy(() -> formula.compute(new BigDecimal("14"), 180, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> formula.compute(new BigDecimal("14"), 180, -1000))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("MAX_REASONABLE_SPEED = 200, MAX_REASONABLE_LENGTH = 100,000 (BR-E05 상수)")
    void constants_verified() {
        assertThat(YieldFormula.MAX_REASONABLE_SPEED).isEqualTo(new BigDecimal("200"));
        assertThat(YieldFormula.MAX_REASONABLE_LENGTH).isEqualTo(100_000);
    }
}
