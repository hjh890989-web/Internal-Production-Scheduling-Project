package com.scheduling.integration;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.KakaoDeliveryAttempt;
import com.scheduling.notify.KakaoDeliveryRepository;
import com.scheduling.notify.KakaoDeliveryService;
import com.scheduling.notify.api.Notification;
import com.scheduling.notify.api.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-16 ST-16-1 IT — Kakao 도달 로그 + retry 영속 (REQ-FUNC-CO-008).
 *
 * <p>kakao stub (NotificationConfig.kakao.enabled=false) → SKIPPED 기록만 검증.
 * 운영 시 enabled=true + Resilience4j Retry 활성 — Sprint 6+.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class KakaoDeliveryIT {

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

    @Autowired private KakaoDeliveryService service;
    @Autowired private KakaoDeliveryRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private Notification newNotification() {
        return new Notification(
            UUID.randomUUID(), UUID.randomUUID(),
            NotificationChannel.KAKAOTALK, ChangeSeverity.CRITICAL,
            "PLANNER", "29673-2R060", LocalDate.of(2026, 6, 1),
            "납기 변경: 6/1 → 6/3", Instant.now());
    }

    @Test
    @DisplayName("kakao disabled — sendWithRetry → FAILED + 3 attempts 영속 (max retry)")
    void disabled_returns_false_three_attempts() {
        Notification n = newNotification();
        boolean ok = service.sendWithRetry(n);
        assertThat(ok).isFalse();   // disabled 시 client 가 false → 3회 retry 모두 실패

        List<KakaoDeliveryAttempt> attempts =
            repository.findByNotificationIdOrderByAttemptedAtDesc(n.notificationId());
        assertThat(attempts).hasSize(3);
        attempts.forEach(a ->
            assertThat(a.getStatus()).isEqualTo(KakaoDeliveryAttempt.Status.FAILED));
        assertThat(attempts).extracting(KakaoDeliveryAttempt::getAttemptNo)
            .containsExactlyInAnyOrder((short) 1, (short) 2, (short) 3);
    }

    @Test
    @DisplayName("recordSkipped — SKIPPED 1 row 영속 + countByStatus 정합")
    void skipped_recorded() {
        Notification n = newNotification();
        service.recordSkipped(n, "config disabled");

        assertThat(repository.countByStatus(KakaoDeliveryAttempt.Status.SKIPPED)).isEqualTo(1);
        List<KakaoDeliveryAttempt> attempts =
            repository.findByNotificationIdOrderByAttemptedAtDesc(n.notificationId());
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getErrorMessage()).isEqualTo("config disabled");
    }

    @Test
    @DisplayName("countByStatus — FAILED / SKIPPED 분리 집계 (NS-04 KPI)")
    void count_by_status() {
        service.sendWithRetry(newNotification());      // 3 FAILED
        service.sendWithRetry(newNotification());      // 3 FAILED
        service.recordSkipped(newNotification(), "test");  // 1 SKIPPED

        assertThat(repository.countByStatus(KakaoDeliveryAttempt.Status.FAILED)).isEqualTo(6);
        assertThat(repository.countByStatus(KakaoDeliveryAttempt.Status.SKIPPED)).isEqualTo(1);
    }
}
