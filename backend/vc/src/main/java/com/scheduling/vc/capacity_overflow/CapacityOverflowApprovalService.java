package com.scheduling.vc.capacity_overflow;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.master.api.ProductPriorityLookup;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sprint 8 BR-V12 추가 요청 큐 승인 워크플로우 — REQ-FUNC-VC-022 (deferred 활성).
 *
 * <p>{@link CapacityOverflowQueueService#split} 결과의 requestQueue 를 영속화 (enqueue) +
 * Planner 1클릭 승인/거절 (accept/reject) — 상태 머신 PENDING → ACCEPTED|REJECTED.
 *
 * <p>BR-X02 audit 강제 — 모든 변경 메서드 {@code @Auditable} (ScheduleAuditedEvent 발행).
 * V034 trigger 가 중복 결정 차단 (PENDING → ACCEPTED|REJECTED 후 immutable).
 */
@Service
@Profile("with-infra")
public class CapacityOverflowApprovalService {

    private static final Logger log = LoggerFactory.getLogger(CapacityOverflowApprovalService.class);

    private final CapacityOverflowRequestRepository requestRepo;
    private final ProductPriorityLookup priorityLookup;
    private final Clock clock;

    public CapacityOverflowApprovalService(CapacityOverflowRequestRepository requestRepo,
                                            ProductPriorityLookup priorityLookup,
                                            Clock clock) {
        this.requestRepo = requestRepo;
        this.priorityLookup = priorityLookup;
        this.clock = clock;
    }

    /**
     * {@code split()} 결과의 requestQueue 를 영속화 — Planner 가 명시적으로 큐 등록 시.
     *
     * @param queue      hose_id → 추가 요청 qty (split() requestQueue)
     * @param requestedBy Planner 또는 system
     * @return 영속된 request UUID 리스트
     */
    @Auditable("BR-V12 추가 요청 큐 영속화 (Sprint 8 진입)")
    @Transactional
    public List<UUID> enqueue(Map<String, Integer> queue, String requestedBy) {
        if (queue == null || queue.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now(clock);
        Map<String, Short> rankByHose = priorityLookup.findEffectiveAt(today).stream()
            .collect(java.util.stream.Collectors.toMap(
                p -> p.hoseId(),
                p -> p.priorityRank(),
                (a, b) -> a));

        List<UUID> persisted = new ArrayList<>(queue.size());
        for (Map.Entry<String, Integer> e : queue.entrySet()) {
            short rank = rankByHose.getOrDefault(e.getKey(), (short) 99);
            CapacityOverflowRequest req = new CapacityOverflowRequest(
                UUID.randomUUID(), e.getKey(), e.getValue(), rank,
                clock.instant(), requestedBy);
            persisted.add(requestRepo.save(req).getRequestId());
        }
        log.info("BR-V12 enqueue — {} requests persisted by {}", persisted.size(), requestedBy);
        return persisted;
    }

    /** Planner 1클릭 승인 — Allocator 후속 처리 (Sprint 8+ event) 진입점. */
    @Auditable("BR-V12 추가 요청 큐 Planner 승인")
    @Transactional
    public CapacityOverflowRequest accept(UUID requestId, String plannerId, String note) {
        CapacityOverflowRequest req = loadPending(requestId);
        req.accept(plannerId, clock.instant(), note);
        log.info("BR-V12 accept — request={} by {}", requestId, plannerId);
        return req;
    }

    /** Planner 1클릭 거절 — reason 필수 (BR-X02 audit). */
    @Auditable("BR-V12 추가 요청 큐 Planner 거절")
    @Transactional
    public CapacityOverflowRequest reject(UUID requestId, String plannerId, String reason) {
        CapacityOverflowRequest req = loadPending(requestId);
        req.reject(plannerId, clock.instant(), reason);
        log.info("BR-V12 reject — request={} by {}, reason={}", requestId, plannerId, reason);
        return req;
    }

    private CapacityOverflowRequest loadPending(UUID requestId) {
        return requestRepo.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException(
                "capacity_overflow_request 미존재: " + requestId));
    }
}
