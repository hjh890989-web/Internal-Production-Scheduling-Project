package com.scheduling.ex.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExScheduleCandidate.applyVcChange() — TK-EX13-1-3 (Sprint 4 정식 활성, BR-E11).
 */
class ExScheduleCandidateApplyVcChangeTest {

    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-05-25T01:00:00Z");
    private static final LocalDate VC_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate EX_DEADLINE = LocalDate.of(2026, 5, 29);

    private ExScheduleCandidate newScheduled() {
        return new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            UUID.randomUUID(), VC_DATE, EX_DEADLINE, 2531,
            CandidateStatus.SCHEDULED, T0, T0);
    }

    @Test
    @DisplayName("applyVcChange — newYield/deadline/vcDate 갱신 + status PENDING + updatedAt")
    void apply_resets_to_pending() {
        ExScheduleCandidate c = newScheduled();
        LocalDate newVc = LocalDate.of(2026, 6, 3);
        LocalDate newDeadline = LocalDate.of(2026, 6, 2);

        c.applyVcChange(3000, newDeadline, newVc, NOW);

        assertThat(c.getVcYield()).isEqualTo(3000);
        assertThat(c.getExtrusionDeadline()).isEqualTo(newDeadline);
        assertThat(c.getVcProductionDate()).isEqualTo(newVc);
        assertThat(c.getStatus()).isEqualTo(CandidateStatus.PENDING);
        assertThat(c.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("newYield 음수 → IllegalArgumentException")
    void negative_yield_rejected() {
        ExScheduleCandidate c = newScheduled();
        assertThatThrownBy(() -> c.applyVcChange(-1,
            LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 3), NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("newYield");
    }

    @Test
    @DisplayName("newDeadline > newVcDate → IllegalArgumentException (BR-E01)")
    void deadline_after_vc_date_rejected() {
        ExScheduleCandidate c = newScheduled();
        assertThatThrownBy(() -> c.applyVcChange(1000,
            LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 3), NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BR-E01");
    }

    @Test
    @DisplayName("null deadline/vcDate → IllegalArgumentException")
    void null_dates_rejected() {
        ExScheduleCandidate c = newScheduled();
        assertThatThrownBy(() -> c.applyVcChange(1000, null, LocalDate.now(), NOW))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> c.applyVcChange(1000, LocalDate.now(), null, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
