package com.scheduling.vc.rule;

import com.scheduling.master.api.VcConstraintLookup;
import com.scheduling.master.api.VcConstraintSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * LeftRightRule 단위 — TK-21-1-2 (BR-V15·V16).
 */
class LeftRightRuleTest {

    private VcConstraintLookup lookup;
    private LeftRightRule rule;

    @BeforeEach
    void setUp() {
        lookup = mock(VcConstraintLookup.class);
        rule = new LeftRightRule(lookup);
    }

    private VcConstraintSummary summary(String hose, String left, String right) {
        return new VcConstraintSummary(hose, (short) 1, (short) 1, (short) 20,
            (short) 1, (short) 20, left, right);
    }

    @Test
    @DisplayName("28421-2M800 좌측 (lp_left=O) + LP-01/02 (LEFT) → pass")
    void left_only_hose_on_left_machine() {
        when(lookup.findById(eq("28421-2M800"))).thenReturn(Optional.of(
            summary("28421-2M800", "O", "X")));

        assertThat(rule.validate("28421-2M800", "LP-01")).isTrue();
        assertThat(rule.validate("28421-2M800", "LP-02")).isTrue();
    }

    @Test
    @DisplayName("28421-2M800 좌측 (lp_left=O) + LP-03/04 (RIGHT) → fail (BR-V15)")
    void left_only_hose_on_right_machine() {
        when(lookup.findById(eq("28421-2M800"))).thenReturn(Optional.of(
            summary("28421-2M800", "O", "X")));

        assertThat(rule.validate("28421-2M800", "LP-03")).isFalse();
        assertThat(rule.validate("28421-2M800", "LP-04")).isFalse();
    }

    @Test
    @DisplayName("28422-2M800 우측 (lp_right=O) + LP-03/04 (RIGHT) → pass")
    void right_only_hose_on_right_machine() {
        when(lookup.findById(eq("28422-2M800"))).thenReturn(Optional.of(
            summary("28422-2M800", "X", "O")));

        assertThat(rule.validate("28422-2M800", "LP-03")).isTrue();
        assertThat(rule.validate("28422-2M800", "LP-04")).isTrue();
    }

    @Test
    @DisplayName("28422-2M800 우측 (lp_right=O) + LP-01/02 (LEFT) → fail (BR-V16)")
    void right_only_hose_on_left_machine() {
        when(lookup.findById(eq("28422-2M800"))).thenReturn(Optional.of(
            summary("28422-2M800", "X", "O")));

        assertThat(rule.validate("28422-2M800", "LP-01")).isFalse();
        assertThat(rule.validate("28422-2M800", "LP-02")).isFalse();
    }

    @Test
    @DisplayName("28422-08HA0 양쪽 (O/O) → 4 LP 머신 모두 pass")
    void both_sides_allowed_passes_all_lp() {
        when(lookup.findById(eq("28422-08HA0"))).thenReturn(Optional.of(
            summary("28422-08HA0", "O", "O")));

        for (String m : new String[]{"LP-01", "LP-02", "LP-03", "LP-04"}) {
            assertThat(rule.validate("28422-08HA0", m)).as("LP %s", m).isTrue();
        }
    }

    @Test
    @DisplayName("IC-01 머신 → rule 미적용 (fail-open, 항상 pass)")
    void ic_machine_skipped() {
        when(lookup.findById(eq("ANY"))).thenReturn(Optional.of(
            summary("ANY", "X", "X")));

        assertThat(rule.validate("ANY", "IC-01")).isTrue();  // lookup 조차 호출 안 함 (단락)
    }

    @Test
    @DisplayName("마스터 미등록 hose → fail-open (Unschedulable rule 이 별도 차단)")
    void unknown_hose_passes() {
        when(lookup.findById(eq("UNKNOWN"))).thenReturn(Optional.empty());

        assertThat(rule.validate("UNKNOWN", "LP-01")).isTrue();
    }

    @Test
    @DisplayName("기본 X/X (legacy 양쪽 'X') → LP 4대 모두 fail")
    void default_x_blocks_all_lp() {
        when(lookup.findById(eq("LEGACY"))).thenReturn(Optional.of(
            summary("LEGACY", "X", "X")));

        for (String m : new String[]{"LP-01", "LP-02", "LP-03", "LP-04"}) {
            assertThat(rule.validate("LEGACY", m)).as("LP %s", m).isFalse();
        }
    }
}
