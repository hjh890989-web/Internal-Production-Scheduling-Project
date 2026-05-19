package com.scheduling.order.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SourceClassifierService 회귀 테스트 — TK-01-1-2.
 *
 * <p>14 케이스: 4 SourceType × 7 (월별=2·주간=2·확정=2·KD=2) + 모호 + 미식별 + 추가.
 * 실제 30 회귀 워크북은 src/test/resources/workbooks/ 위치 (TK-01-1-4 에서 합성).
 * 본 단위 테스트는 ParsedWorkbook 모킹으로 분류기 로직 자체 검증.
 */
class SourceClassifierServiceTest {

    private SourceClassifierService classifier;

    @BeforeEach
    void setUp() {
        classifier = new SourceClassifierService(
            new ClassPathResource("classification/header-signatures.yaml")
        );
        classifier.load();
    }

    // ---------- 헬퍼: 시그니처 키워드를 헤더에 포함한 워크북 생성 ----------
    private ParsedWorkbook workbook(String filename, String sheetName, List<String> headers) {
        ParsedWorkbook wb = new ParsedWorkbook(filename);
        ParsedSheet sheet = new ParsedSheet(sheetName);
        sheet.addRow(new ParsedRow(0, headers));
        wb.addSheet(sheet);
        return wb;
    }

    @Test
    @DisplayName("월별 예상 — 파일명+시트명 매치 → MONTHLY_FORECAST")
    void monthly_forecast_filename() {
        ParsedWorkbook wb = workbook(
            "2026년 1월 월별 예상 발주.xlsx",
            "월간 예상",
            List.of("품번", "수량", "예상")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.MONTHLY_FORECAST);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.5);
    }

    @Test
    @DisplayName("월별 예상 — 영문 FORECAST 키워드")
    void monthly_forecast_english() {
        ParsedWorkbook wb = workbook(
            "monthly_forecast_2026.xlsx",
            "Forecast",
            List.of("Part", "Qty")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.MONTHLY_FORECAST);
    }

    @Test
    @DisplayName("주간 계획 — 주차 키워드")
    void weekly_plan_jucha() {
        ParsedWorkbook wb = workbook(
            "실리콘 02월 1주차 주간 계획.xlsx",
            "주간 계획",
            List.of("품번", "Mon", "Tue")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.WEEKLY_PLAN);
    }

    @Test
    @DisplayName("주간 계획 — WEEKLY 영문")
    void weekly_plan_english() {
        ParsedWorkbook wb = workbook(
            "weekly_plan_w05.xlsx",
            "WEEKLY",
            List.of("Hose", "Mon", "Fri")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.WEEKLY_PLAN);
    }

    @Test
    @DisplayName("확정 발주 — 확정 키워드")
    void confirmed_order_korean() {
        ParsedWorkbook wb = workbook(
            "2026년 1월 확정 발주.xlsx",
            "확정 발주",
            List.of("품번", "확정수량")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.CONFIRMED_ORDER);
    }

    @Test
    @DisplayName("확정 발주 — CONFIRMED 영문")
    void confirmed_order_english() {
        ParsedWorkbook wb = workbook(
            "confirmed_order_jan.xlsx",
            "CONFIRMED",
            List.of("Hose", "Qty")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.CONFIRMED_ORDER);
    }

    @Test
    @DisplayName("KD 발주 — KD 약어 + 강한 시그니처")
    void kd_order_abbreviation() {
        ParsedWorkbook wb = workbook(
            "저압 이중관 KD 발주및 납품현황 26년01월.xlsx",
            "KD 발주",
            List.of("품번", "KD수량")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.KD_ORDER);
        // weight 1.5 → confidence 더 높음
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.5);
    }

    @Test
    @DisplayName("KD 발주 — Knock-Down 전체 표기")
    void kd_order_knockdown() {
        ParsedWorkbook wb = workbook(
            "knockdown_jan.xlsx",
            "Knock-Down 발주",
            List.of("Part", "Qty")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.KD_ORDER);
    }

    @Test
    @DisplayName("미식별 — 무관한 워크북 (재고 현황) → UNRECOGNIZED")
    void unrecognized_unrelated() {
        ParsedWorkbook wb = workbook(
            "재고 현황 26년1월.xlsx",
            "재고",
            List.of("품번", "재고수량")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.UNRECOGNIZED);
        assertThat(result.confidence()).isLessThan(0.5);
    }

    @Test
    @DisplayName("미식별 — 빈 워크북")
    void unrecognized_empty() {
        ParsedWorkbook wb = new ParsedWorkbook("blank.xlsx");
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.UNRECOGNIZED);
    }

    @Test
    @DisplayName("excluded 룰 — 월별 키워드 + 주간 키워드 → 주간 score 0")
    void excluded_cross_keyword() {
        ParsedWorkbook wb = workbook(
            "월별 예상_주간 변환.xlsx",
            "주간 계획",
            List.of("월별")
        );
        ClassificationResult result = classifier.classify(wb);
        // 월별/주간 양쪽 모두 excluded 충돌 → KD/확정 후보도 매칭 안 됨 → UNRECOGNIZED
        // 또는 KD/CONFIRMED 매칭 없어서 점수 모두 0
        assertThat(result.sourceType())
            .as("교차 키워드 — 양쪽 모두 excluded 충돌")
            .isEqualTo(SourceType.UNRECOGNIZED);
    }

    @Test
    @DisplayName("KD 강한 시그니처 — KD + 월별 혼합 → KD 우선 (excluded 영향 없음)")
    void kd_priority_over_monthly() {
        ParsedWorkbook wb = workbook(
            "KD 월별 발주 현황.xlsx",
            "KD",
            List.of("품번")
        );
        ClassificationResult result = classifier.classify(wb);
        // 월별은 KD excluded 가 매칭 → 0 / KD 는 weight 1.5 우선
        assertThat(result.sourceType()).isEqualTo(SourceType.KD_ORDER);
    }

    @Test
    @DisplayName("allScores — 모든 SourceType 점수 추적 가능 (디버깅 정보)")
    void all_scores_recorded() {
        ParsedWorkbook wb = workbook(
            "월별 예상.xlsx",
            "Forecast",
            List.of("품번")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.allScores()).hasSize(4);
        assertThat(result.allScores().get(SourceType.MONTHLY_FORECAST)).isPositive();
    }

    @Test
    @DisplayName("대소문자 무시 — kd 소문자도 매칭")
    void case_insensitive_matching() {
        ParsedWorkbook wb = workbook(
            "Jan_kd_order.xlsx",
            "kd",
            List.of("Hose")
        );
        ClassificationResult result = classifier.classify(wb);
        assertThat(result.sourceType()).isEqualTo(SourceType.KD_ORDER);
    }
}
