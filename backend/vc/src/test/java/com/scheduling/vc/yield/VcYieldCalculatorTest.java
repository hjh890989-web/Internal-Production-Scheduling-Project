package com.scheduling.vc.yield;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.master.api.VcConstraintLookup;
import com.scheduling.master.api.VcConstraintSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VcYieldCalculatorTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-05-21T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private VcConstraintLookup lookup;
    private SchedulingMetrics metrics;
    private VcYieldCalculator calc;

    @BeforeEach
    void setUp() {
        lookup = mock(VcConstraintLookup.class);
        metrics = mock(SchedulingMetrics.class);
        calc = new VcYieldCalculator(lookup, metrics, CLOCK);
    }

    private VcConstraintSummary cs(String hose, short composite, Short lpMolds, Short icMolds) {
        return new VcConstraintSummary(hose, composite, lpMolds, (short) 20, icMolds, (short) 20);
    }

    @Test
    @DisplayName("BR-V03 yield — composite × molds_per_angle (LP 단순 1·1=1)")
    void yield_simple_lp() {
        when(lookup.findAll()).thenReturn(List.of(
            cs("29673-2F900", (short) 1, (short) 1, (short) 1)
        ));
        calc.rebuild();

        assertThat(calc.yieldPerRotation("29673-2F900", "LP")).contains(1);
        assertThat(calc.yieldPerRotation("29673-2F900", "IC")).contains(1);
    }

    @Test
    @DisplayName("BR-V03 yield — composite 2 × molds 7 = LP 14 / IC 6 (28421-2M800 패턴)")
    void yield_composite_2_molds_7() {
        when(lookup.findAll()).thenReturn(List.of(
            cs("28421-2M800", (short) 2, (short) 7, (short) 3)
        ));
        calc.rebuild();

        assertThat(calc.yieldPerRotation("28421-2M800", "LP")).contains(14);
        assertThat(calc.yieldPerRotation("28421-2M800", "IC")).contains(6);
    }

    @Test
    @DisplayName("한 머신만 가능 — LP null molds → IC 만 yield (29689-2U000 패턴)")
    void only_one_machine_eligible() {
        when(lookup.findAll()).thenReturn(List.of(
            cs("29689-2U000", (short) 2, null, (short) 1)
        ));
        calc.rebuild();

        assertThat(calc.yieldPerRotation("29689-2U000", "LP")).isEmpty();
        assertThat(calc.yieldPerRotation("29689-2U000", "IC")).contains(2);
    }

    @Test
    @DisplayName("양쪽 0 (28415-08400 패턴) — unschedulable 분류")
    void both_zero_unschedulable() {
        when(lookup.findAll()).thenReturn(List.of(
            cs("28415-08400", (short) 6, null, null)
        ));
        YieldMatrix m = calc.rebuild();

        assertThat(calc.yieldPerRotation("28415-08400", "LP")).isEmpty();
        assertThat(calc.yieldPerRotation("28415-08400", "IC")).isEmpty();
        assertThat(m.isUnschedulable("28415-08400")).isTrue();
    }

    @Test
    @DisplayName("version monotonic — rebuild 마다 +1")
    void version_monotonic() {
        when(lookup.findAll()).thenReturn(List.of());
        int v1 = calc.rebuild().version();
        int v2 = calc.rebuild().version();
        assertThat(v2).isGreaterThan(v1);
    }

    @Test
    @DisplayName("미지 품번 → empty")
    void unknown_hose_id_empty() {
        when(lookup.findAll()).thenReturn(List.of(
            cs("KNOWN", (short) 1, (short) 1, (short) 1)
        ));
        calc.rebuild();
        assertThat(calc.yieldPerRotation("UNKNOWN", "LP")).isEmpty();
        assertThat(calc.yieldPerRotation("UNKNOWN", "IC")).isEmpty();
    }

    @Test
    @DisplayName("currentMatrix — initialBuild 전이면 자동 rebuild (lazy fallback)")
    void current_matrix_lazy_rebuild() {
        when(lookup.findAll()).thenReturn(List.of(cs("X", (short) 1, (short) 1, (short) 1)));
        // initialBuild 호출 X — yieldPerRotation 가 매트릭스 null 감지 후 rebuild
        assertThat(calc.yieldPerRotation("X", "LP")).contains(1);
    }

    @Test
    @DisplayName("metrics — rebuild 카운터 + duration timer emit")
    void metrics_emitted() {
        when(lookup.findAll()).thenReturn(List.of());
        calc.rebuild();
        verify(metrics).increment("vc_yield", "rebuild");
    }
}
