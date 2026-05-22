package com.scheduling.vc.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VcSchedule.confirm() 상태 전이 — TK-10-1-2 (EP-10 ST-10-1, BR-X01).
 */
class VcScheduleConfirmTest {

    private static final Instant NOW = Instant.parse("2026-05-22T01:00:00Z");
    private static final Instant CREATED = Instant.parse("2026-05-20T00:00:00Z");
    private static final LocalDate PROD = LocalDate.of(2026, 5, 25);

    private VcSchedule newCandidate() {
        return new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            (short) 1, PROD, (short) 5,
            "ANG-A", 100, VcScheduleStatus.CANDIDATE,
            "", CREATED, CREATED);
    }

    @Test
    @DisplayName("CANDIDATE → CONFIRMED 정상 전이 + confirmedAt/By set")
    void candidate_to_confirmed_sets_audit_fields() {
        VcSchedule s = newCandidate();
        s.confirm("planner-001", NOW);
        assertThat(s.getStatus()).isEqualTo(VcScheduleStatus.CONFIRMED);
        assertThat(s.getConfirmedAt()).isEqualTo(NOW);
        assertThat(s.getConfirmedBy()).isEqualTo("planner-001");
        assertThat(s.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("이미 CONFIRMED 인 row 재확정 → IllegalStateException (BR-X01)")
    void already_confirmed_rejects_reconfirm() {
        VcSchedule s = newCandidate();
        s.confirm("planner-001", NOW);
        assertThatThrownBy(() -> s.confirm("planner-002", NOW.plusSeconds(60)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("BR-X01");
    }

    @Test
    @DisplayName("DONE 인 row 확정 시도 → IllegalStateException")
    void done_rejects_confirm() {
        VcSchedule s = newCandidate();
        s.setStatus(VcScheduleStatus.DONE);
        assertThatThrownBy(() -> s.confirm("planner-001", NOW))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DONE");
    }

    @Test
    @DisplayName("plannerId null/blank → IllegalArgumentException (RBAC)")
    void blank_planner_id_rejected() {
        VcSchedule s = newCandidate();
        assertThatThrownBy(() -> s.confirm(null, NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("plannerId");
        assertThatThrownBy(() -> s.confirm("   ", NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("plannerId");
    }
}
