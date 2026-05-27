package com.scheduling.integration;

import com.scheduling.notify.VcSchedulePushListener;
import com.scheduling.vc.events.VcChangedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Sprint 14 EP-VC-FULL ST-VC-4 — VcSchedulePushListener STOMP broadcast IT (TK-VC-4-3).
 *
 * <p>VcChangedEvent publish → ApplicationModuleListener (AFTER_COMMIT async) →
 * SimpMessagingTemplate.convertAndSend({@code /topic/vc-schedule-updates}) 호출 검증.
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VcSchedulePushListenerIT {

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
    }

    @MockitoBean private SimpMessagingTemplate stomp;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager txManager;

    @Test
    @DisplayName("VcChangedEvent → STOMP convertAndSend /topic/vc-schedule-updates (AFTER_COMMIT async)")
    void vc_changed_triggers_stomp_push() {
        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(),
            Instant.now(),
            List.of(new VcChangedEvent.VcChangedRow(
                UUID.randomUUID(), "29673-2R060",
                null, java.time.LocalDate.of(2026, 6, 1),
                null, 100, VcChangedEvent.ChangeType.QUANTITY))
        );

        // @ApplicationModuleListener (AFTER_COMMIT) — transaction 안에서 publish 필요
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
        });

        // ApplicationModuleListener 가 AFTER_COMMIT + Async — Mockito timeout 으로 wait
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(stomp, timeout(5_000))
            .convertAndSend(eq(VcSchedulePushListener.VC_SCHEDULE_UPDATES_TOPIC), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).isInstanceOf(VcChangedEvent.class);
        VcChangedEvent captured = (VcChangedEvent) payloadCaptor.getValue();
        assertThat(captured.scheduleId()).isEqualTo(event.scheduleId());
        assertThat(captured.changedRows()).hasSize(1);
    }

    // mockito eq static import 헬퍼
    private static String eq(String value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
