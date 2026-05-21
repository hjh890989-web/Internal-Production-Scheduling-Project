package com.scheduling.master.vc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VcHoseRule 단위 — TK-21-2-1.
 */
class VcHoseRuleTest {

    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    @Test
    @DisplayName("28422-08HA0 — LP-01 단일 셋팅 + 동시 1슬롯 + lpOnly")
    void rule_28422_08HA0() {
        VcHoseRule r = new VcHoseRule("28422-08HA0", "LP-01", 1, null, true,
            "BR-V14", T0, "seed");
        assertThat(r.hasMachinePin()).isTrue();
        assertThat(r.getMachinePin()).isEqualTo("LP-01");
        assertThat(r.getMaxConcurrentSlots()).isEqualTo(1);
        assertThat(r.hasSideLock()).isFalse();
        assertThat(r.isLpOnly()).isTrue();
    }

    @Test
    @DisplayName("28422-2M800 — RIGHT side_lock + 동시 ≤2")
    void rule_28422_2M800() {
        VcHoseRule r = new VcHoseRule("28422-2M800", null, 2, "RIGHT", false,
            "BR-V16", T0, "seed");
        assertThat(r.hasMachinePin()).isFalse();
        assertThat(r.hasSideLock()).isTrue();
        assertThat(r.getSideLock()).isEqualTo("RIGHT");
        assertThat(r.getMaxConcurrentSlots()).isEqualTo(2);
    }

    @Test
    @DisplayName("maxConcurrentSlots 범위 외 (0, 100) → IllegalArgumentException")
    void max_concurrent_out_of_range() {
        assertThatThrownBy(() -> new VcHoseRule("X", null, 0, null, false, null, T0, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maxConcurrentSlots");
        assertThatThrownBy(() -> new VcHoseRule("X", null, 100, null, false, null, T0, "test"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("side_lock 도메인 외 ('TOP') → IllegalArgumentException")
    void invalid_side_lock() {
        assertThatThrownBy(() -> new VcHoseRule("X", null, 1, "TOP", false, null, T0, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("sideLock");
    }

    @Test
    @DisplayName("updatedAt null → IllegalArgumentException (BR-X04)")
    void null_updated_at_throws() {
        assertThatThrownBy(() -> new VcHoseRule("X", null, 1, null, false, null, null, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("updatedAt");
    }
}
