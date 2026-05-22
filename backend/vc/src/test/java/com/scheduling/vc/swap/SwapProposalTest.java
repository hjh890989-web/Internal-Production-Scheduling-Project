package com.scheduling.vc.swap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SwapProposal 도메인 invariant — TK-15-2-2 (EP-15 ST-15-2).
 */
class SwapProposalTest {

    private static final Instant T0 = Instant.parse("2026-05-22T01:00:00Z");
    private static final Instant NOW = Instant.parse("2026-05-22T02:00:00Z");

    private SwapProposal proposed(UUID src, UUID tgt) {
        return new SwapProposal(UUID.randomUUID(), src, tgt,
            "stk-001", T0, "현장 우선순위 조정");
    }

    @Test
    @DisplayName("초기 상태 PROPOSED — accept → ACCEPTED + resolvedAt/By")
    void accept_resolves_proposal() {
        SwapProposal p = proposed(UUID.randomUUID(), UUID.randomUUID());
        assertThat(p.getStatus()).isEqualTo(SwapStatus.PROPOSED);

        p.accept("planner-001", NOW, "OK");

        assertThat(p.getStatus()).isEqualTo(SwapStatus.ACCEPTED);
        assertThat(p.getResolvedBy()).isEqualTo("planner-001");
        assertThat(p.getResolvedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("reject → REJECTED + 사유")
    void reject_resolves_proposal() {
        SwapProposal p = proposed(UUID.randomUUID(), UUID.randomUUID());
        p.reject("planner-002", NOW, "총량 보존 불가");
        assertThat(p.getStatus()).isEqualTo(SwapStatus.REJECTED);
        assertThat(p.getResolutionNote()).isEqualTo("총량 보존 불가");
    }

    @Test
    @DisplayName("이미 resolved → IllegalStateException (중복 처리 차단)")
    void already_resolved_rejected() {
        SwapProposal p = proposed(UUID.randomUUID(), UUID.randomUUID());
        p.accept("planner-001", NOW, null);
        assertThatThrownBy(() -> p.accept("planner-002", NOW.plusSeconds(60), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("resolved");
        assertThatThrownBy(() -> p.reject("planner-002", NOW.plusSeconds(60), null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("source == target → IllegalArgumentException (생성자)")
    void same_source_target_rejected() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new SwapProposal(UUID.randomUUID(), id, id,
            "stk-001", T0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("source_row_id == target_row_id");
    }

    @Test
    @DisplayName("blank proposedBy → IllegalArgumentException (RBAC actor)")
    void blank_proposed_by_rejected() {
        assertThatThrownBy(() -> new SwapProposal(UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), " ", T0, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("proposedBy");
    }

    @Test
    @DisplayName("blank plannerId 수용 → IllegalArgumentException (RBAC)")
    void blank_planner_id_rejected() {
        SwapProposal p = proposed(UUID.randomUUID(), UUID.randomUUID());
        assertThatThrownBy(() -> p.accept("", NOW, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("plannerId");
    }
}
