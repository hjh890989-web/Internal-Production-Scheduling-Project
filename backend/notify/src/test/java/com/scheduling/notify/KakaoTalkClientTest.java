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
    @DisplayName("kakao.enabled=true → stub 송신 OK, send() = true")
    void enabled_returns_true_stub() {
        config.getKakao().setEnabled(true);
        assertThat(client.send(SAMPLE)).isTrue();
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
