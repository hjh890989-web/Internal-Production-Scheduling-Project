package com.scheduling.notify;

import com.scheduling.order.events.OrderCommittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Sprint 13 EP-OC-FULL ST-OC-5 — PLANNER 가 수주 import 확정 시 알림 발행.
 *
 * <p>{@link ApplicationModuleListener} = {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async}.
 * commit transaction 완료 후 비동기 처리 — PLANNER 확정 응답 latency 영향 X.
 *
 * <p>Sprint 13 baseline 은 LOG 만 — Sprint 18 EP-NOTIFY 진입 시 in-app STOMP + 카카오 통합
 * ({@link NotificationService#notify} 패턴 동일하게 OrderCommittedEvent → Notification 변환).
 */
@Component
class OrderCommittedNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCommittedNotificationListener.class);

    @ApplicationModuleListener
    void on(OrderCommittedEvent event) {
        // Sprint 13 baseline — LOG only. Sprint 18 에서 NotificationService 통합:
        //   - PLANNER 본인 + IT_OPS 에게 in-app push
        //   - 카카오 미발송 (PLANNER 행동의 정상 흐름, critical 아님)
        log.info("EP-OC-FULL committed — tracking={} by {} at {} reason={}",
            event.trackingId(), event.committedBy(), event.committedAt(), event.reason());
    }
}
