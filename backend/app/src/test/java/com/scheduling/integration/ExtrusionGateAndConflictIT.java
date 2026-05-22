package com.scheduling.integration;

import com.scheduling.ex.conflict.ExAlternative;
import com.scheduling.ex.conflict.ExConflictCategory;
import com.scheduling.ex.conflict.ExConflictReport;
import com.scheduling.ex.conflict.ExConflictReportItem;
import com.scheduling.ex.conflict.ExConflictReportService;
import com.scheduling.ex.gate.ExGateResult;
import com.scheduling.ex.gate.ExGateViolation;
import com.scheduling.ex.gate.ExtrusionValidationGate;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-EX11 + EP-EX12 통합 IT — 실 PG + Shift + ExConstraint + Inventory + Gate + ConflictReport.
 *
 * <p>p95 ≤ 2,000ms (TK-EX11-1-3, 600 candidate batch) + 충돌 리포트 ≥ 3 distinct 대안
 * (TK-EX12-1-2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExtrusionGateAndConflictIT {

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

    @Autowired private ExtrusionValidationGate gate;
    @Autowired private ExConflictReportService reportService;

    private ExScheduleCandidate candidate(String hose, LocalDate deadline) {
        Instant now = Instant.parse("2026-05-22T00:00:00Z");
        return new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), hose, UUID.randomUUID(),
            deadline.plusDays(1), deadline, 100,
            CandidateStatus.PENDING, now, now);
    }

    // ---------- TC-EX-011 게이트 ----------

    @Test
    @DisplayName("Gate validate — 29673-2R060 정상 candidate (yield ≥ Q_ext) → pass")
    void gate_pass_when_yield_sufficient() {
        // 29673-2R060: speed=14.06, length=1000, 주간전반 effective_min=180 → yield 2531/회
        // inventory: target=500, current=100 → Q_ext = vcYield(100) + 500 − 100 = 500
        // 1 candidate yield 2531 ≥ 500 → pass
        ExScheduleCandidate c = candidate("29673-2R060", LocalDate.of(2026, 3, 5));
        ExGateResult r = gate.validate(c, "DAY_EARLY", List.of(c), 0);

        assertThat(r.passed()).isTrue();
        assertThat(r.violations()).isEmpty();
    }

    @Test
    @DisplayName("Gate fail — shift capacity 초과 시 SHIFT_CAPACITY_EXCEEDED")
    void gate_fail_when_shift_capacity_exceeded() {
        ExScheduleCandidate c = candidate("29673-2R060", LocalDate.of(2026, 3, 5));
        // shiftActualMin = 100 → candidate effective_min 180 추가 시 280 > 180
        ExGateResult r = gate.validate(c, "DAY_EARLY", List.of(c), 100);

        assertThat(r.passed()).isFalse();
        assertThat(r.violations()).hasSize(1);
        assertThat(r.violations().get(0).category())
            .isEqualTo(ExGateViolation.Category.SHIFT_CAPACITY_EXCEEDED);
    }

    @Test
    @DisplayName("Gate validate — 마스터 미등록 hose → pass (보수적 fallback)")
    void gate_pass_when_master_missing() {
        ExScheduleCandidate c = candidate("UNKNOWN-HOSE", LocalDate.of(2026, 3, 5));
        ExGateResult r = gate.validate(c, "DAY_EARLY", List.of(c), 0);

        assertThat(r.passed()).isTrue();
    }

    @Test
    @DisplayName("Gate batch — 50 candidate p95 < 2,000ms (REQ-FUNC-EX-011)")
    void gate_batch_performance_p95_under_2s() {
        // 50 candidate (29673-2R060 / 29673-2F900 mix, 다양한 일자)
        String[] hoses = {"29673-2R060", "29673-2F900", "28912-2U000"};
        List<ExScheduleCandidate> candidates = new ArrayList<>();
        Map<UUID, String> shiftMap = new HashMap<>();
        String[] shifts = {"DAY_EARLY", "DAY_LATE", "NIGHT_EARLY", "NIGHT_LATE"};
        LocalDate base = LocalDate.of(2026, 3, 2);

        for (int i = 0; i < 50; i++) {
            ExScheduleCandidate c = candidate(hoses[i % 3], base.plusDays(i / 4));
            candidates.add(c);
            shiftMap.put(c.getExCandidateId(), shifts[i % 4]);
        }

        // warm-up
        for (int i = 0; i < 3; i++) gate.validateBatch(candidates, shiftMap);

        // 측정 10회
        List<Long> latencies = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            gate.validateBatch(candidates, shiftMap);
            latencies.add((System.nanoTime() - start) / 1_000_000);
        }
        Collections.sort(latencies);
        long p95 = latencies.get((int) (10 * 0.95));

        System.out.printf("Gate batch latency: 50 candidate p95 = %dms%n", p95);
        assertThat(p95).as("REQ-FUNC-EX-011 p95 ≤ 2,000ms").isLessThanOrEqualTo(2000L);
    }

    // ---------- TC-EX-012 충돌 리포트 ----------

    @Test
    @DisplayName("ConflictReport — pass 만 → 빈 리포트")
    void conflict_report_empty_when_all_pass() {
        var passResults = List.of(
            new ExGateResult(UUID.randomUUID(), true, List.of(), Instant.now()),
            new ExGateResult(UUID.randomUUID(), true, List.of(), Instant.now()));
        ExConflictReport report = reportService.buildReport(passResults);
        assertThat(report.totalItems()).isZero();
        assertThat(report.items()).isEmpty();
    }

    @Test
    @DisplayName("ConflictReport — 모든 fail item ≥ 3 distinct 대안 (REQ-FUNC-EX-012)")
    void conflict_report_all_items_have_three_distinct_alternatives() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        Map<UUID, String> hoses = Map.of(c1, "29673-2R060", c2, "28912-2U000");

        var failResults = List.of(
            new ExGateResult(c1, false,
                List.of(ExGateViolation.yieldShort(500, 200)), Instant.now()),
            new ExGateResult(c2, false,
                List.of(ExGateViolation.shiftCapacityExceeded(180, 220)), Instant.now()));

        ExConflictReport report = reportService.buildReport(failResults, hoses);

        assertThat(report.totalItems()).isEqualTo(2);
        assertThat(report.summary()).containsKeys(
            ExConflictCategory.CUMULATIVE_YIELD_SHORT,
            ExConflictCategory.SHIFT_CAPACITY_EXCEEDED);

        for (ExConflictReportItem item : report.items()) {
            long distinct = item.alternatives().stream()
                .map(ExAlternative::type).distinct().count();
            assertThat(distinct).as("candidate %s ≥ 3 distinct", item.candidateId())
                .isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    @DisplayName("ConflictReport — generatedAt + p95 ≤ 1,000ms (TK-EX12-1-2)")
    void conflict_report_p95_under_one_second() {
        UUID c1 = UUID.randomUUID();
        var results = List.of(new ExGateResult(c1, false,
            List.of(ExGateViolation.yieldShort(500, 200)), Instant.now()));

        // warm-up
        for (int i = 0; i < 5; i++) reportService.buildReport(results);

        List<Long> latencies = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            long start = System.nanoTime();
            reportService.buildReport(results);
            latencies.add((System.nanoTime() - start) / 1_000_000);
        }
        Collections.sort(latencies);
        long p95 = latencies.get((int) (20 * 0.95));

        System.out.printf("ConflictReport p95 = %dms (20 runs)%n", p95);
        assertThat(p95).as("p95 ≤ 1,000ms").isLessThanOrEqualTo(1000L);
    }
}
