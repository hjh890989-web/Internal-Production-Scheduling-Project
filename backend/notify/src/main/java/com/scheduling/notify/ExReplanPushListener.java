package com.scheduling.notify;

import com.scheduling.ex.events.ExReplanCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * EX partial replan 완료 → STOMP {@code /topic/extrusion-updates} push —
 * TK-EX14-1-2 (EP-EX14, REQ-FUNC-EX-014).
 *
 * <p>BR-X03 chain — vc.changed → PartialReplanService.replanWithContext →
 * ExReplanCompletedEvent → 본 listener → SimpMessagingTemplate → SockJS client.
 *
 * <p>p95 ≤ 2초 목표 — in-memory broker + AFTER_COMMIT 비동기 (REQ-NF-PER-004).
 */
@Component
@Profile("with-infra")
public class ExReplanPushListener {

    private static final Logger log = LoggerFactory.getLogger(ExReplanPushListener.class);

    /** BR-EX14 압출 패드 구독 토픽. */
    public static final String EXTRUSION_UPDATES_TOPIC = "/topic/extrusion-updates";

    private final SimpMessagingTemplate stomp;

    public ExReplanPushListener(SimpMessagingTemplate stomp) {
        this.stomp = stomp;
    }

    @ApplicationModuleListener
    public void onReplanCompleted(ExReplanCompletedEvent event) {
        stomp.convertAndSend(EXTRUSION_UPDATES_TOPIC, event);
        log.info("STOMP PUSH {} — batch={}, triggered={}",
            EXTRUSION_UPDATES_TOPIC, event.vcScheduleId(), event.triggeredCount());
    }
}
