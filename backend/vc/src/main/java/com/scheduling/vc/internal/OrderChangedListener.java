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
 * Sprint 2 (VC 스케줄링) 에서 실제 replan 로직 추가. 본 Task는 모듈 간 이벤트 통신
 * 골격 검증용 — 로그만 출력.
 */
@Component
class OrderChangedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderChangedListener.class);

    @ApplicationModuleListener
    void on(OrderChangedEvent event) {
        log.info("OrderChangedEvent received: orderId={} hoseId={} severity={}",
                 event.orderId(), event.hoseId(), event.severity());
        // TODO Sprint 2 (TK-13-*) — partial replan 로직
    }
}
