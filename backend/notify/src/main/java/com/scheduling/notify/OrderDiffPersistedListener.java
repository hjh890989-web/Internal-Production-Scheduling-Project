package com.scheduling.notify;

import com.scheduling.order.events.OrderDiffPersistedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * OrderDiffPersistedEvent → NotificationService.notify() — TK-03-3-1.
 *
 * <p>{@link ApplicationModuleListener} = {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async}.
 * Order diff 트랜잭션 커밋 후 비동기 처리 — 발송 지연이 commit latency 영향 X.
 *
 * <p>실패 시 — Sprint 1 baseline 는 로그만 (재시도는 TK-03-3-2 DeliveryEscalator + retry_count 컬럼).
 */
@Component
class OrderDiffPersistedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderDiffPersistedListener.class);

    private final NotificationService service;

    OrderDiffPersistedListener(NotificationService service) {
        this.service = service;
    }

    @ApplicationModuleListener
    void on(OrderDiffPersistedEvent event) {
        try {
            service.notify(event);
        } catch (RuntimeException ex) {
            // BR-X02 — 알림 실패는 audit 보존 + 운영 알림. Sprint 1 baseline 는 로그만.
            log.error("Notification dispatch failed changeId={} severity={}: {}",
                event.changeId(), event.severity(), ex.getMessage(), ex);
        }
    }
}
