package com.scheduling.vc.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VcSchedule.applyOverride() 도메인 검증 — TK-13-4-1 (BR-V07).
 */
class VcScheduleOverrideTest {

    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-05-25T01:00:00Z");
    private static final LocalDate PROD = LocalDate.of(2026, 5, 25);

    private VcSchedule newRow() {
        return new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 5,
            "ANG-A", 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);
    }

    @Test
    @DisplayName("applyOverride — reason + overrideBy set + updatedAt 갱신")
    void apply_override_sets_fields() {
        VcSchedule s = newRow();
        s.applyOverride("긴급 LOT 변경 요청", "planner-001", NOW);
        assertThat(s.getOverrideReason()).isEqualTo("긴급 LOT 변경 요청");
        assertThat(s.getOverrideBy()).isEqualTo("planner-001");
        assertThat(s.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("blank reason → IllegalArgumentException (BR-V07)")
    void blank_reason_rejected() {
        VcSchedule s = newRow();
        assertThatThrownBy(() -> s.applyOverride(" ", "planner-001", NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("override_reason");
        assertThatThrownBy(() -> s.applyOverride(null, "planner-001", NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("override_reason");
    }

    @Test
    @DisplayName("blank overrideBy → IllegalArgumentException (BR-V07 actor)")
    void blank_override_by_rejected() {
        VcSchedule s = newRow();
        assertThatThrownBy(() -> s.applyOverride("긴급 사유", "", NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("override_by");
        assertThatThrownBy(() -> s.applyOverride("긴급 사유", null, NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("override_by");
    }
}
