package com.scheduling.vc.capacity_overflow;

import com.scheduling.vc.events.CapacityOverflowAcceptedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Sprint 9 EP-V12-Allocator-Chain — {@link CapacityOverflowAcceptedEvent} 후속 listener.
 *
 * <p>{@code @ApplicationModuleListener} = AFTER_COMMIT + Async (Modulith 표준). Planner ACCEPT
 * 트랜잭션 commit 후 비동기 처리 — Spring Modulith {@code event_publication} 영속 통해
 * 재시작 복구 보장.
 *
 * <p><strong>본 Sprint 9 turn 은 stub</strong> — log + (향후 metric counter) 만 처리.
 * 실 {@code vc_schedule} INSERT chain (GreedyRotationAllocator 호출) 은 베타 운영 후
 * 실 요구 식별 시점 별 turn 진행 (가설 기반 over-engineering 회피):
 *
 * <ul>
 *   <li>Allocator input — {@code OrderInput} (hose + qty + delivery_date + linked_order_ids 등)</li>
 *   <li>호라이즌 — 다음 영업일 기준 (BR-X07 D-2 hard 제약 정합)</li>
 *   <li>capa ledger — 기존 vc_schedule + accepted V12 합산</li>
 *   <li>BR-V07 일중 락 + setting group + side rule 5 룰 적용</li>
 * </ul>
 *
 * <p>현재는 이벤트 chain 골격 + audit trail 만 — Sprint 9+ 후속 turn 의 진입점.
 */
@Component
@Profile("with-infra")
class AllocatorChainListener {

    private static final Logger log = LoggerFactory.getLogger(AllocatorChainListener.class);

    @ApplicationModuleListener
    void on(CapacityOverflowAcceptedEvent event) {
        log.info("BR-V12 Allocator-Chain — request={} hose={} qty={} rank={} acceptedBy={} acceptedAt={}",
            event.requestId(), event.hoseId(), event.requestedQty(),
            event.priorityRank(), event.acceptedBy(), event.acceptedAt());
        // Sprint 9+ 후속 turn:
        //   1. OrderInput 생성 (hose + qty + 다음 영업일 deadline)
        //   2. GreedyRotationAllocator.allocate(input, dailyCapa) 호출
        //   3. 결과 VcSchedule list → repository.saveAll
        //   4. VcChangedEvent 발행 → ex partial replan cascade (BR-X03)
        //   5. notify (Planner 알림 — 자동 배치 결과)
    }
}
