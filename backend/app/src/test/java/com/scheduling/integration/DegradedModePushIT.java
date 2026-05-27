package com.scheduling.integration;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.SlackNotifier;
import com.scheduling.vc.events.MesDegradedModeChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Sprint 18 EP-NOTIFY ST-NOTIFY-4 IT — DegradedModePushListener (TK-NOTIFY-4-4).
 *
 * <p>{@link MesDegradedModeChangedEvent} 발행 → AFTER_COMMIT async → DegradedModePushListener →
 * SlackNotifier.alert + SimpMessagingTemplate.convertAndSend 양쪽 호출 검증.
 *
 * <p>{@link ApplicationModuleListener AFTER_COMMIT} — TransactionTemplate 으로 publish 필수
 * (Sprint 14/15 패턴 재사용).
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DegradedModePushIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
        registry.add("scheduling.notification.slack.enabled", () -> "false");
    }

    @MockitoSpyBean private SlackNotifier slackNotifier;
    @MockitoSpyBean private SimpMessagingTemplate stomp;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager txManager;

    @Test
    @DisplayName("BR-X06 — degraded 진입 (NORMAL → DEGRADED) → Slack CRITICAL + STOMP 양쪽 push")
    void degraded_entering_triggers_slack_and_stomp() {
        MesDegradedModeChangedEvent event = new MesDegradedModeChangedEvent(
            "LP-01", false, true, Instant.now());

        new TransactionTemplate(txManager).executeWithoutResult(status ->
            eventPublisher.publishEvent(event));

        verify(slackNotifier, timeout(5_000)).alert(
            eq(ChangeSeverity.CRITICAL),
            contains("MES degraded 진입"),
            contains("LP-01"));
        verify(stomp, timeout(5_000)).convertAndSend(
            eq("/topic/mes-degraded-updates"),
            any(Object.class));
    }

    @Test
    @DisplayName("BR-X06 — degraded 해제 (DEGRADED → NORMAL) → Slack 복구 알림 + STOMP")
    void degraded_recovered_triggers_slack_and_stomp() {
        MesDegradedModeChangedEvent event = new MesDegradedModeChangedEvent(
            "LP-02", true, false, Instant.now());

        new TransactionTemplate(txManager).executeWithoutResult(status ->
            eventPublisher.publishEvent(event));

        verify(slackNotifier, timeout(5_000)).alert(
            eq(ChangeSeverity.CRITICAL),
            contains("MES 정상 복구"),
            contains("LP-02"));
        verify(stomp, timeout(5_000)).convertAndSend(
            eq("/topic/mes-degraded-updates"),
            any(Object.class));
    }

    @Test
    @DisplayName("동일 상태 (DEGRADED → DEGRADED) — STOMP 만 push, Slack 미호출 (노이즈 차단)")
    void same_state_no_slack_only_stomp() {
        MesDegradedModeChangedEvent event = new MesDegradedModeChangedEvent(
            "LP-03", true, true, Instant.now());

        new TransactionTemplate(txManager).executeWithoutResult(status ->
            eventPublisher.publishEvent(event));

        verify(stomp, timeout(5_000)).convertAndSend(
            eq("/topic/mes-degraded-updates"),
            any(Object.class));
        // Slack은 진입/해제 만 — 동일 상태는 호출 안 됨
        verify(slackNotifier, never()).alert(any(), contains("LP-03"), any());
    }
}
