package com.scheduling.notify;

import com.scheduling.notify.api.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 카카오톡 Workplace Bot Webhook 클라이언트 — TK-03-3-1 (SAD §3.1 EXT-SYS-05).
 *
 * <p><b>Sprint 1 baseline</b> — log-only stub. 실제 HTTP 호출 + Resilience4j Circuit Breaker
 * + Retry 는 Sprint 2 (KakaoTalk Workplace Bot 계약·BotToken 확보 후) 에서 활성.
 *
 * <p>현재 동작: {@link NotificationConfig.Kakao#isEnabled()} 가 {@code true} 면 송신 시도 로그 출력
 * (BizMessage 한국어 템플릿 빌드 + 도달 결과 false 반환 — fallback 의미). {@code false} 면 skip.
 *
 * <p>Sprint 2 활성 시 추가될 사항:
 * <ul>
 *   <li>{@code @CircuitBreaker(name = "kakaotalk", fallbackMethod = "fallbackSend")}</li>
 *   <li>{@code @Retry(name = "kakaotalk")} — max 3, exponential backoff 1s × 2</li>
 *   <li>WebClient / RestTemplate 으로 {@code config.getKakao().getWebhookUrl()} POST</li>
 *   <li>{@code Authorization: Bearer ${config.getKakao().getBotToken()}}</li>
 * </ul>
 */
@Component
public class KakaoTalkClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoTalkClient.class);

    private final NotificationConfig config;

    public KakaoTalkClient(NotificationConfig config) {
        this.config = config;
    }

    /**
     * Critical 알림 → 카카오톡 송신 시도.
     *
     * @return {@code true} = 송신 성공 (Sprint 2 실제 활성 시), {@code false} = stub / 비활성 / 실패
     */
    public boolean send(Notification notification) {
        if (!config.getKakao().isEnabled()) {
            log.debug("KakaoTalk disabled (config) — skip notificationId={}", notification.notificationId());
            return false;
        }
        String message = buildBizMessage(notification);
        log.info("[KakaoTalk-STUB] notificationId={} target={} message={}",
            notification.notificationId(), notification.targetRole(), message);
        // Sprint 2 — 실제 HTTP POST + Resilience4j. 현재는 stub 송신 OK.
        return true;
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
