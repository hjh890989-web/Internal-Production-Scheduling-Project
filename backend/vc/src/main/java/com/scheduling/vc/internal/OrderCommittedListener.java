package com.scheduling.vc.internal;

import com.scheduling.order.events.OrderCommittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Sprint 14 EP-VC-FULL ST-VC-1 — Sprint 13 publisher 와 chain 완성.
 *
 * <p>PLANNER 가 수주 import 확정 시 (POST /api/v1/orders/{trackingId}/commit) 발행되는
 * {@link OrderCommittedEvent} 를 구독 → 성형 스케줄 입력 단계로 자동 진입.
 *
 * <p>{@link ApplicationModuleListener} = {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async}.
 * Order commit transaction 완료 후 비동기 처리 — PLANNER 응답 latency 영향 X.
 *
 * <p>Sprint 14 baseline 은 LOG only — 실 VC schedule 자동 INSERT 흐름은 Phase 5+ 베타 운영 후
 * priority/slot 알고리즘 결정 후 통합 (현재는 manual seed + simview 검증).
 *
 * <p>OrderChangedListener 와 의미가 다름:
 * <ul>
 *   <li>OrderChangedListener — row-level 변경 (partial replan 트리거)</li>
 *   <li>OrderCommittedListener — trackingId 단위 PLANNER 확정 (전체 schedule chain 진입점)</li>
 * </ul>
 */
@Component
class OrderCommittedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCommittedListener.class);

    @ApplicationModuleListener
    void on(OrderCommittedEvent event) {
        log.info("EP-VC-FULL OrderCommittedEvent received — tracking={} by {} at {} reason='{}'",
            event.trackingId(), event.committedBy(), event.committedAt(), event.reason());
        // TODO Phase 5+ — VcScheduleService.draftBatch(trackingId) 호출 (priority/slot 알고리즘 활성 후)
    }
}
