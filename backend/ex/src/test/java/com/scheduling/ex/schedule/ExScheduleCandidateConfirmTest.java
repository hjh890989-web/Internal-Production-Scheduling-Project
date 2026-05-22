package com.scheduling.ex.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExScheduleCandidate.confirm() 상태 전이 — TK-10-2-1 (BR-X01).
 */
class ExScheduleCandidateConfirmTest {

    private static final Instant NOW = Instant.parse("2026-05-22T01:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-05-20T00:00:00Z");
    private static final LocalDate VC_DATE = LocalDate.of(2026, 5, 25);
    private static final LocalDate EX_DEADLINE = LocalDate.of(2026, 5, 24);

    private ExScheduleCandidate newScheduled() {
        ExScheduleCandidate c = new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            UUID.randomUUID(), VC_DATE, EX_DEADLINE, 2531,
            CandidateStatus.SCHEDULED, CREATED, CREATED);
        return c;
    }

    @Test
    @DisplayName("SCHEDULED → CONFIRMED 정상 전이 + audit fields")
    void scheduled_to_confirmed() {
        ExScheduleCandidate c = newScheduled();
        c.confirm("planner-001", NOW);
        assertThat(c.getStatus()).isEqualTo(CandidateStatus.CONFIRMED);
        assertThat(c.getConfirmedAt()).isEqualTo(NOW);
        assertThat(c.getConfirmedBy()).isEqualTo("planner-001");
        assertThat(c.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("PENDING 상태 확정 시도 → IllegalStateException")
    void pending_rejects_confirm() {
        ExScheduleCandidate c = new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            UUID.randomUUID(), VC_DATE, EX_DEADLINE, 2531,
            CandidateStatus.PENDING, CREATED, CREATED);
        assertThatThrownBy(() -> c.confirm("planner-001", NOW))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SCHEDULED");
    }

    @Test
    @DisplayName("FAILED row 확정 시도 → IllegalStateException")
    void failed_rejects_confirm() {
        ExScheduleCandidate c = new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            UUID.randomUUID(), VC_DATE, EX_DEADLINE, 2531,
            CandidateStatus.FAILED, CREATED, CREATED);
        assertThatThrownBy(() -> c.confirm("planner-001", NOW))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("FAILED");
    }

    @Test
    @DisplayName("plannerId blank → IllegalArgumentException")
    void blank_planner_rejected() {
        ExScheduleCandidate c = newScheduled();
        assertThatThrownBy(() -> c.confirm("", NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("plannerId");
    }
}
