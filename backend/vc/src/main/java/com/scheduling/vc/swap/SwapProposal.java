package com.scheduling.vc.swap;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * STK_USER → Planner swap 제안 — TK-15-2-2 (EP-15 ST-15-2, REQ-FUNC-VC-018).
 *
 * <p>두 row 의 rotation_no swap 제안. 상태: PROPOSED → ACCEPTED / REJECTED.
 * 수용 시 atomic swap (총량 보존 invariant) — {@link SwapProposalService}.
 */
@Entity
@Table(name = "vc_schedule_swap_proposal", schema = "app")
public class SwapProposal {

    @Id
    @Column(name = "proposal_id", nullable = false, updatable = false)
    private UUID proposalId;

    @Column(name = "source_row_id", nullable = false)
    private UUID sourceRowId;

    @Column(name = "target_row_id", nullable = false)
    private UUID targetRowId;

    @Column(name = "proposed_by", nullable = false, length = 40)
    private String proposedBy;

    @Column(name = "proposed_at", nullable = false)
    private Instant proposedAt;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private SwapStatus status;

    @Column(name = "resolved_by", length = 40)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", columnDefinition = "text")
    private String resolutionNote;

    protected SwapProposal() {}

    public SwapProposal(UUID proposalId, UUID sourceRowId, UUID targetRowId,
                        String proposedBy, Instant proposedAt, String reason) {
        if (sourceRowId.equals(targetRowId)) {
            throw new IllegalArgumentException("source_row_id == target_row_id 금지");
        }
        if (proposedBy == null || proposedBy.isBlank()) {
            throw new IllegalArgumentException("proposedBy 필수 (RBAC actor)");
        }
        this.proposalId = proposalId;
        this.sourceRowId = sourceRowId;
        this.targetRowId = targetRowId;
        this.proposedBy = proposedBy;
        this.proposedAt = proposedAt;
        this.reason = reason;
        this.status = SwapStatus.PROPOSED;
    }

    public UUID getProposalId() { return proposalId; }
    public UUID getSourceRowId() { return sourceRowId; }
    public UUID getTargetRowId() { return targetRowId; }
    public String getProposedBy() { return proposedBy; }
    public Instant getProposedAt() { return proposedAt; }
    public String getReason() { return reason; }
    public SwapStatus getStatus() { return status; }
    public String getResolvedBy() { return resolvedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }

    public void accept(String plannerId, Instant now, String note) {
        resolve(SwapStatus.ACCEPTED, plannerId, now, note);
    }

    public void reject(String plannerId, Instant now, String note) {
        resolve(SwapStatus.REJECTED, plannerId, now, note);
    }

    private void resolve(SwapStatus next, String plannerId, Instant now, String note) {
        if (status != SwapStatus.PROPOSED) {
            throw new IllegalStateException(
                "swap proposal 이미 resolved: " + status);
        }
        if (plannerId == null || plannerId.isBlank()) {
            throw new IllegalArgumentException("plannerId 필수 (BR-X01 RBAC)");
        }
        this.status = next;
        this.resolvedBy = plannerId;
        this.resolvedAt = now;
        this.resolutionNote = note;
    }
}
