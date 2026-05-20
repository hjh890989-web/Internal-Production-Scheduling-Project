package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.api.Notification;
import com.scheduling.notify.api.NotificationChannel;
import com.scheduling.order.events.OrderDiffPersistedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 알림 라우팅 진입점 — TK-03-3-1 (REQ-FUNC-OC-009).
 *
 * <p>채널 라우팅 규칙:
 * <ul>
 *   <li>모든 변경 → 인앱 WebSocket PUSH (Critical + Normal)</li>
 *   <li>Critical 만 → 카카오톡 BizMessage 백업</li>
 * </ul>
 *
 * <p>{@link WebSocketNotificationPublisher} 는 {@code @Profile("with-infra")} — DEV 컨텍스트에서는
 * bean 부재 → {@link ObjectProvider} 가 빈 Optional 반환, 인앱 PUSH 는 로그 fallback.
 *
 * <p>{@link KakaoTalkClient} 는 무조건 bean 생성 — config 의 {@code kakao.enabled} 플래그로 stub/active 결정.
 *
 * <p>{@link com.scheduling.order.events.OrderDiffPersistedEvent} 1건당 본 메서드 1회 호출
 * — TK-03-3-2 후속에서 NOTIFICATION row 영속 + 상태 추적 통합.
 */
@Service
@EnableConfigurationProperties(NotificationConfig.class)
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectProvider<WebSocketNotificationPublisher> wsPublisherProvider;
    private final KakaoTalkClient kakaoClient;
    private final NotificationConfig config;
    private final Clock clock;

    public NotificationService(
        ObjectProvider<WebSocketNotificationPublisher> wsPublisherProvider,
        KakaoTalkClient kakaoClient,
        NotificationConfig config,
        Clock clock
    ) {
        this.wsPublisherProvider = wsPublisherProvider;
        this.kakaoClient = kakaoClient;
        this.config = config;
        this.clock = clock;
    }

    /**
     * OrderDiffPersistedEvent → 채널별 알림 발송.
     */
    public DispatchSummary notify(OrderDiffPersistedEvent event) {
        Instant at = Instant.now(clock);
        boolean criticalSent = false;

        // 1. 인앱 알림 — 모든 변경
        Notification inApp = build(event, NotificationChannel.IN_APP, at);
        dispatchInApp(inApp);

        // 2. Critical 만 카카오톡 백업
        if (event.severity() == ChangeSeverity.CRITICAL) {
            Notification kakao = build(event, NotificationChannel.KAKAOTALK, at);
            criticalSent = kakaoClient.send(kakao);
        }

        log.info("Notify dispatched changeId={} severity={} inApp=true kakao={}",
            event.changeId(), event.severity(), criticalSent);
        return new DispatchSummary(event.changeId(), true, criticalSent);
    }

    private void dispatchInApp(Notification notification) {
        WebSocketNotificationPublisher publisher = wsPublisherProvider.getIfAvailable();
        if (publisher == null) {
            log.warn("[InApp-FALLBACK] WebSocket publisher 미연동 (DEV context) — notificationId={} target={}",
                notification.notificationId(), notification.targetRole());
            return;
        }
        publisher.publish(notification);
    }

    private Notification build(OrderDiffPersistedEvent event, NotificationChannel channel, Instant at) {
        return new Notification(
            UUID.randomUUID(),
            event.changeId(),
            channel,
            event.severity(),
            config.getDefaultTargetRole(),
            event.hoseId(),
            event.deliveryDate(),
            event.changeSummary(),
            at
        );
    }

    /** dispatch 결과 — TK-03-3-2 에서 NOTIFICATION 영속 시 사용. */
    public record DispatchSummary(UUID changeId, boolean inAppSent, boolean kakaoSent) {}
}
