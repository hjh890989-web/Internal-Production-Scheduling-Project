package com.scheduling.vc.capacity_overflow;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.master.api.ProductPriorityLookup;
import com.scheduling.vc.events.CapacityOverflowAcceptedEvent;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public CapacityOverflowApprovalService(CapacityOverflowRequestRepository requestRepo,
                                            ProductPriorityLookup priorityLookup,
                                            ApplicationEventPublisher eventPublisher,
                                            Clock clock) {
        this.requestRepo = requestRepo;
        this.priorityLookup = priorityLookup;
        this.eventPublisher = eventPublisher;
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

    /**
     * Planner 1클릭 승인 — Sprint 9 EP-V12-Allocator-Chain 진입점.
     *
     * <p>{@link CapacityOverflowAcceptedEvent} 발행 → 후속 listener (AllocatorChainListener)
     * 가 vc_schedule INSERT chain 진입 (Sprint 9 본 turn 은 listener stub, 실 Allocator
     * 호출은 베타 운영 후 별 turn).
     */
    @Auditable("BR-V12 추가 요청 큐 Planner 승인")
    @Transactional
    public CapacityOverflowRequest accept(UUID requestId, String plannerId, String note) {
        CapacityOverflowRequest req = loadPending(requestId);
        req.accept(plannerId, clock.instant(), note);
        log.info("BR-V12 accept — request={} by {}", requestId, plannerId);
        eventPublisher.publishEvent(new CapacityOverflowAcceptedEvent(
            req.getRequestId(), req.getHoseId(), req.getRequestedQty(),
            req.getPriorityRank(), plannerId, clock.instant()));
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

    /** Sprint 9 EP-V12-Auto-Expire — 24h 보존 임계 (Planner 잊은 PENDING 누적 방지). */
    static final Duration AUTO_EXPIRE_THRESHOLD = Duration.ofHours(24);
    static final String AUTO_EXPIRE_REASON = "auto-expired after 24h (Sprint 9 EP-V12-Auto-Expire)";
    static final String AUTO_EXPIRE_ACTOR = "system";

    /**
     * Sprint 9 EP-V12-Auto-Expire — 매일 03:00 KST PENDING > 24h 자동 REJECTED.
     *
     * <p>운영 시간 외 (Planner 일과 시작 전) 실행 — 일과 중 변동 충돌 회피. 본 메서드는
     * 단순 batch — V034 trigger 가 PENDING 외 status 변경 차단 (이중 보장).
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void expirePendingScheduled() {
        int expired = expirePending();
        if (expired > 0) {
            log.info("BR-V12 auto-expire — {} requests PENDING → REJECTED (24h threshold)", expired);
        }
    }

    /** 본 메서드는 직접 호출 가능 (IT + 수동 cleanup). 반환 — 만료 처리한 row 수. */
    @Auditable("BR-V12 추가 요청 큐 24h 자동 만료 (Sprint 9 EP-V12-Auto-Expire)")
    @Transactional
    public int expirePending() {
        Instant threshold = clock.instant().minus(AUTO_EXPIRE_THRESHOLD);
        List<CapacityOverflowRequest> stale = requestRepo.findByStatusAndRequestedAtBefore(
            CapacityOverflowRequest.Status.PENDING, threshold);
        for (CapacityOverflowRequest req : stale) {
            req.reject(AUTO_EXPIRE_ACTOR, clock.instant(), AUTO_EXPIRE_REASON);
        }
        return stale.size();
    }
}
