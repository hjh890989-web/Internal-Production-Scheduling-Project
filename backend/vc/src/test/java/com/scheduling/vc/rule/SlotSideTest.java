package com.scheduling.vc.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SlotSide 단위 — TK-21-1-2.
 *
 * <p>LP-01·02 = LEFT, LP-03·04 = RIGHT 매핑 + IC/형식 외 = Optional.empty.
 */
class SlotSideTest {

    @Test
    @DisplayName("LP-01, LP-02 → LEFT")
    void lp_low_numbers_are_left() {
        assertThat(SlotSide.ofLp("LP-01")).contains(SlotSide.LEFT);
        assertThat(SlotSide.ofLp("LP-02")).contains(SlotSide.LEFT);
    }

    @Test
    @DisplayName("LP-03, LP-04 → RIGHT")
    void lp_high_numbers_are_right() {
        assertThat(SlotSide.ofLp("LP-03")).contains(SlotSide.RIGHT);
        assertThat(SlotSide.ofLp("LP-04")).contains(SlotSide.RIGHT);
    }

    @Test
    @DisplayName("IC-01 → empty (좌/우 rule 미적용)")
    void ic_machine_returns_empty() {
        assertThat(SlotSide.ofLp("IC-01")).isEmpty();
    }

    @Test
    @DisplayName("null / 형식 외 → empty (defensive)")
    void invalid_input_returns_empty() {
        assertThat(SlotSide.ofLp(null)).isEmpty();
        assertThat(SlotSide.ofLp("")).isEmpty();
        assertThat(SlotSide.ofLp("EX-01")).isEmpty();
        assertThat(SlotSide.ofLp("LP-ABC")).isEmpty();
    }
}
