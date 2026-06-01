package com.scheduling.vc.internal;

import com.scheduling.order.events.OrderChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 수주 변경 → vc partial replan 트리거 (REQ-FUNC-VC-015).
 *
 * @ApplicationModuleListener = @TransactionalEventListener(AFTER_COMMIT) + @Async.
 * 트랜잭션 커밋 후 비동기 처리 → 성능 + 신뢰성.
 *
 * Sprint 2 carry-over (TK-13-*) — partial replan 로직 미구현 상태 유지.
 * row-level 변경 트리거용 골격 (log only). OrderCommittedListener 와 의미 상이:
 * 본 listener 는 개별 수주 row 변경 (partial replan), OrderCommittedListener 는
 * trackingId 단위 전체 확정 (전체 schedule chain 진입점, ST-ORDER-1 활성).
 */
@Component
class OrderChangedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderChangedListener.class);

    @ApplicationModuleListener
    void on(OrderChangedEvent event) {
        log.info("OrderChangedEvent received: orderId={} hoseId={} severity={}",
                 event.orderId(), event.hoseId(), event.severity());
        // carry-over Sprint 2 (TK-13-*) — partial replan 로직 (현재 log only 유지)
    }
}
