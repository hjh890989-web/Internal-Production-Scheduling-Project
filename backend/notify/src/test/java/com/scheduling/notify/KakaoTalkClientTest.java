package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.api.Notification;
import com.scheduling.notify.api.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KakaoTalkClient 회귀 — TK-03-3-1 (Sprint 1 baseline stub).
 *
 * <p>Sprint 1 baseline 의 stub 동작 (config.enabled=false → skip, true → log 송신) +
 * 한국어 BizMessage 템플릿 형식 검증.
 */
class KakaoTalkClientTest {

    private NotificationConfig config;
    private KakaoTalkClient client;

    private static final Notification SAMPLE = new Notification(
        UUID.randomUUID(),
        UUID.randomUUID(),
        NotificationChannel.KAKAOTALK,
        ChangeSeverity.CRITICAL,
        "PLANNER",
        "29673-2F900",
        LocalDate.of(2026, 2, 15),
        "qty: 100 → 130 (+30%)",
        Instant.now()
    );

    @BeforeEach
    void setUp() {
        config = new NotificationConfig();
        client = new KakaoTalkClient(config);
    }

    @Test
    @DisplayName("kakao.enabled=false → skip, send() = false")
    void disabled_returns_false() {
        config.getKakao().setEnabled(false);
        assertThat(client.send(SAMPLE)).isFalse();
    }

    @Test
    @DisplayName("kakao.enabled=true + webhookUrl 비어있음 → skip, send() = false (Sprint 18 실 HTTP 진입 가드)")
    void enabled_but_no_webhook_returns_false() {
        // Sprint 18 KakaoTalkClient stub log → 실 RestClient POST 로 변경.
        // enabled=true 만 으로는 부족 — webhookUrl 비어있으면 false 반환 (실 운영 URL 미발급 환경 보호).
        config.getKakao().setEnabled(true);
        config.getKakao().setWebhookUrl("");
        assertThat(client.send(SAMPLE)).isFalse();
    }

    @Test
    @DisplayName("BizMessage 한국어 템플릿 — 품번·납기·변경·확인 URL 포함 (REQ-NF-USA-003)")
    void biz_message_template_contains_korean_fields() {
        String msg = client.buildBizMessage(SAMPLE);

        assertThat(msg)
            .contains("Critical 수주 변경 알림")
            .contains("품번: 29673-2F900")
            .contains("납기일: 2026-02-15")
            .contains("변경: qty: 100 → 130")
            .contains("확인: " + config.getAppUrl() + "/notifications/" + SAMPLE.notificationId());
    }
}
