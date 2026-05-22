package com.scheduling.integration;

import com.scheduling.vc.allocator.AllocationConflict;
import com.scheduling.vc.conflict.Alternative;
import com.scheduling.vc.conflict.AlternativeGenerator;
import com.scheduling.vc.conflict.ConflictCategory;
import com.scheduling.vc.conflict.ConflictReport;
import com.scheduling.vc.conflict.ConflictReportItem;
import com.scheduling.vc.conflict.ConflictReportService;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-VC15 ST-VC15-1 TK-VC15-1-3 — TC-VC-015 충돌 리포트 회귀.
 *
 * <p>실 PG + ConflictReportService (Categorizer + AlternativeGenerator) 통합 검증.
 * 모든 카테고리 conflict 에 ≥ 3 distinct 대안 + summary 카운트.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConflictReportIT {

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

    @Autowired private ConflictReportService service;

    private List<AllocationConflict> mixConflicts() {
        return List.of(
            AllocationConflict.unschedulable("A", 10),
            AllocationConflict.insufficientCapacity("B", 20, 5, 5),
            AllocationConflict.deadlineExceeded("C", 15, 0, LocalDate.of(2026, 3, 1)),
            AllocationConflict.leftRightViolation("D", 12, 6),
            AllocationConflict.machinePinViolation("E", 8, 3, "LP-01 핀 위반"),
            AllocationConflict.hoseCapViolation("F", 5, 2, 2)
        );
    }

    // ---------- TC-VC-015 ----------

    @Test
    @DisplayName("모든 카테고리 mix — items 6건 + summary 6 카테고리")
    void mix_conflicts_classified() {
        ConflictReport report = service.buildReport(mixConflicts());

        assertThat(report.totalItems()).isEqualTo(6);
        assertThat(report.items()).hasSize(6);
        assertThat(report.summary()).containsKeys(
            ConflictCategory.UNSCHEDULABLE,
            ConflictCategory.DAILY_CAPA,
            ConflictCategory.DEADLINE_D2,
            ConflictCategory.LEFT_RIGHT,
            ConflictCategory.MACHINE_PIN,
            ConflictCategory.HOSE_CAP);
    }

    @Test
    @DisplayName("모든 conflict — ≥ 3 distinct 대안 포함 (REQ-FUNC-VC-015)")
    void all_items_have_at_least_three_distinct_alternatives() {
        ConflictReport report = service.buildReport(mixConflicts());

        for (ConflictReportItem item : report.items()) {
            long distinct = item.alternatives().stream()
                .map(Alternative::type).distinct().count();
            assertThat(distinct)
                .as("hose %s (%s) ≥ 3 distinct", item.hoseId(), item.category())
                .isGreaterThanOrEqualTo(AlternativeGenerator.MIN_ALTERNATIVES);
        }
    }

    @Test
    @DisplayName("빈 conflict list — empty report")
    void empty_conflicts_empty_report() {
        ConflictReport report = service.buildReport(List.of());
        assertThat(report.totalItems()).isZero();
        assertThat(report.items()).isEmpty();
        assertThat(report.summary()).isEmpty();
        assertThat(report.generatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Idempotent — 같은 입력 2회 → 같은 카운트 + items (generatedAt 만 갱신)")
    void idempotent_same_summary() {
        var conflicts = mixConflicts();
        ConflictReport r1 = service.buildReport(conflicts);
        ConflictReport r2 = service.buildReport(conflicts);

        assertThat(r1.summary()).isEqualTo(r2.summary());
        assertThat(r1.items()).hasSameSizeAs(r2.items());
    }

    @Test
    @DisplayName("p95 ≤ 1000ms — buildReport 50회 (REQ-FUNC-VC-015)")
    void p95_under_one_second() {
        var conflicts = mixConflicts();
        // warm-up
        for (int i = 0; i < 5; i++) service.buildReport(conflicts);

        List<Long> latencies = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            long start = System.nanoTime();
            service.buildReport(conflicts);
            latencies.add((System.nanoTime() - start) / 1_000_000);
        }
        Collections.sort(latencies);
        long p95 = latencies.get((int) (50 * 0.95));

        System.out.printf("buildReport p95 = %dms (50 runs, 6 conflicts)%n", p95);
        assertThat(p95).as("p95 ≤ 1000ms").isLessThanOrEqualTo(1000L);
    }
}
