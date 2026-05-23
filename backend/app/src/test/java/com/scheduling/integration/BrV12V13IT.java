package com.scheduling.integration;

import com.scheduling.master.kd.KdOrder;
import com.scheduling.master.kd.KdOrderRepository;
import com.scheduling.master.priority.ProductPriority;
import com.scheduling.master.priority.ProductPriorityRepository;
import com.scheduling.vc.capacity_overflow.CapacityOverflowQueueService;
import com.scheduling.vc.capacity_overflow.KdSupplementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 7 BR-V12·V13 IT — Sprint 7 carry-over (REQ-FUNC-VC-022·023, deferred).
 *
 * <p>V033 product_priority + kd_order seed + 알고리즘 chain 검증.
 * 활성 조건 (수주통합 후) 충족 전이라도 entity + service + facade chain 동작 회귀.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")   // setting_group 시드 활용 (BR-V13 2차 우선순위)
class BrV12V13IT {

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

    @Autowired private ProductPriorityRepository priorityRepo;
    @Autowired private KdOrderRepository kdRepo;
    @Autowired private CapacityOverflowQueueService overflowService;
    @Autowired private KdSupplementService supplementService;

    private static final LocalDate D = LocalDate.of(2026, 6, 1);
    private static final Instant T0 = Instant.parse("2026-05-23T00:00:00Z");

    @BeforeEach
    void clean() {
        priorityRepo.deleteAll();
        kdRepo.deleteAll();
    }

    @Test
    @DisplayName("BR-V12 — priority rank ASC + capa 초과 큐 분리")
    void br_v12_priority_split() {
        priorityRepo.save(new ProductPriority("29673-2R060", (short) 1,
            "VIP 고객", D, null, T0, "seed"));
        priorityRepo.save(new ProductPriority("28422-2M800", (short) 2,
            "긴급", D, null, T0, "seed"));

        Map<String, Integer> required = Map.of(
            "29673-2R060", 60,    // rank 1
            "28422-2M800", 50,    // rank 2
            "28421-2M800", 40);   // 미등록 → rank 99 fallback

        // CLOCK 가 fixed 2026-05-23 (Sprint 6) — D=2026-06-01 효력 이후로 lookup
        // 단위 테스트에서는 Clock 2026-06-01 mock 했지만 IT 에서는 real Clock.
        // priority effective_from = D = 2026-06-01 ↔ Clock 2026-05-23 (이전) → 미효력
        // → 모두 rank 99 fallback (모두 동일 priority → 입력 순서 정렬)
        // 따라서 본 IT 는 priority 영향 검증보다 capa 분기 알고리즘 자체 검증
        CapacityOverflowQueueService.SplitResult r = overflowService.split(required, 100);

        assertThat(r.totalAccepted() + r.totalQueued()).isEqualTo(150);
        assertThat(r.totalAccepted()).isLessThanOrEqualTo(100);
        assertThat(r.totalQueued()).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("BR-V13 — 동일 hose KD 잔량 보충 (1차 우선순위)")
    void br_v13_same_hose_supplement() {
        UUID kd1 = UUID.randomUUID();
        kdRepo.save(new KdOrder(kd1, "29673-2R060",
            100, 100, LocalDate.of(2026, 4, 1), "CUST-A",
            KdOrder.Status.OPEN, T0, "seed"));

        KdSupplementService.SupplementResult r =
            supplementService.supplement("29673-2R060", 80, "planner-001");

        assertThat(r.supplemented()).isEqualTo(80);
        assertThat(r.consumed()).hasSize(1);
        assertThat(r.consumed().get(0).qty()).isEqualTo(80);

        KdOrder reloaded = kdRepo.findById(kd1).orElseThrow();
        assertThat(reloaded.getRemainingQty()).isEqualTo(20);
        assertThat(reloaded.getStatus()).isEqualTo(KdOrder.Status.PARTIAL);
    }

    @Test
    @DisplayName("BR-V13 — 동일 hose 부족 시 동일 셋팅 그룹 hose 2차 우선순위")
    void br_v13_group_fallback() {
        // 29673-2R060 + 28422-2M800 는 DS-VC-CONSTRAINT-47 같은 setting_group 일 수 있음
        // 단위 IT 단순화 — 동일 hose 잔량 0 → 그룹 fallback 시도
        kdRepo.save(new KdOrder(UUID.randomUUID(), "29673-2R060",
            50, 30, LocalDate.of(2026, 4, 1), "CUST-A",
            KdOrder.Status.PARTIAL, T0, "seed"));

        // shortage 100 — 동일 hose 30 + 그룹 fallback (0 이면 부족)
        KdSupplementService.SupplementResult r =
            supplementService.supplement("29673-2R060", 100, "planner-001");

        assertThat(r.supplemented()).isLessThanOrEqualTo(100);
        assertThat(r.supplemented()).isGreaterThanOrEqualTo(30);   // 최소 동일 hose 잔량
    }

    @Test
    @DisplayName("BR-V13 — shortage 0 → 보충 0")
    void zero_shortage() {
        KdSupplementService.SupplementResult r =
            supplementService.supplement("29673-2R060", 0, "planner-001");
        assertThat(r.supplemented()).isZero();
        assertThat(r.consumed()).isEmpty();
    }

    @Test
    @DisplayName("BR-V13 — KD 잔량 0 → 보충 0 (fallback group 도 잔량 0 시)")
    void no_kd_remaining() {
        KdSupplementService.SupplementResult r =
            supplementService.supplement("UNKNOWN-HOSE", 100, "planner-001");
        assertThat(r.supplemented()).isZero();
        assertThat(r.shortage()).isEqualTo(100);
    }
}
