package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Sprint 18 EP-NOTIFY ST-NOTIFY-1 — Slack webhook 클라이언트 (TK-NOTIFY-1-1).
 *
 * <p>{@code scheduling.notification.slack.enabled=true} 일 때만 실 HTTP POST.
 * 기본 false (실 webhook URL 미발급 환경 보호) — LOG only fallback.
 *
 * <p>Resilience4j {@code @Retry(name=slack)} + {@code @CircuitBreaker(name=slack)} 정책 적용 —
 * 3회 retry · 5초 timeout · 5회 연속 실패 시 30초 OPEN (application.yml 참조).
 *
 * <p>채널 라우팅:
 * <ul>
 *   <li>{@link ChangeSeverity#CRITICAL} → {@link NotificationConfig.Slack#getCriticalChannel()}</li>
 *   <li>그 외 (IMPORTANT/STANDARD) → {@link NotificationConfig.Slack#getAlertsChannel()}</li>
 * </ul>
 */
@Component
public class SlackNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackNotifier.class);

    private final NotificationConfig config;
    private final RestClient restClient;

    public SlackNotifier(NotificationConfig config) {
        this.config = config;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Slack alert push.
     *
     * @param severity 알림 심각도 — 채널 라우팅 결정
     * @param title    Slack 메시지 헤더 (예: "BR-V07 D-0 락 위반")
     * @param body     본문 — Markdown 지원 (Slack mrkdwn)
     * @return true = 송신 성공 (또는 disabled stub), false = 실패
     */
    @CircuitBreaker(name = "slack", fallbackMethod = "fallbackAlert")
    @Retry(name = "slack")
    public boolean alert(ChangeSeverity severity, String title, String body) {
        NotificationConfig.Slack slack = config.getSlack();
        if (!slack.isEnabled() || slack.getWebhookUrl() == null || slack.getWebhookUrl().isBlank()) {
            log.debug("[Slack-STUB] disabled or webhook empty — severity={} title={}", severity, title);
            return true;     // disabled 는 성공 취급 (LOG only)
        }
        String channel = severity == ChangeSeverity.CRITICAL
            ? slack.getCriticalChannel()
            : slack.getAlertsChannel();

        Map<String, Object> payload = Map.of(
            "channel", channel,
            "text", String.format("*[%s]* %s", severity.name(), title),
            "blocks", java.util.List.of(
                Map.of("type", "header",
                       "text", Map.of("type", "plain_text", "text", title)),
                Map.of("type", "section",
                       "text", Map.of("type", "mrkdwn", "text", body))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String response = restClient.post()
            .uri(slack.getWebhookUrl())
            .headers(h -> h.addAll(headers))
            .body(payload)
            .retrieve()
            .body(String.class);

        log.info("[Slack] sent — channel={} severity={} title={} response={}",
            channel, severity, title, response);
        return true;
    }

    @SuppressWarnings("unused")
    private boolean fallbackAlert(ChangeSeverity severity, String title, String body, Throwable t) {
        log.warn("[Slack] fallback (Resilience4j) — severity={} title={} cause={}",
            severity, title, t.getClass().getSimpleName());
        return false;
    }

    /** Slack 알림 timeout (실 HTTP). Sprint 18 baseline 5초. */
    public Duration timeout() {
        return Duration.ofSeconds(config.getSlack().getTimeoutSeconds());
    }
}
