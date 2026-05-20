package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.api.Notification;
import com.scheduling.notify.api.NotificationChannel;
import com.scheduling.order.events.OrderDiffPersistedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotificationService 라우팅 회귀 — TK-03-3-1 (REQ-FUNC-OC-009).
 *
 * <p>핵심 검증:
 * <ul>
 *   <li>Critical → 인앱 + 카카오톡 양쪽 발송</li>
 *   <li>Normal → 인앱만 (카카오톡 skip)</li>
 *   <li>WebSocket publisher 부재 (DEV context) → fallback 로깅, 예외 X</li>
 *   <li>KakaoTalk 응답 false 도 정상 처리 (인앱 영향 X — 장애 격리)</li>
 * </ul>
 */
class NotificationServiceTest {

    private static final Instant FIXED = Instant.parse("2026-05-20T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED, ZoneId.of("Asia/Seoul"));
    private static final LocalDate D = LocalDate.of(2026, 2, 15);

    private WebSocketNotificationPublisher publisher;
    private KakaoTalkClient kakaoClient;
    private NotificationConfig config;
    private ObjectProvider<WebSocketNotificationPublisher> publisherProvider;
    private ObjectProvider<NotificationRepository> repositoryProvider;
    private NotificationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        publisher = mock(WebSocketNotificationPublisher.class);
        kakaoClient = mock(KakaoTalkClient.class);
        config = new NotificationConfig();
        publisherProvider = (ObjectProvider<WebSocketNotificationPublisher>) mock(ObjectProvider.class);
        repositoryProvider = (ObjectProvider<NotificationRepository>) mock(ObjectProvider.class);
        when(publisherProvider.getIfAvailable()).thenReturn(publisher);
        when(repositoryProvider.getIfAvailable()).thenReturn(null);   // DEV — Repository 부재 default

        service = new NotificationService(publisherProvider, kakaoClient, config, repositoryProvider, CLOCK);
    }

    private OrderDiffPersistedEvent event(ChangeSeverity sev) {
        return new OrderDiffPersistedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "29673-2F900",
            D,
            "MODIFIED",
            sev,
            "qty: 100 → 130",
            FIXED
        );
    }

    // ---------- Critical 라우팅 ----------

    @Test
    @DisplayName("Critical → 인앱 + 카카오톡 양쪽 발송")
    void critical_dispatches_both_channels() {
        when(kakaoClient.send(any())).thenReturn(true);

        var summary = service.notify(event(ChangeSeverity.CRITICAL));

        assertThat(summary.inAppSent()).isTrue();
        assertThat(summary.kakaoSent()).isTrue();
        verify(publisher, times(1)).publish(any(Notification.class));
        verify(kakaoClient, times(1)).send(any(Notification.class));
    }

    @Test
    @DisplayName("Critical — 인앱 Notification channel=IN_APP, 카카오 channel=KAKAOTALK")
    void critical_routes_with_correct_channel_per_notification() {
        when(kakaoClient.send(any())).thenReturn(true);

        service.notify(event(ChangeSeverity.CRITICAL));

        ArgumentCaptor<Notification> wsCap = ArgumentCaptor.forClass(Notification.class);
        verify(publisher).publish(wsCap.capture());
        assertThat(wsCap.getValue().channel()).isEqualTo(NotificationChannel.IN_APP);

        ArgumentCaptor<Notification> kkCap = ArgumentCaptor.forClass(Notification.class);
        verify(kakaoClient).send(kkCap.capture());
        assertThat(kkCap.getValue().channel()).isEqualTo(NotificationChannel.KAKAOTALK);
        assertThat(kkCap.getValue().severity()).isEqualTo(ChangeSeverity.CRITICAL);
    }

    // ---------- Normal 라우팅 ----------

    @Test
    @DisplayName("Normal → 인앱만 발송, 카카오톡 skip")
    void normal_dispatches_in_app_only() {
        var summary = service.notify(event(ChangeSeverity.NORMAL));

        assertThat(summary.inAppSent()).isTrue();
        assertThat(summary.kakaoSent()).isFalse();
        verify(publisher, times(1)).publish(any(Notification.class));
        verify(kakaoClient, never()).send(any());
    }

    // ---------- Fallback / 장애 격리 ----------

    @Test
    @DisplayName("WebSocket publisher 부재 (DEV) → 예외 X, kakao 만 활성, inAppSent=false")
    void in_app_publisher_absent_falls_back_silently() {
        when(publisherProvider.getIfAvailable()).thenReturn(null);
        when(kakaoClient.send(any())).thenReturn(true);

        var summary = service.notify(event(ChangeSeverity.CRITICAL));

        // 인앱 fallback (publisher 부재) → inAppSent=false 기록, 카톡은 정상
        assertThat(summary.inAppSent()).isFalse();
        assertThat(summary.kakaoSent()).isTrue();
        verify(publisher, never()).publish(any());
        verify(kakaoClient, times(1)).send(any());
    }

    @Test
    @DisplayName("KakaoTalk 실패 (false) → 인앱은 정상, summary.kakaoSent=false")
    void kakao_failure_does_not_affect_in_app() {
        when(kakaoClient.send(any())).thenReturn(false);

        var summary = service.notify(event(ChangeSeverity.CRITICAL));

        assertThat(summary.inAppSent()).isTrue();
        assertThat(summary.kakaoSent()).isFalse();
        verify(publisher, times(1)).publish(any());
    }

    // ---------- DispatchSummary 값 ----------

    @Test
    @DisplayName("DispatchSummary.changeId = event.changeId 동일")
    void summary_change_id_matches_event() {
        var ev = event(ChangeSeverity.NORMAL);
        var summary = service.notify(ev);
        assertThat(summary.changeId()).isEqualTo(ev.changeId());
    }

    @Test
    @DisplayName("targetRole = config.defaultTargetRole")
    void target_role_from_config() {
        config.setDefaultTargetRole("STK_USER");

        service.notify(event(ChangeSeverity.NORMAL));

        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        verify(publisher).publish(cap.capture());
        assertThat(cap.getValue().targetRole()).isEqualTo("STK_USER");
    }
}
