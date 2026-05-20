package com.scheduling.notify;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 알림 발송 설정 — TK-03-3-1.
 *
 * <p>{@code application.yml} 의 {@code scheduling.notification} prefix.
 *
 * <pre>
 * scheduling:
 *   notification:
 *     app-url: https://schedule.intranet
 *     default-target-role: PLANNER
 *     kakao:
 *       enabled: false                              # Sprint 1 baseline 기본 비활성
 *       webhook-url: ${KAKAO_WEBHOOK_URL:}
 *       bot-token: ${KAKAO_BOT_TOKEN:}
 *       timeout-seconds: 5
 *     in-app:
 *       topic-prefix: /topic/notifications
 * </pre>
 */
@ConfigurationProperties(prefix = "scheduling.notification")
public class NotificationConfig {

    private String appUrl = "https://schedule.intranet";

    /** Critical 발송 시 target role 기본값 — Sprint 1 baseline. Phase 2 EP-30 에서 사용자별 routing. */
    private String defaultTargetRole = "PLANNER";

    private Kakao kakao = new Kakao();
    private InApp inApp = new InApp();

    public String getAppUrl() { return appUrl; }
    public void setAppUrl(String appUrl) { this.appUrl = appUrl; }

    public String getDefaultTargetRole() { return defaultTargetRole; }
    public void setDefaultTargetRole(String defaultTargetRole) { this.defaultTargetRole = defaultTargetRole; }

    public Kakao getKakao() { return kakao; }
    public void setKakao(Kakao kakao) { this.kakao = kakao; }

    public InApp getInApp() { return inApp; }
    public void setInApp(InApp inApp) { this.inApp = inApp; }

    public static class Kakao {
        private boolean enabled = false;
        private String webhookUrl = "";
        private String botToken = "";
        private int timeoutSeconds = 5;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public String getBotToken() { return botToken; }
        public void setBotToken(String botToken) { this.botToken = botToken; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class InApp {
        /** STOMP destination prefix — `/topic/notifications/{role}` 로 publish. */
        private String topicPrefix = "/topic/notifications";

        public String getTopicPrefix() { return topicPrefix; }
        public void setTopicPrefix(String topicPrefix) { this.topicPrefix = topicPrefix; }
    }
}
