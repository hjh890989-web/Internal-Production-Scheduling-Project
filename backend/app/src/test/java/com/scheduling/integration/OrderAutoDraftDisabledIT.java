package com.scheduling.integration;

import com.scheduling.order.events.OrderCommittedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 26 ST-ORDER-2 Case 2 — {@code scheduling.order.auto-draft.enabled=false} (default) 시
 * 자동 draft 체인 미실행 검증.
 *
 * <p>{@link OrderAutoDraftIT} 와 분리한 이유: {@code @DynamicPropertySource} 가 static scope 이므로
 * 동일 {@code @SpringBootTest} 내에서 per-test 오버라이드 불가. 별도 컨텍스트 (별도 클래스) 로 분리.
 *
 * @see com.scheduling.vc.internal.OrderCommittedListener
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@org.junit.jupiter.api.Disabled("Sprint 26 S26-B carry-over — VcScheduleService.draftBatch 가 현재 log+audit placeholder. GreedyRotationAllocator wire-up 후 본 IT 와 OrderAutoDraftIT 함께 재활성")
class OrderAutoDraftDisabledIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    @DynamicPropertySource
    static void infraProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
        // Case 2: flag 비활성 (default 와 동일) — listener guard 검증
        registry.add("scheduling.order.auto-draft.enabled", () -> "false");
    }

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private JdbcTemplate jdbc;

    private UUID trackingId;

    @BeforeEach
    void setUp() {
        trackingId = UUID.randomUUID();
    }

    // =========================================================================
    // Case 2: auto-draft.enabled=false — 이벤트 publish 후 vc_schedule row 미삽입 검증
    // =========================================================================

    @Test
    @DisplayName("Case 2: auto-draft.enabled=false (default) → OrderCommittedEvent publish 후 vc_schedule row 없음")
    void should_not_insert_draft_vc_schedule_when_auto_draft_disabled() {
        OrderCommittedEvent event = new OrderCommittedEvent(
            trackingId,
            "00000003",
            Instant.parse("2026-06-01T03:00:00Z"),
            "flag 비활성 — auto draft 차단 검증"
        );

        // @ApplicationModuleListener (AFTER_COMMIT + Async) — transaction 내 publish
        new TransactionTemplate(txManager).executeWithoutResult(status ->
            eventPublisher.publishEvent(event)
        );

        // flag=false 이므로 listener 내부 guard 가 draftBatch 호출 차단 — 2초 대기 후 0건 assert
        Awaitility.await()
            .during(2, TimeUnit.SECONDS)
            .atMost(3, TimeUnit.SECONDS)
            .pollInterval(250, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                int count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM app.vc_schedule "
                        + "WHERE linked_order_ids LIKE ?",
                    Integer.class,
                    "%" + trackingId + "%");
                assertThat(count)
                    .as("auto-draft.enabled=false 시 vc_schedule row 삽입 없어야 함")
                    .isEqualTo(0);
            });
    }
}
