package com.scheduling.notify;

import com.scheduling.notify.api.Notification;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 카카오톡 Workplace Bot Webhook 클라이언트 — TK-03-3-1 (SAD §3.1 EXT-SYS-05).
 *
 * <p><b>Sprint 18 ST-NOTIFY-2</b> — 실 HTTP POST 활성 (TK-NOTIFY-2-1).
 *   {@code scheduling.notification.kakao.enabled=true} + {@code webhookUrl} 비어있지 않을 때만
 *   실 webhook 호출. 그 외 LOG only stub (실 BizToken 미발급 환경 보호).
 *
 * <p>Resilience4j {@code @Retry(name=kakaotalk)} + {@code @CircuitBreaker(name=kakaotalk)} —
 * 3회 retry · 5초 timeout · 5회 연속 실패 시 30초 OPEN.
 */
@Component
public class KakaoTalkClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoTalkClient.class);

    private final NotificationConfig config;
    private final RestClient restClient;

    public KakaoTalkClient(NotificationConfig config) {
        this.config = config;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Critical 알림 → 카카오톡 송신 시도.
     *
     * @return {@code true} = 송신 성공 (또는 disabled stub), {@code false} = 실패
     */
    @CircuitBreaker(name = "kakaotalk", fallbackMethod = "fallbackSend")
    @Retry(name = "kakaotalk")
    public boolean send(Notification notification) {
        NotificationConfig.Kakao kakao = config.getKakao();
        if (!kakao.isEnabled() || kakao.getWebhookUrl() == null || kakao.getWebhookUrl().isBlank()) {
            log.debug("KakaoTalk disabled (config) or webhook empty — skip notificationId={}",
                notification.notificationId());
            return false;
        }
        String message = buildBizMessage(notification);

        Map<String, Object> payload = Map.of(
            "target", notification.targetRole() == null ? "PLANNER" : notification.targetRole(),
            "text", message
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (kakao.getBotToken() != null && !kakao.getBotToken().isBlank()) {
            headers.setBearerAuth(kakao.getBotToken());
        }

        String response = restClient.post()
            .uri(kakao.getWebhookUrl())
            .headers(h -> h.addAll(headers))
            .body(payload)
            .retrieve()
            .body(String.class);

        log.info("[KakaoTalk] sent — notificationId={} target={} response={}",
            notification.notificationId(), notification.targetRole(), response);
        return true;
    }

    /**
     * Resilience4j fallback — Circuit OPEN 또는 max retry 도달 시 호출.
     * 본 메서드 signature 는 원본 send(Notification) + 마지막 Throwable 인자 (R4j 규약).
     */
    @SuppressWarnings("unused")
    private boolean fallbackSend(Notification notification, Throwable t) {
        log.warn("[KakaoTalk] fallback (Resilience4j) — notificationId={}, cause={}",
            notification.notificationId(), t.getClass().getSimpleName());
        return false;
    }

    /**
     * 한국어 BizMessage 템플릿 — REQ-NF-USA-003.
     */
    String buildBizMessage(Notification n) {
        return """
            [Critical 수주 변경 알림]
            품번: %s
            납기일: %s
            변경: %s
            확인: %s/notifications/%s
            """.formatted(
                n.hoseId(),
                n.deliveryDate(),
                n.changeSummary(),
                config.getAppUrl(),
                n.notificationId()
            );
    }
}
