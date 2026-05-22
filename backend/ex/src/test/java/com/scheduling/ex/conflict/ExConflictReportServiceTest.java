package com.scheduling.ex.conflict;

import com.scheduling.ex.gate.ExGateResult;
import com.scheduling.ex.gate.ExGateViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExConflictReportService 단위 — TK-EX12-1-2 (REQ-FUNC-EX-012).
 */
class ExConflictReportServiceTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-05-22T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private ExConflictReportService service;

    @BeforeEach
    void setUp() {
        service = new ExConflictReportService(
            new ExConflictCategorizer(), new ExAlternativeGenerator(), CLOCK);
    }

    private ExGateResult fail(UUID candidateId, ExGateViolation v) {
        return new ExGateResult(candidateId, false, List.of(v), Instant.now(CLOCK));
    }

    private ExGateResult passResult(UUID candidateId) {
        return new ExGateResult(candidateId, true, List.of(), Instant.now(CLOCK));
    }

    @Test
    @DisplayName("passed=true 만 → 빈 리포트")
    void all_passed_empty_report() {
        var results = List.of(passResult(UUID.randomUUID()), passResult(UUID.randomUUID()));
        ExConflictReport report = service.buildReport(results);

        assertThat(report.totalItems()).isZero();
        assertThat(report.items()).isEmpty();
        assertThat(report.summary()).isEmpty();
        assertThat(report.generatedAt()).isNotNull();
    }

    @Test
    @DisplayName("CUMULATIVE_YIELD_SHORT + SHIFT_CAPACITY_EXCEEDED mix → 2 items + summary")
    void mix_violations_classified() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        Map<UUID, String> hoses = Map.of(c1, "29673-2R060", c2, "28912-2U000");

        var results = List.of(
            fail(c1, ExGateViolation.yieldShort(500, 200)),
            fail(c2, ExGateViolation.shiftCapacityExceeded(180, 200)));

        ExConflictReport report = service.buildReport(results, hoses);

        assertThat(report.totalItems()).isEqualTo(2);
        assertThat(report.summary()).containsKeys(
            ExConflictCategory.CUMULATIVE_YIELD_SHORT,
            ExConflictCategory.SHIFT_CAPACITY_EXCEEDED);
        assertThat(report.summary().get(ExConflictCategory.CUMULATIVE_YIELD_SHORT)).isEqualTo(1L);
    }

    @Test
    @DisplayName("모든 item — ≥ 3 distinct 대안 (REQ-FUNC-EX-012)")
    void all_items_have_at_least_three_distinct_alternatives() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        var results = List.of(
            fail(c1, ExGateViolation.yieldShort(500, 200)),
            fail(c2, ExGateViolation.shiftCapacityExceeded(180, 220)));

        ExConflictReport report = service.buildReport(results);

        for (ExConflictReportItem item : report.items()) {
            long distinct = item.alternatives().stream()
                .map(ExAlternative::type).distinct().count();
            assertThat(distinct).as("candidate %s ≥ 3 distinct", item.candidateId())
                .isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    @DisplayName("hoseByCandidate 미제공 시 → UNKNOWN hose 표시 (보수적)")
    void missing_hose_mapping_unknown() {
        UUID c1 = UUID.randomUUID();
        var results = List.of(fail(c1, ExGateViolation.yieldShort(500, 200)));

        ExConflictReport report = service.buildReport(results);
        assertThat(report.items()).hasSize(1);
        assertThat(report.items().get(0).hoseId()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("Idempotent — 같은 입력 2회 → 같은 summary + items")
    void idempotent_same_input() {
        UUID c1 = UUID.randomUUID();
        var results = List.of(fail(c1, ExGateViolation.yieldShort(500, 200)));

        ExConflictReport r1 = service.buildReport(results);
        ExConflictReport r2 = service.buildReport(results);

        assertThat(r1.summary()).isEqualTo(r2.summary());
        assertThat(r1.items()).hasSameSizeAs(r2.items());
    }
}
