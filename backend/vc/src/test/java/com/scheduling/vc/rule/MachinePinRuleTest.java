package com.scheduling.vc.rule;

import com.scheduling.master.api.HoseRuleLookup;
import com.scheduling.master.api.VcHoseRuleSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MachinePinRule 단위 — TK-21-3-1 (BR-V14).
 */
class MachinePinRuleTest {

    private HoseRuleLookup lookup;
    private MachinePinRule rule;

    @BeforeEach
    void setUp() {
        lookup = mock(HoseRuleLookup.class);
        rule = new MachinePinRule(lookup);
    }

    private VcHoseRuleSummary hoseRule(String hose, String pin, boolean lpOnly) {
        return new VcHoseRuleSummary(hose, pin, 1, null, lpOnly);
    }

    @Test
    @DisplayName("28422-08HA0 — machine_pin=LP-01, lp_only=TRUE → LP-01만 pass")
    void rule_28422_08HA0_only_LP01() {
        when(lookup.findById(eq("28422-08HA0"))).thenReturn(Optional.of(
            hoseRule("28422-08HA0", "LP-01", true)));

        assertThat(rule.validate("28422-08HA0", "LP-01")).isTrue();
        assertThat(rule.validate("28422-08HA0", "LP-02")).isFalse();
        assertThat(rule.validate("28422-08HA0", "LP-03")).isFalse();
        assertThat(rule.validate("28422-08HA0", "LP-04")).isFalse();
    }

    @Test
    @DisplayName("28422-08HA0 lp_only=TRUE + IC → fail")
    void lp_only_rejects_ic() {
        when(lookup.findById(eq("28422-08HA0"))).thenReturn(Optional.of(
            hoseRule("28422-08HA0", "LP-01", true)));

        assertThat(rule.validate("28422-08HA0", "IC-01")).isFalse();
    }

    @Test
    @DisplayName("machine_pin NULL → 모든 머신 pass")
    void no_pin_passes_all() {
        when(lookup.findById(eq("FREE"))).thenReturn(Optional.of(
            new VcHoseRuleSummary("FREE", null, 99, null, false)));

        for (String m : new String[]{"LP-01", "LP-02", "LP-03", "LP-04", "IC-01"}) {
            assertThat(rule.validate("FREE", m)).as("free hose %s", m).isTrue();
        }
    }

    @Test
    @DisplayName("lp_only=FALSE + machine_pin=NULL — IC도 pass")
    void no_constraints_allows_ic() {
        when(lookup.findById(eq("BOTH"))).thenReturn(Optional.of(
            new VcHoseRuleSummary("BOTH", null, 2, "LEFT", false)));

        assertThat(rule.validate("BOTH", "IC-01")).isTrue();
        assertThat(rule.validate("BOTH", "LP-01")).isTrue();
    }

    @Test
    @DisplayName("마스터 미등록 hose → fail-open (Unschedulable rule 별도 차단)")
    void unknown_hose_passes() {
        when(lookup.findById(eq("UNKNOWN"))).thenReturn(Optional.empty());

        assertThat(rule.validate("UNKNOWN", "LP-01")).isTrue();
        assertThat(rule.validate("UNKNOWN", "IC-01")).isTrue();
    }
}
