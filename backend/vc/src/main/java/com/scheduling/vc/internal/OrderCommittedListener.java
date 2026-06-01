package com.scheduling.vc.internal;

import com.scheduling.order.events.OrderCommittedEvent;
import com.scheduling.vc.draft.VcScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Sprint 14 EP-VC-FULL ST-VC-1 — Sprint 13 publisher 와 chain 완성.
 * Sprint 26 S26-A ST-ORDER-1 — Phase 4 carry-over: auto-draft chain 활성.
 *
 * <p>PLANNER 가 수주 import 확정 시 (POST /api/v1/orders/{trackingId}/commit) 발행되는
 * {@link OrderCommittedEvent} 를 구독 → {@link VcScheduleService#draftBatch} 조건부 호출.
 *
 * <p>{@link ApplicationModuleListener} = {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async}.
 * Order commit transaction 완료 후 비동기 처리 — PLANNER 응답 latency 영향 X.
 *
 * <p>config flag {@code scheduling.order.auto-draft.enabled} (default false) 로 ON/OFF.
 * false 이면 기존 Sprint 14 baseline 동작 (log only) — 운영 안전 확보.
 *
 * <p>OrderChangedListener 와 의미가 다름:
 * <ul>
 *   <li>OrderChangedListener — row-level 변경 (partial replan 트리거)</li>
 *   <li>OrderCommittedListener — trackingId 단위 PLANNER 확정 (전체 schedule chain 진입점)</li>
 * </ul>
 *
 * @see BR-X02 (audit 강제 — VcScheduleService.draftBatch @Auditable)
 */
@Component
@Profile("with-infra")
class OrderCommittedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCommittedListener.class);

    private final VcScheduleService vcScheduleService;

    OrderCommittedListener(VcScheduleService vcScheduleService) {
        this.vcScheduleService = vcScheduleService;
    }

    @ApplicationModuleListener
    void on(OrderCommittedEvent event) {
        log.info("ST-ORDER-1 OrderCommittedEvent received — tracking={} by={} at={} reason='{}'",
                event.trackingId(), event.committedBy(), event.committedAt(), event.reason());

        if (vcScheduleService.isAutoDraftEnabled()) {
            log.info("ST-ORDER-1 auto-draft enabled — invoking draftBatch tracking={}",
                    event.trackingId());
            vcScheduleService.draftBatch(
                    event.trackingId(),
                    "system",
                    "auto-chain-from-order-commit");
        } else {
            log.info("ST-ORDER-1 auto-draft disabled (scheduling.order.auto-draft.enabled=false)" +
                    " — skipping draftBatch tracking={}", event.trackingId());
        }
    }
}
