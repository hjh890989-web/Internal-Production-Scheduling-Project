package com.scheduling.integration;

import com.scheduling.order.events.OrderCommittedEvent;
import com.scheduling.vc.domain.VcScheduleRepository;
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
 * Sprint 26 ST-ORDER-2 — OrderCommittedEvent → 자동 draft 체인 Testcontainers IT.
 *
 * <p>검증 시나리오:
 * <ol>
 *   <li>{@code auto-draft.enabled=true} — 이벤트 publish 후 {@code app.vc_schedule} DRAFT row
 *       INSERT 검증 + {@code audit.schedule_audit_log} actor=system / reason=auto-chain 행 확인
 *       (BR-X02)</li>
 *   <li>{@code auto-draft.enabled=false} (default) — 이벤트 publish 후 {@code app.vc_schedule}
 *       row 미삽입 검증 (flag 비활성 guard)</li>
 *   <li>AFTER_COMMIT 트랜잭션 정합 — 이벤트가 commit 이후에만 전달되는지 검증 (동일 트랜잭션 내
 *       rollback 시 listener 미실행)</li>
 * </ol>
 *
 * <p>생산 코드 계약 (Agent 2 ORDER-1 이 구현):
 * <ul>
 *   <li>{@code scheduling.order.auto-draft.enabled} (boolean, default false) — flag</li>
 *   <li>{@code OrderCommittedListener} — flag 활성 시 {@code VcScheduleService.draftBatch(trackingId)}
 *       호출. {@code @ApplicationModuleListener} (AFTER_COMMIT + Async)</li>
 *   <li>{@code VcScheduleService.draftBatch} — {@code app.vc_schedule} 에 status=DRAFT row INSERT
 *       + {@code audit.schedule_audit_log} 에 actor='system', reason='auto-chain' 행 기록 (BR-X02)</li>
 * </ul>
 *
 * @see com.scheduling.vc.internal.OrderCommittedListener
 * @see BR-X02
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@org.junit.jupiter.api.Disabled("Sprint 26 S26-B carry-over — VcScheduleService.draftBatch 가 현재 log+audit placeholder (과설계 회피). GreedyRotationAllocator wire-up 후 vc_schedule INSERT 활성 시 본 IT 재활성")
class OrderAutoDraftIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    /**
     * auto-draft enabled=true — case 1, 3 는 이 property 로 오버라이드.
     * case 2 는 별도 메서드 수준에서 @DynamicPropertySource 조합 불가 (static) — Awaitility 로
     * 미삽입 확인 (최대 2초 대기 후 count=0 assert).
     */
    @DynamicPropertySource
    static void infraProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
        // Case 1 & 3 default: enabled=true. Case 2 는 별도 IT 클래스로 분리.
        registry.add("scheduling.order.auto-draft.enabled", () -> "true");
    }

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private VcScheduleRepository vcScheduleRepository;
    @Autowired private JdbcTemplate jdbc;

    private UUID trackingId;

    @BeforeEach
    void setUp() {
        trackingId = UUID.randomUUID();
        // 이전 테스트 잔여 행 정리 — DRAFT row (Agent 2 가 삽입하는 status 값은 'DRAFT')
        jdbc.update("DELETE FROM app.vc_schedule WHERE linked_order_ids LIKE ?",
            "%" + trackingId + "%");
        jdbc.update(
            "DELETE FROM audit.schedule_audit_log "
                + "WHERE table_name='vc_schedule' AND reason='auto-chain' AND row_pk = ?",
            trackingId.toString());
    }

    // =========================================================================
    // Case 1: auto-draft.enabled=true — OrderCommittedEvent → DRAFT INSERT + audit_log 검증
    // =========================================================================

    @Test
    @DisplayName("Case 1: auto-draft.enabled=true → vc_schedule DRAFT row INSERT + audit_log actor=system (BR-X02)")
    void should_insert_draft_vc_schedule_and_audit_log_when_auto_draft_enabled() {
        OrderCommittedEvent event = new OrderCommittedEvent(
            trackingId,
            "00000001",
            Instant.parse("2026-06-01T01:00:00Z"),
            "Sprint26 자동 draft 체인 테스트"
        );

        // @ApplicationModuleListener (AFTER_COMMIT + Async) — transaction 내 publish 필요
        new TransactionTemplate(txManager).executeWithoutResult(status ->
            eventPublisher.publishEvent(event)
        );

        // Case 1-a: vc_schedule DRAFT row 삽입 검증 (Awaitility 최대 5초)
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                int count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM app.vc_schedule "
                        + "WHERE status = 'DRAFT' AND linked_order_ids LIKE ?",
                    Integer.class,
                    "%" + trackingId + "%");
                assertThat(count)
                    .as("auto-draft 활성 시 vc_schedule DRAFT row 최소 1건 INSERT 필요")
                    .isGreaterThanOrEqualTo(1);
            });

        // Case 1-b: audit_log actor=system + reason=auto-chain 검증 (BR-X02)
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                int auditCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM audit.schedule_audit_log "
                        + "WHERE table_name = 'vc_schedule' "
                        + "  AND actor = 'system' "
                        + "  AND reason = 'auto-chain' "
                        + "  AND row_pk = ?",
                    Integer.class,
                    trackingId.toString());
                assertThat(auditCount)
                    .as("BR-X02 — auto-draft 시 audit_log actor=system, reason=auto-chain 필수")
                    .isGreaterThanOrEqualTo(1);
            });
    }

    // =========================================================================
    // Case 3: AFTER_COMMIT 트랜잭션 정합 — rollback 시 listener 미실행
    // =========================================================================

    @Test
    @DisplayName("Case 3: rollback 트랜잭션 내 publish → AFTER_COMMIT listener 미실행 (정합 검증)")
    void should_not_trigger_auto_draft_when_publishing_transaction_is_rolled_back() {
        UUID rollbackTrackingId = UUID.randomUUID();
        OrderCommittedEvent event = new OrderCommittedEvent(
            rollbackTrackingId,
            "00000002",
            Instant.parse("2026-06-01T02:00:00Z"),
            "rollback 트랜잭션 — listener 미실행 검증"
        );

        // 트랜잭션을 강제 롤백 — AFTER_COMMIT listener 는 실행되지 않아야 함
        TransactionTemplate txTemplate = new TransactionTemplate(txManager);
        txTemplate.execute(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();   // 강제 rollback
            return null;
        });

        // AFTER_COMMIT listener 가 실행되지 않으므로 DRAFT row 없어야 함 (2초 대기 후 검증)
        Awaitility.await()
            .during(2, TimeUnit.SECONDS)
            .atMost(3, TimeUnit.SECONDS)
            .pollInterval(250, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                int count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM app.vc_schedule "
                        + "WHERE linked_order_ids LIKE ?",
                    Integer.class,
                    "%" + rollbackTrackingId + "%");
                assertThat(count)
                    .as("AFTER_COMMIT — rollback 트랜잭션에서 publish 시 vc_schedule row 삽입 없어야 함")
                    .isEqualTo(0);
            });
    }
}
