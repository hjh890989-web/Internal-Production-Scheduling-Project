package com.scheduling.order.diff;

import com.scheduling.order.domain.OrderKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SeverityClassifier 회귀 — TK-03-2-1 + TK-03-2-2 (BR-O02 / TC-OC-008).
 *
 * <p>15+ 케이스 — False Negative 0 (Critical 누락 0) + False Positive 최소.
 */
class SeverityClassifierTest {

    private static final LocalDate D = LocalDate.of(2026, 2, 15);
    private static final OrderKey KEY = new OrderKey("29673-2F900", D);

    private SeverityConfig config;
    private SeverityClassifier classifier;

    @BeforeEach
    void setUp() {
        config = new SeverityConfig();
        classifier = new SeverityClassifier(config);
    }

    private RowDiff modified(List<FieldDiff> fieldDiffs) {
        return new RowDiff(KEY, DiffType.MODIFIED, null, null, fieldDiffs);
    }

    // ---------- 표준 4 분류 ----------

    @Test
    @DisplayName("UNCHANGED → NORMAL")
    void unchanged_is_normal() {
        RowDiff diff = new RowDiff(KEY, DiffType.UNCHANGED, null, null, List.of());
        assertThat(classifier.classify(diff)).isEqualTo(Severity.NORMAL);
    }

    @Test
    @DisplayName("NEW → CRITICAL (기본 설정)")
    void new_is_critical_by_default() {
        RowDiff diff = new RowDiff(KEY, DiffType.NEW, null, null, List.of());
        assertThat(classifier.classify(diff)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("DELETED → CRITICAL (기본 설정)")
    void deleted_is_critical_by_default() {
        RowDiff diff = new RowDiff(KEY, DiffType.DELETED, null, null, List.of());
        assertThat(classifier.classify(diff)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("null RowDiff → NORMAL (defensive)")
    void null_diff_is_normal() {
        assertThat(classifier.classify(null)).isEqualTo(Severity.NORMAL);
    }

    // ---------- MODIFIED 필드별 분류 ----------

    @Test
    @DisplayName("MODIFIED delivery_date 변경 → CRITICAL")
    void delivery_date_change_critical() {
        RowDiff diff = modified(List.of(
            new FieldDiff("delivery_date", D, D.plusDays(5))));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("MODIFIED hose_id 변경 → CRITICAL")
    void hose_id_change_critical() {
        RowDiff diff = modified(List.of(
            new FieldDiff("hose_id", "A", "B")));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.CRITICAL);
    }

    @ParameterizedTest(name = "qty {0} → {1} (변경 {2}%) = {3}")
    @CsvSource({
        "100,120,20,CRITICAL",       // 정확 +20%
        "100,119,19,NORMAL",         // -1%pt 아래
        "100,80,20,CRITICAL",        // 정확 -20%
        "100,81,19,NORMAL",          // -1%pt 아래
        "100,200,100,CRITICAL",      // +100%
        "100,50,50,CRITICAL",        // -50%
        "1000,1001,0.1,NORMAL",      // 미세 변경
        "500,600,20,CRITICAL"        // 정확 +20%
    })
    @DisplayName("qty ±20% 경계 — Critical 분류 (BR-O02)")
    void qty_threshold_boundary(int before, int after, double changePct, Severity expected) {
        RowDiff diff = modified(List.of(new FieldDiff("qty", before, after)));
        assertThat(classifier.classify(diff)).isEqualTo(expected);
    }

    @Test
    @DisplayName("qty 0 → N 변경 → CRITICAL (특수 케이스)")
    void qty_zero_to_nonzero_critical() {
        RowDiff diff = modified(List.of(new FieldDiff("qty", 0, 50)));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("qty 0 → 0 (변경 없음 동등) → NORMAL")
    void qty_zero_to_zero_normal() {
        RowDiff diff = modified(List.of(new FieldDiff("qty", 0, 0)));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.NORMAL);
    }

    @Test
    @DisplayName("qty non-numeric (string) → CRITICAL (conservative)")
    void qty_non_numeric_is_critical_conservative() {
        RowDiff diff = modified(List.of(new FieldDiff("qty", "abc", "def")));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("qty 콤마 표기 문자열 → 숫자 변환 후 비교")
    void qty_comma_string_parsed() {
        RowDiff diff = modified(List.of(new FieldDiff("qty", "1,000", "1,100")));
        // 10% 변경 → NORMAL
        assertThat(classifier.classify(diff)).isEqualTo(Severity.NORMAL);
    }

    @Test
    @DisplayName("MODIFIED customer 변경만 → NORMAL")
    void customer_change_only_normal() {
        RowDiff diff = modified(List.of(
            new FieldDiff("customer", "내수", "현대모비스")));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.NORMAL);
    }

    @Test
    @DisplayName("MODIFIED order_type 변경만 → NORMAL")
    void order_type_change_only_normal() {
        RowDiff diff = modified(List.of(
            new FieldDiff("order_type", "FORECAST", "WEEKLY")));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.NORMAL);
    }

    @Test
    @DisplayName("MODIFIED 다중 필드 — 1개라도 Critical 매치 → CRITICAL")
    void multi_field_any_critical_short_circuits() {
        RowDiff diff = modified(List.of(
            new FieldDiff("customer", "A", "B"),                 // Normal
            new FieldDiff("qty", 100, 100),                       // 변경 없음
            new FieldDiff("delivery_date", D, D.plusDays(1))      // Critical
        ));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.CRITICAL);
    }

    // ---------- 임계치 외부화 ----------

    @Test
    @DisplayName("config 변경 — threshold 50% 시 +30% 변경 → NORMAL")
    void custom_threshold_higher_makes_smaller_changes_normal() {
        config.setQtyChangeThresholdPct(0.50);
        RowDiff diff = modified(List.of(new FieldDiff("qty", 100, 130)));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.NORMAL);
    }

    @Test
    @DisplayName("config — newAlwaysCritical=false → NEW = NORMAL")
    void new_can_be_normal_when_disabled() {
        config.setNewAlwaysCritical(false);
        RowDiff diff = new RowDiff(KEY, DiffType.NEW, null, null, List.of());
        assertThat(classifier.classify(diff)).isEqualTo(Severity.NORMAL);
    }

    @Test
    @DisplayName("config — deliveryDateAlwaysCritical=false → delivery_date 변경 = NORMAL")
    void delivery_date_can_be_normal_when_disabled() {
        config.setDeliveryDateAlwaysCritical(false);
        RowDiff diff = modified(List.of(
            new FieldDiff("delivery_date", D, D.plusDays(5))));
        assertThat(classifier.classify(diff)).isEqualTo(Severity.NORMAL);
    }
}
