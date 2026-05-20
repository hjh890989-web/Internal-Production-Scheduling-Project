package com.scheduling.notify;

import com.scheduling.notify.api.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * STOMP {@code /topic/notifications/{role}} 채널로 인앱 PUSH — TK-03-3-1.
 *
 * <p>{@link SimpMessagingTemplate} 가 {@link WebSocketStompConfig} 의 simple broker 를 통해
 * subscribe 클라이언트에 전파. p95 ≤ 2초 (REQ-NF-PER-004) — broker in-memory.
 *
 * <p>{@code @Profile("with-infra")} — DEV 미연동 context 에서는 publisher bean 미생성,
 * NotificationService 가 fallback 로깅으로 대체.
 */
@Component
@Profile("with-infra")
public class WebSocketNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationPublisher.class);

    private final SimpMessagingTemplate stomp;
    private final NotificationConfig config;

    public WebSocketNotificationPublisher(SimpMessagingTemplate stomp, NotificationConfig config) {
        this.stomp = stomp;
        this.config = config;
    }

    public void publish(Notification notification) {
        String destination = config.getInApp().getTopicPrefix() + "/" + notification.targetRole();
        stomp.convertAndSend(destination, notification);
        log.debug("WebSocket PUSH dest={} notificationId={}", destination, notification.notificationId());
    }
}
