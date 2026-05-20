package com.scheduling.integration;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.NotificationEntity;
import com.scheduling.notify.NotificationRepository;
import com.scheduling.notify.api.NotificationChannel;
import com.scheduling.notify.api.NotificationStatus;
import com.scheduling.order.diff.DiffResult;
import com.scheduling.order.diff.DiffTaggingService;
import com.scheduling.order.diff.DiffType;
import com.scheduling.order.diff.OrderChangeEntity;
import com.scheduling.order.diff.OrderChangeRepository;
import com.scheduling.order.diff.Severity;
import com.scheduling.order.domain.Order;
import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.domain.OrderRepository;
import com.scheduling.order.domain.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * EP-03 E2E (Testcontainers PostgreSQL) — Diff → Severity → Notify 통합 회귀.
 *
 * <p>흐름:
 * <ol>
 *   <li>master_version=1 의 ACTIVE Order 3건 시드 (DB)</li>
 *   <li>{@link DiffTaggingService#computeAndTag} 호출 — 신규 winner = 1 NEW + 1 MODIFIED-Critical + 1 MODIFIED-Normal + 1 UNCHANGED</li>
 *   <li>검증:
 *     <ul>
 *       <li>{@code app.order_change} — UNCHANGED 제외 3건 영속 + severity 컬럼 채움</li>
 *       <li>{@code OrderDiffPersistedEvent} → {@code @ApplicationModuleListener} 비동기 발송</li>
 *       <li>{@code app.notification} — Critical 2건 × (IN_APP + KAKAOTALK) + Normal 1건 × IN_APP = 총 5 row</li>
 *       <li>NotificationStatus = SENT (인앱 publisher 정상) / FAILED (kakao stub disabled — config.enabled=false)</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>STG / PROD Keycloak 없이도 통합 가능 — JWT issuer-uri 미설정 시 SecurityConfig 폴백 httpBasic.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DiffNotifyEndToEndIT {

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
        // Redis 미사용 — Lettuce lazy connection 가 부팅 시 fail 안 함 (호출 시점만 fail).
        // 본 IT 는 ImportTrackingService 호출 없음.
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");      // unused stub port
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
    }

    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderChangeRepository orderChangeRepo;
    @Autowired private NotificationRepository notificationRepo;
    @Autowired private DiffTaggingService diffTagging;

    private static final LocalDate D = LocalDate.of(2026, 2, 15);
    private static final Instant T0 = Instant.parse("2026-05-20T05:00:00Z");

    @BeforeEach
    void cleanState() {
        notificationRepo.deleteAll();
        orderChangeRepo.deleteAll();
        orderRepo.deleteAll();
    }

    private Order activeOrder(String hoseId, LocalDate date, int qty, OrderType type, String customer, int version) {
        return new Order(
            UUID.randomUUID(), hoseId, date, qty, type, customer, version, "ACTIVE", T0
        );
    }

    private OrderDraft draft(String hoseId, LocalDate date, int qty, OrderType type, String customer) {
        return new OrderDraft(null, hoseId, date, qty, type, customer);
    }

    @Test
    @DisplayName("master v1 → v2 — 4 시나리오 (UNCHANGED·NEW·MOD-Critical·MOD-Normal) diff·severity·notify E2E")
    void diff_severity_notify_end_to_end() {
        // ----- 시드: master_version=1 ACTIVE 3건 -----
        Order kept    = orderRepo.save(activeOrder("29673-A001", D, 100, OrderType.WEEKLY,  "현대모비스", 1));
        Order modCrit = orderRepo.save(activeOrder("29673-B002", D, 100, OrderType.WEEKLY,  "현대모비스", 1));
        Order modNorm = orderRepo.save(activeOrder("29673-C003", D, 100, OrderType.WEEKLY,  "내수",       1));

        // ----- 신규 winner: kept 동일, modCrit qty +30% (Critical), modNorm customer 변경 (Normal), neworder NEW (Critical 기본 정책) -----
        List<OrderDraft> newWinners = List.of(
            draft("29673-A001", D, 100, OrderType.WEEKLY, "현대모비스"),          // UNCHANGED
            draft("29673-B002", D, 130, OrderType.WEEKLY, "현대모비스"),          // MODIFIED Critical (qty +30%)
            draft("29673-C003", D, 100, OrderType.WEEKLY, "기아"),                 // MODIFIED Normal (customer)
            draft("29673-D004", D, 50,  OrderType.CONFIRMED, "현대모비스")         // NEW (Critical)
        );

        UUID trackingId = UUID.randomUUID();
        DiffResult result = diffTagging.computeAndTag(trackingId, newWinners, 1);

        // ----- order_change 검증 -----
        assertThat(result.countByType(DiffType.UNCHANGED)).isEqualTo(1);
        assertThat(result.countByType(DiffType.NEW)).isEqualTo(1);
        assertThat(result.countByType(DiffType.MODIFIED)).isEqualTo(2);
        assertThat(result.countByType(DiffType.DELETED)).isZero();

        List<OrderChangeEntity> changes = orderChangeRepo.findByTrackingIdOrderByChangedAtAsc(trackingId);
        // UNCHANGED 는 persist skip → 3 row
        assertThat(changes).hasSize(3);
        assertThat(changes).extracting(OrderChangeEntity::getDiffType)
            .containsExactlyInAnyOrder(DiffType.NEW, DiffType.MODIFIED, DiffType.MODIFIED);
        assertThat(changes).allSatisfy(c -> assertThat(c.getSeverity()).isIn(Severity.CRITICAL.name(), Severity.NORMAL.name()));

        // Severity 분포 — 2 Critical (B002 qty 변경 + D004 NEW) + 1 Normal (C003 customer)
        long critical = changes.stream().filter(c -> Severity.CRITICAL.name().equals(c.getSeverity())).count();
        long normal   = changes.stream().filter(c -> Severity.NORMAL.name().equals(c.getSeverity())).count();
        assertThat(critical).isEqualTo(2);
        assertThat(normal).isEqualTo(1);

        // ----- notification 비동기 적재 검증 (Awaitility) -----
        // Critical 2 × (IN_APP + KAKAOTALK) = 4 + Normal 1 × IN_APP = 1 → 총 5 row
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<NotificationEntity> all = notificationRepo.findAll();
            assertThat(all).hasSize(5);

            long inApp = all.stream().filter(n -> n.getChannel() == NotificationChannel.IN_APP).count();
            long kakao = all.stream().filter(n -> n.getChannel() == NotificationChannel.KAKAOTALK).count();
            assertThat(inApp).isEqualTo(3);     // Critical 2 + Normal 1
            assertThat(kakao).isEqualTo(2);     // Critical 2
        });

        // status 검증 — 인앱 SENT (publisher 정상), 카톡 FAILED (config.enabled=false → send() false → markFailed)
        List<NotificationEntity> all = notificationRepo.findAll();
        all.stream().filter(n -> n.getChannel() == NotificationChannel.IN_APP)
            .forEach(n -> assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT));
        all.stream().filter(n -> n.getChannel() == NotificationChannel.KAKAOTALK)
            .forEach(n -> {
                assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
                assertThat(n.getErrorMessage()).contains("KakaoTalk send failed");
            });
    }

    @Test
    @DisplayName("DELETED 시나리오 — v1 의 row 가 v2 winners 에서 빠짐 → DELETED Critical + 알림 발송")
    void deleted_diff_produces_critical_notification() {
        Order toDelete = orderRepo.save(activeOrder("29673-DEL01", D, 100, OrderType.WEEKLY, "현대모비스", 1));

        // 신규 winner 비어있음 — toDelete 가 DELETED
        UUID trackingId = UUID.randomUUID();
        DiffResult result = diffTagging.computeAndTag(trackingId, List.of(), 1);

        assertThat(result.countByType(DiffType.DELETED)).isEqualTo(1);

        List<OrderChangeEntity> changes = orderChangeRepo.findByTrackingIdOrderByChangedAtAsc(trackingId);
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getDiffType()).isEqualTo(DiffType.DELETED);
        assertThat(changes.get(0).getSeverity()).isEqualTo(Severity.CRITICAL.name());

        // 알림 — Critical 이므로 IN_APP + KAKAOTALK 둘 다
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(notificationRepo.findByOrderChangeIdOrderByDispatchedAtAsc(changes.get(0).getChangeId()))
                .hasSize(2));
    }

    @Test
    @DisplayName("severity 인덱스 적중 — Critical 만 조회 (idx_order_change_severity)")
    void critical_only_query_via_severity_index() {
        Order a = orderRepo.save(activeOrder("29673-X100", D, 100, OrderType.WEEKLY, "내수", 1));

        diffTagging.computeAndTag(UUID.randomUUID(), List.of(
            draft("29673-X100", D, 200, OrderType.WEEKLY, "내수"),     // Critical (qty +100%)
            draft("29673-X101", D, 50,  OrderType.WEEKLY, "내수")      // Critical (NEW)
        ), 1);

        List<OrderChangeEntity> all = orderChangeRepo.findAll();
        assertThat(all).hasSize(2);
        assertThat(all).allSatisfy(c -> assertThat(c.getSeverity()).isEqualTo(Severity.CRITICAL.name()));
    }
}
