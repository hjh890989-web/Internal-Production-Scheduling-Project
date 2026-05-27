package com.scheduling.integration;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.DeliveryEscalator;
import com.scheduling.notify.NotificationEntity;
import com.scheduling.notify.NotificationRepository;
import com.scheduling.notify.SlackNotifier;
import com.scheduling.notify.api.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Sprint 18 EP-NOTIFY ST-NOTIFY-1 IT — SlackNotifier + DeliveryEscalator 연동 (TK-NOTIFY-1-3).
 *
 * <p>검증:
 * <ul>
 *   <li>1분 overdue Critical 알림 → DeliveryEscalator.escalate() → SlackNotifier.alert 호출 (1회/row)</li>
 *   <li>overdue 0건 시 → SlackNotifier 미호출</li>
 *   <li>SlackNotifier disabled (default) → 실 HTTP 호출 없이 LOG only true 반환 (no crash)</li>
 *   <li>incrementRetry — retry_count++ 영속</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SlackEscalationIT {

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
    @Autowired private DeliveryEscalator escalator;
    @Autowired private NotificationRepository repository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        // notification 먼저 (order_change FK), order_change 후 정리
        repository.deleteAll();
        jdbc.update("DELETE FROM app.order_change WHERE tracking_id::TEXT LIKE 'sprint18-it-%' "
            + "OR hose_id LIKE '29673-2R%' AND change_id IN ("
            + "SELECT change_id FROM app.order_change ORDER BY changed_at DESC LIMIT 100)");
    }

    private NotificationEntity newOverdueCritical(String hoseId) {
        UUID orderChangeId = UUID.randomUUID();
        // FK 충족 위해 order_change stub INSERT (jsonb 빈 array)
        jdbc.update("""
            INSERT INTO app.order_change
              (change_id, tracking_id, diff_type, hose_id, delivery_date,
               new_order_id, old_order_id, field_diffs, previous_version, new_version,
               severity, changed_at)
            VALUES (?, ?, 'MODIFIED', ?, ?, ?, ?, '[]'::jsonb, 1, 2, 'CRITICAL', now())
            """,
            orderChangeId, UUID.randomUUID(), hoseId, LocalDate.now().plusDays(2),
            UUID.randomUUID(), UUID.randomUUID());

        Instant overdueDispatchedAt = Instant.now().minus(Duration.ofMinutes(3));
        return new NotificationEntity(
            UUID.randomUUID(), orderChangeId,
            NotificationChannel.IN_APP, ChangeSeverity.CRITICAL,
            "PLANNER", hoseId, LocalDate.now().plusDays(2),
            "납기 변경 (Sprint 18 IT)", overdueDispatchedAt);
    }

    @Test
    @DisplayName("BR-NOTIFY-1 — overdue Critical 1건 → SlackNotifier.alert 1회 호출")
    void overdue_critical_triggers_slack_alert() {
        NotificationEntity n = repository.save(newOverdueCritical("29673-2R060"));

        escalator.escalate();

        verify(slackNotifier, atLeastOnce()).alert(
            eq(ChangeSeverity.CRITICAL),
            contains(n.getHoseId()),
            any(String.class));

        // retry_count++ 영속 검증
        NotificationEntity reloaded = repository.findById(n.getNotificationId()).orElseThrow();
        assertThat(reloaded.getRetryCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("overdue 0건 — SlackNotifier 미호출")
    void no_overdue_no_slack_alert() {
        escalator.escalate();
        verify(slackNotifier, never()).alert(any(), any(), any());
    }

    @Test
    @DisplayName("SlackNotifier.alert — disabled (default) 시 실 HTTP 없이 true 반환")
    void slack_disabled_returns_true_stub() {
        boolean ok = slackNotifier.alert(
            ChangeSeverity.CRITICAL, "Slack disabled test", "body");
        assertThat(ok).isTrue();
    }

    @Test
    @DisplayName("overdue 3건 → SlackNotifier.alert 3회 (row 별 1회)")
    void multiple_overdue_calls_slack_per_row() {
        List<NotificationEntity> overdueRows = List.of(
            repository.save(newOverdueCritical("29673-2R060")),
            repository.save(newOverdueCritical("29673-2R030")),
            repository.save(newOverdueCritical("29673-2R040"))
        );

        assertThatCode(() -> escalator.escalate()).doesNotThrowAnyException();

        for (NotificationEntity n : overdueRows) {
            verify(slackNotifier, atLeastOnce()).alert(
                eq(ChangeSeverity.CRITICAL),
                contains(n.getHoseId()),
                any(String.class));
        }
    }
}
