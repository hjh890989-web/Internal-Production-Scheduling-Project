package com.scheduling.vc.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 9 EP-V12-Allocator-Chain — Planner ACCEPT 시점 발행 (REQ-FUNC-VC-022).
 *
 * <p>{@code CapacityOverflowApprovalService.accept()} 가 발행 → 후속 Allocator chain
 * (vc.allocator.GreedyRotationAllocator 호출 → vc_schedule INSERT) 진입점.
 *
 * <p>본 Sprint 9 turn 은 Event publish + Listener log/audit stub 만 마감. 실 Allocator
 * 호출은 베타 운영 후 실 요구 식별 시점 별 turn 진행 (가설 기반 over-engineering 회피).
 *
 * @param requestId     capacity_overflow_request PK
 * @param hoseId        승인된 hose
 * @param requestedQty  승인된 qty
 * @param priorityRank  split() 시점 보존된 rank
 * @param acceptedBy    Planner (audit attribution)
 * @param acceptedAt    승인 시각 (Clock 주입, BR-X04)
 */
public record CapacityOverflowAcceptedEvent(
    UUID requestId,
    String hoseId,
    int requestedQty,
    short priorityRank,
    String acceptedBy,
    Instant acceptedAt
) {}
