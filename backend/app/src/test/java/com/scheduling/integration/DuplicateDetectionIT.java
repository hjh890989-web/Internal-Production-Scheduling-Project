package com.scheduling.integration;

import com.scheduling.order.domain.DuplicateDetectionService;
import com.scheduling.order.domain.DuplicateGroup;
import com.scheduling.order.domain.DuplicateOrderException;
import com.scheduling.order.domain.OrderCommitService;
import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.domain.OrderRepository;
import com.scheduling.order.domain.OrderType;
import io.micrometer.core.instrument.MeterRegistry;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-02 DuplicateDetection 통합 — TK-02-1-2 (TC-OC-005 100사이클 + K-O03 메트릭).
 *
 * <p>핵심 검증:
 * <ul>
 *   <li>DS-DUPLICATE-50: 50쌍 중복 × 100 사이클 → 마스터 row 50건 정확 (REQ-FUNC-OC-005)</li>
 *   <li>거부 횟수 ≥ 5000 (50 × 100) — UNIQUE 위반 카운터 emit</li>
 *   <li>동시성 — 10 thread 동일 키 commit → 1 win + 9 reject</li>
 *   <li>{@code scheduling.events{module=order_commit,operation=unique_violation}} 노출</li>
 * </ul>
 *
 * <p>합성 데이터셋 — {@link #generateDuplicatePairs} 가 50쌍 (hose_id, delivery_date) seed 발생.
 * 동일 키 + 다른 qty/customer 로 중복 표현. fixed seed 로 재현 가능.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DuplicateDetectionIT {

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

    @Autowired private OrderCommitService commitService;
    @Autowired private OrderRepository repository;
    @Autowired private DuplicateDetectionService detectionService;
    @Autowired private MeterRegistry meterRegistry;

    private static final int MASTER_VERSION = 1;
    private static final int DUPLICATE_PAIRS = 50;
    private static final int CYCLES = 100;
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 2, 15);

    @BeforeEach
    void cleanState() {
        repository.deleteAll();
    }

    private double counter(String module, String operation) {
        var c = meterRegistry.find("scheduling.events")
            .tag("module", module)
            .tag("operation", operation)
            .counter();
        return c == null ? 0.0 : c.count();
    }

    private OrderDraft draft(String hoseId, LocalDate date, int qty, String customer) {
        return new OrderDraft(null, hoseId, date, qty, OrderType.WEEKLY, customer);
    }

    /**
     * 50쌍 중복 케이스 — 각 쌍은 같은 (hose_id, delivery_date) 의 첫 시도 + 중복 시도.
     */
    private List<DuplicatePair> generateDuplicatePairs() {
        List<DuplicatePair> pairs = new ArrayList<>(DUPLICATE_PAIRS);
        for (int i = 0; i < DUPLICATE_PAIRS; i++) {
            String hoseId = String.format("29673-DUP%03d", i);
            LocalDate date = BASE_DATE.plusDays(i % 7);
            pairs.add(new DuplicatePair(
                draft(hoseId, date, 100, "현대모비스"),
                draft(hoseId, date, 130, "기아")           // 같은 키, 다른 qty/customer
            ));
        }
        return pairs;
    }

    record DuplicatePair(OrderDraft orig, OrderDraft dup) {}

    // ---------- 100 cycle 회귀 (TC-OC-005) ----------

    @Test
    @DisplayName("DS-DUPLICATE-50 × 100 사이클 → master row = 50, 거부 ≥ 5000")
    void no_duplicates_after_100_cycles() {
        List<DuplicatePair> pairs = generateDuplicatePairs();
        long totalCommitted = 0;
        long totalRejected = 0;
        Random random = new Random(42);   // fixed seed — 재현성

        // Spring context 가 재사용되므로 counter 가 다른 test 의 누적값 포함 — snapshot delta 검증.
        double initialViolations = counter("order_commit", "unique_violation");
        double initialSuccess = counter("order_commit", "success");

        for (int cycle = 0; cycle < CYCLES; cycle++) {
            List<DuplicatePair> shuffled = new ArrayList<>(pairs);
            Collections.shuffle(shuffled, random);

            for (DuplicatePair pair : shuffled) {
                // 첫 row — cycle 1 에서는 성공, 이후 cycle 은 중복 거부
                try {
                    commitService.commit(pair.orig(), MASTER_VERSION);
                    totalCommitted++;
                } catch (DuplicateOrderException e) {
                    totalRejected++;
                }
                // 두 번째 row — 항상 중복 거부 (같은 key)
                try {
                    commitService.commit(pair.dup(), MASTER_VERSION);
                    totalCommitted++;
                } catch (DuplicateOrderException e) {
                    totalRejected++;
                }
            }
        }

        long uniqueRowCount = repository.count();
        assertThat(uniqueRowCount)
            .as("100 cycle 후 master row 는 정확히 %d", DUPLICATE_PAIRS)
            .isEqualTo(DUPLICATE_PAIRS);

        // 첫 cycle 에서 50 win, 50 reject. 이후 99 cycle × (50 + 50) = 9900 reject. 합계 9950.
        // 최소 5000 보장 — 보수적 임계
        assertThat(totalRejected)
            .as("거부 카운트 — 최소 50쌍 × 100 cycle = 5000 이상")
            .isGreaterThanOrEqualTo(5000);
        assertThat(totalCommitted)
            .as("성공 commit = unique 50 (첫 cycle 만)")
            .isEqualTo(DUPLICATE_PAIRS);

        // K-O03 Prometheus 메트릭 delta — Spring context 공유로 누적값에서 차감.
        double violationDelta = counter("order_commit", "unique_violation") - initialViolations;
        double successDelta = counter("order_commit", "success") - initialSuccess;
        assertThat(violationDelta)
            .as("UNIQUE 위반 카운터 delta — 거부 횟수와 일치")
            .isEqualTo((double) totalRejected);
        assertThat(successDelta)
            .as("commit.success delta — totalCommitted 와 일치")
            .isEqualTo((double) totalCommitted);
    }

    // ---------- 동시성 — 10 thread 동일 키 ----------

    @Test
    @DisplayName("10 thread 동일 키 commit → 1 win, 9 reject (PG row-level lock)")
    void concurrent_inserts_only_one_wins() throws Exception {
        OrderDraft template = draft("29673-CONC01", LocalDate.of(2026, 3, 1), 100, "현대모비스");

        ExecutorService pool = Executors.newFixedThreadPool(10);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger rejects = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(pool.submit(() -> {
                try {
                    commitService.commit(
                        new OrderDraft(UUID.randomUUID(), template.hoseId(), template.deliveryDate(),
                            template.qty(), template.orderType(), template.customer()),
                        MASTER_VERSION);
                    wins.incrementAndGet();
                } catch (DuplicateOrderException e) {
                    rejects.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected", e);
                }
            }));
        }

        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(wins.get())
            .as("동시성 — 동일 키 commit 10 thread 중 정확히 1 win")
            .isEqualTo(1);
        assertThat(rejects.get())
            .as("나머지 9 thread 는 DuplicateOrderException")
            .isEqualTo(9);
        assertThat(repository.count())
            .as("DB row 1건만 (UNIQUE 제약 정합)")
            .isEqualTo(1);
    }

    // ---------- ORM 사전 검증 (DuplicateDetectionService) ----------

    @Test
    @DisplayName("DuplicateDetectionService — 기존 마스터 + batch 내 중복 동시 검출")
    void detection_service_finds_existing_and_within_batch() {
        // master 시드 — 1 row
        commitService.commit(draft("29673-PRE01", BASE_DATE, 100, "현대모비스"), MASTER_VERSION);

        // batch — 2 row 중복 (vs master 1건 + within batch 1쌍)
        List<OrderDraft> batch = List.of(
            draft("29673-PRE01", BASE_DATE, 200, "기아"),      // vs master 중복
            draft("29673-PRE02", BASE_DATE, 100, "현대모비스"), // unique
            draft("29673-PRE02", BASE_DATE, 150, "기아")        // batch 내 중복 (PRE02 와)
        );

        List<DuplicateGroup> dups = detectionService.detect(batch);

        // 중복 그룹 2개 (PRE01 vs master, PRE02 within batch)
        assertThat(dups).hasSize(2);
        assertThat(dups).extracting(g -> g.key().hoseId())
            .containsExactlyInAnyOrder("29673-PRE01", "29673-PRE02");

        // master 와 충돌 1건, batch 내 충돌 1건
        long vsMaster = dups.stream().filter(DuplicateGroup::hasExisting).count();
        long withinBatch = dups.stream().filter(g -> g.candidateCount() > 1).count();
        assertThat(vsMaster).isEqualTo(1);
        assertThat(withinBatch).isEqualTo(1);

        // Prometheus 메트릭 — scheduling.events{module=order_duplicate, operation=detected_batch}
        assertThat(counter("order_duplicate", "detected_batch")).isGreaterThanOrEqualTo(1.0);
    }
}
