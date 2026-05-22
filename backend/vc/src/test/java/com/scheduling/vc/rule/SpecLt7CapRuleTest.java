package com.scheduling.vc.rule;

import com.scheduling.master.api.ProductSpecLookup;
import com.scheduling.master.api.ProductSpecSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SpecLt7CapRule 단위 — TK-21-5-3 (BR-V17).
 */
class SpecLt7CapRuleTest {

    private ProductSpecLookup lookup;
    private SpecLt7CapRule rule;

    @BeforeEach
    void setUp() {
        lookup = mock(ProductSpecLookup.class);
        rule = new SpecLt7CapRule(lookup);
    }

    private ProductSpecSummary spec(String hose, Integer spec, int angles, boolean lt7) {
        return new ProductSpecSummary(hose, spec, angles, lt7);
    }

    @Test
    @DisplayName("spec NULL → pass (룰 미적용)")
    void no_spec_passes() {
        when(lookup.findById(eq("X"))).thenReturn(Optional.of(spec("X", null, 1, false)));
        assertThat(rule.fitsAngleCap("X", 0)).isTrue();
        assertThat(rule.fitsAngleCap("X", 99)).isTrue();
    }

    @Test
    @DisplayName("spec ≥ 7 → pass (룰 미적용)")
    void spec_gte_7_passes() {
        when(lookup.findById(eq("Y"))).thenReturn(Optional.of(spec("Y", 8, 1, false)));
        assertThat(rule.fitsAngleCap("Y", 99)).isTrue();
    }

    @Test
    @DisplayName("spec=6, angle=2 — 누계 0+2=2 → pass, 0+5=5 → fail")
    void spec_lt7_cap_basic() {
        when(lookup.findById(eq("28442-6T010"))).thenReturn(Optional.of(
            spec("28442-6T010", 6, 2, true)));

        assertThat(rule.fitsAngleCap("28442-6T010", 0)).isTrue();   // 0+2=2 ≤ 4
        assertThat(rule.fitsAngleCap("28442-6T010", 2)).isTrue();   // 2+2=4 ≤ 4
        assertThat(rule.fitsAngleCap("28442-6T010", 3)).isFalse();  // 3+2=5 > 4
    }

    @Test
    @DisplayName("spec=5, angle=1 — 누계 3+1=4 → pass, 4+1=5 → fail")
    void spec_lt7_cap_boundary() {
        when(lookup.findById(eq("28415-08400"))).thenReturn(Optional.of(
            spec("28415-08400", 5, 1, true)));

        assertThat(rule.fitsAngleCap("28415-08400", 3)).isTrue();
        assertThat(rule.fitsAngleCap("28415-08400", 4)).isFalse();
    }

    @Test
    @DisplayName("마스터 미등록 hose → pass")
    void unknown_hose_passes() {
        when(lookup.findById(eq("UNKNOWN"))).thenReturn(Optional.empty());
        assertThat(rule.fitsAngleCap("UNKNOWN", 99)).isTrue();
    }

    @Test
    @DisplayName("AngleLedger — (machine, date) 별 누계 add/count")
    void angle_ledger_accumulates() {
        SpecLt7CapRule.AngleLedger ledger = new SpecLt7CapRule.AngleLedger();
        LocalDate d = LocalDate.of(2026, 2, 23);

        assertThat(ledger.count("LP-01", d)).isZero();

        ledger.add("LP-01", d, 2);
        assertThat(ledger.count("LP-01", d)).isEqualTo(2);

        ledger.add("LP-01", d, 1);
        assertThat(ledger.count("LP-01", d)).isEqualTo(3);

        // 다른 머신 — 독립
        assertThat(ledger.count("LP-02", d)).isZero();

        // 다른 날짜 — 독립
        assertThat(ledger.count("LP-01", d.plusDays(1))).isZero();
    }

    @Test
    @DisplayName("SPEC_LT7_MAX_ANGLES = 4 (BR-V17 상수)")
    void max_angles_constant() {
        assertThat(SpecLt7CapRule.SPEC_LT7_MAX_ANGLES).isEqualTo(4);
    }
}
