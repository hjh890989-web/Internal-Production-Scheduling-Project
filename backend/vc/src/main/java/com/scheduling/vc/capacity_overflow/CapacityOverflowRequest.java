package com.scheduling.vc.capacity_overflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 8 BR-V12 capa 초과 추가 요청 큐 — REQ-FUNC-VC-022.
 *
 * <p>{@link CapacityOverflowQueueService#split} 결과의 requestQueue 를 영속화. Planner 가
 * 1클릭 승인/거절 — 상태 머신 PENDING → ACCEPTED|REJECTED (한 번만 전이, V034 trigger 강제).
 *
 * <p>{@code priority_rank} 는 split() 시점의 rank 를 보존 — 마스터 변경 영향 0 (audit trail).
 */
@Entity
@Table(name = "capacity_overflow_request", schema = "app")
public class CapacityOverflowRequest {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "hose_id", nullable = false, length = 50)
    private String hoseId;

    @Column(name = "requested_qty", nullable = false)
    private int requestedQty;

    @Column(name = "priority_rank", nullable = false)
    private short priorityRank;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "requested_by", nullable = false, length = 50)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 50)
    private String decidedBy;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    protected CapacityOverflowRequest() {}

    public CapacityOverflowRequest(UUID requestId, String hoseId, int requestedQty,
                                    short priorityRank, Instant requestedAt, String requestedBy) {
        if (requestedQty <= 0) {
            throw new IllegalArgumentException("requestedQty > 0 (현재: " + requestedQty + ")");
        }
        if (hoseId == null || hoseId.isBlank()) {
            throw new IllegalArgumentException("hoseId 필수");
        }
        if (requestedBy == null || requestedBy.isBlank()) {
            throw new IllegalArgumentException("requestedBy 필수 (audit actor)");
        }
        this.requestId = requestId;
        this.hoseId = hoseId;
        this.requestedQty = requestedQty;
        this.priorityRank = priorityRank;
        this.requestedAt = requestedAt;
        this.requestedBy = requestedBy;
        this.status = Status.PENDING;
    }

    public void accept(String plannerId, Instant now, String reason) {
        decide(Status.ACCEPTED, plannerId, now, reason);
    }

    public void reject(String plannerId, Instant now, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("REJECT 시 reason 필수 (BR-X02 audit)");
        }
        decide(Status.REJECTED, plannerId, now, reason);
    }

    private void decide(Status next, String plannerId, Instant now, String reason) {
        if (status != Status.PENDING) {
            throw new IllegalStateException(
                "capacity_overflow_request 이미 결정됨: " + status);
        }
        if (plannerId == null || plannerId.isBlank()) {
            throw new IllegalArgumentException("plannerId 필수 (RBAC)");
        }
        this.status = next;
        this.decidedBy = plannerId;
        this.decidedAt = now;
        this.decisionReason = reason;
    }

    public UUID getRequestId() { return requestId; }
    public String getHoseId() { return hoseId; }
    public int getRequestedQty() { return requestedQty; }
    public short getPriorityRank() { return priorityRank; }
    public Instant getRequestedAt() { return requestedAt; }
    public String getRequestedBy() { return requestedBy; }
    public Status getStatus() { return status; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getDecidedBy() { return decidedBy; }
    public String getDecisionReason() { return decisionReason; }

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}
