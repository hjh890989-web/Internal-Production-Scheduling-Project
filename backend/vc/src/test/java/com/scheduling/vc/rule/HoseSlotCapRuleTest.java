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
 * HoseSlotCapRule 단위 — TK-21-3-2 / TK-21-4-1 (BR-V14·V15·V16).
 */
class HoseSlotCapRuleTest {

    private HoseRuleLookup lookup;
    private HoseSlotCapRule rule;

    @BeforeEach
    void setUp() {
        lookup = mock(HoseRuleLookup.class);
        rule = new HoseSlotCapRule(lookup);
    }

    private VcHoseRuleSummary cap(String hose, int max, String sideLock) {
        return new VcHoseRuleSummary(hose, null, max, sideLock, false);
    }

    // ---------- max_concurrent_slots ----------

    @Test
    @DisplayName("28422-08HA0 (max=1) — current=0 pass, current=1 fail")
    void cap_one_strict() {
        when(lookup.findById(eq("28422-08HA0"))).thenReturn(Optional.of(
            cap("28422-08HA0", 1, null)));

        assertThat(rule.fitsCap("28422-08HA0", 0)).isTrue();
        assertThat(rule.fitsCap("28422-08HA0", 1)).isFalse();
    }

    @Test
    @DisplayName("28422-2M800 (max=2) — current ≤ 1 pass, current ≥ 2 fail")
    void cap_two() {
        when(lookup.findById(eq("28422-2M800"))).thenReturn(Optional.of(
            cap("28422-2M800", 2, "RIGHT")));

        assertThat(rule.fitsCap("28422-2M800", 0)).isTrue();
        assertThat(rule.fitsCap("28422-2M800", 1)).isTrue();
        assertThat(rule.fitsCap("28422-2M800", 2)).isFalse();
    }

    @Test
    @DisplayName("룰 없는 hose → max=99 (사실상 무제한)")
    void no_rule_unlimited() {
        when(lookup.findById(eq("FREE"))).thenReturn(Optional.empty());

        assertThat(rule.maxConcurrentSlots("FREE")).isEqualTo(99);
        assertThat(rule.fitsCap("FREE", 50)).isTrue();
        assertThat(rule.fitsCap("FREE", 98)).isTrue();
        assertThat(rule.fitsCap("FREE", 99)).isFalse();
    }

    // ---------- side_lock ----------

    @Test
    @DisplayName("28422-2M800 (side_lock=RIGHT) — LP-03/04 pass, LP-01/02 fail")
    void side_lock_right_filters_left_machines() {
        when(lookup.findById(eq("28422-2M800"))).thenReturn(Optional.of(
            cap("28422-2M800", 2, "RIGHT")));

        assertThat(rule.fitsSide("28422-2M800", "LP-03")).isTrue();
        assertThat(rule.fitsSide("28422-2M800", "LP-04")).isTrue();
        assertThat(rule.fitsSide("28422-2M800", "LP-01")).isFalse();
        assertThat(rule.fitsSide("28422-2M800", "LP-02")).isFalse();
    }

    @Test
    @DisplayName("28421-2M800 (side_lock=LEFT) — LP-01/02 pass, LP-03/04 fail")
    void side_lock_left_filters_right_machines() {
        when(lookup.findById(eq("28421-2M800"))).thenReturn(Optional.of(
            cap("28421-2M800", 2, "LEFT")));

        assertThat(rule.fitsSide("28421-2M800", "LP-01")).isTrue();
        assertThat(rule.fitsSide("28421-2M800", "LP-02")).isTrue();
        assertThat(rule.fitsSide("28421-2M800", "LP-03")).isFalse();
        assertThat(rule.fitsSide("28421-2M800", "LP-04")).isFalse();
    }

    @Test
    @DisplayName("side_lock 있어도 IC 머신 → pass (LP-only rule)")
    void side_lock_ignores_ic() {
        when(lookup.findById(eq("28422-2M800"))).thenReturn(Optional.of(
            cap("28422-2M800", 2, "RIGHT")));

        assertThat(rule.fitsSide("28422-2M800", "IC-01")).isTrue();
    }

    @Test
    @DisplayName("side_lock NULL → 모든 LP pass")
    void no_side_lock_passes_all() {
        when(lookup.findById(eq("FREE"))).thenReturn(Optional.of(
            cap("FREE", 2, null)));

        for (String m : new String[]{"LP-01", "LP-02", "LP-03", "LP-04"}) {
            assertThat(rule.fitsSide("FREE", m)).as("free side %s", m).isTrue();
        }
    }

    @Test
    @DisplayName("마스터 미등록 hose → fail-open")
    void unknown_hose_passes() {
        when(lookup.findById(eq("UNKNOWN"))).thenReturn(Optional.empty());

        assertThat(rule.fitsSide("UNKNOWN", "LP-01")).isTrue();
    }
}
