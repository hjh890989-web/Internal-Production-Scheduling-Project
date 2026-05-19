package com.scheduling.order.mapping;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.domain.OrderType;
import com.scheduling.order.parser.ParsedRow;
import com.scheduling.order.parser.ParsedSheet;
import com.scheduling.order.parser.ParsedWorkbook;
import com.scheduling.order.parser.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SchemaMappingService 회귀 — TK-01-2-1.
 *
 * <p>15+ 케이스 — 4 SourceType × 정상·실패·헤더감지·자료타입·다중시트 등.
 */
class SchemaMappingServiceTest {

    private SchemaMappingService service;
    private MappingRuleLoader loader;

    @BeforeEach
    void setUp() {
        loader = new MappingRuleLoader();
        loader.load();
        SchedulingMetrics metrics = mock(SchedulingMetrics.class);
        service = new SchemaMappingService(loader, new FieldNormalizer(), metrics);
    }

    // ---------- helpers ----------
    private ParsedWorkbook workbook(String name, ParsedSheet... sheets) {
        ParsedWorkbook wb = new ParsedWorkbook(name);
        for (ParsedSheet s : sheets) wb.addSheet(s);
        return wb;
    }

    private ParsedSheet sheet(String name, List<List<String>> rows) {
        ParsedSheet ps = new ParsedSheet(name);
        for (int i = 0; i < rows.size(); i++) {
            ps.addRow(new ParsedRow(i, rows.get(i)));
        }
        return ps;
    }

    // ---------- 1. 정상 매핑 4종 ----------

    @Test
    @DisplayName("MONTHLY_FORECAST — 정상 매핑 → OrderType.FORECAST")
    void monthly_forecast_normal() {
        ParsedWorkbook wb = workbook("monthly.xlsx", sheet("월별 예상", List.of(
            List.of("품번", "수량", "납기"),
            List.of("29673-2F900", "100", "2026-02-15"),
            List.of("28422-2M100", "200", "2026-02-20")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);

        assertThat(result.failures()).isEmpty();
        assertThat(result.successes()).hasSize(2);
        OrderDraft first = result.successes().get(0);
        assertThat(first.hoseId()).isEqualTo("29673-2F900");
        assertThat(first.qty()).isEqualTo(100);
        assertThat(first.deliveryDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(first.orderType()).isEqualTo(OrderType.FORECAST);
        assertThat(first.customer()).isEqualTo("내수");
    }

    @Test
    @DisplayName("WEEKLY_PLAN — 정상 매핑 → OrderType.WEEKLY")
    void weekly_plan_normal() {
        ParsedWorkbook wb = workbook("weekly.xlsx", sheet("주간 계획", List.of(
            List.of("품번", "수량", "납기"),
            List.of("25450-P7200", "50", "2026-02-10")
        )));
        MappingResult result = service.map(wb, SourceType.WEEKLY_PLAN);
        assertThat(result.successes()).hasSize(1);
        assertThat(result.successes().get(0).orderType()).isEqualTo(OrderType.WEEKLY);
    }

    @Test
    @DisplayName("CONFIRMED_ORDER — 정상 매핑 → OrderType.CONFIRMED")
    void confirmed_order_normal() {
        ParsedWorkbook wb = workbook("confirmed.xlsx", sheet("확정 발주", List.of(
            List.of("품번", "확정 수량", "납기"),
            List.of("28421-2M800", "75", "2026-02-12")
        )));
        MappingResult result = service.map(wb, SourceType.CONFIRMED_ORDER);
        assertThat(result.successes()).hasSize(1);
        assertThat(result.successes().get(0).orderType()).isEqualTo(OrderType.CONFIRMED);
    }

    @Test
    @DisplayName("KD_ORDER — 정상 매핑 → OrderType.KD + customer 기본값 KD")
    void kd_order_normal() {
        ParsedWorkbook wb = workbook("kd.xlsx", sheet("KD 발주", List.of(
            List.of("품번", "KD 수량", "납기"),
            List.of("29696-2U000", "30", "2026-02-08")
        )));
        MappingResult result = service.map(wb, SourceType.KD_ORDER);
        assertThat(result.successes()).hasSize(1);
        OrderDraft first = result.successes().get(0);
        assertThat(first.orderType()).isEqualTo(OrderType.KD);
        assertThat(first.customer()).isEqualTo("KD");
    }

    // ---------- 2. 헤더 행 감지 ----------

    @Test
    @DisplayName("헤더가 row 2 에 있는 워크북 자동 감지")
    void detects_header_at_row_2() {
        ParsedWorkbook wb = workbook("offset.xlsx", sheet("Sheet1", List.of(
            List.of("회사명: ABC", "", ""),                            // row 0
            List.of("발행일: 2026-02-15", "", ""),                     // row 1
            List.of("품번", "수량", "납기"),                            // row 2 — 헤더
            List.of("29673-2F900", "100", "2026-02-15")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successes()).hasSize(1);
    }

    @Test
    @DisplayName("헤더 row 없음 → MappingFailure HEADER")
    void missing_header_fails() {
        ParsedWorkbook wb = workbook("noheader.xlsx", sheet("Sheet1", List.of(
            List.of("xxx", "yyy", "zzz"),
            List.of("29673-2F900", "100", "2026-02-15")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).failedField()).isEqualTo("HEADER");
    }

    // ---------- 3. row 단위 실패 ----------

    @Test
    @DisplayName("필수 필드 빈 값 → MappingFailure 누적, 성공 row 는 보존")
    void mixed_success_and_failure() {
        ParsedWorkbook wb = workbook("mixed.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기"),
            List.of("29673-2F900", "100", "2026-02-15"),         // OK
            List.of("", "200", "2026-02-20"),                     // hose_id 누락
            List.of("28422-2M100", "abc", "2026-02-20"),          // qty 비숫자
            List.of("28421-2M800", "75", "잘못된날짜")            // date 포맷 실패
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successes()).hasSize(1);
        assertThat(result.failures()).hasSize(3);
    }

    @Test
    @DisplayName("빈 row 는 무시 (실패도 아님)")
    void empty_rows_skipped() {
        ParsedWorkbook wb = workbook("empty.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기"),
            List.of("", "", ""),
            List.of("29673-2F900", "100", "2026-02-15"),
            List.of("", "", "")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successes()).hasSize(1);
        assertThat(result.failures()).isEmpty();
    }

    // ---------- 4. 다중 시트 ----------

    @Test
    @DisplayName("다중 시트 — 모두 매핑 후 통합")
    void multiple_sheets_combined() {
        ParsedWorkbook wb = workbook("multi.xlsx",
            sheet("월별 1", List.of(
                List.of("품번", "수량", "납기"),
                List.of("29673-2F900", "100", "2026-02-15"))),
            sheet("월별 2", List.of(
                List.of("품번", "수량", "납기"),
                List.of("28422-2M100", "200", "2026-02-20"),
                List.of("28421-2M800", "75", "2026-02-12")))
        );
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successes()).hasSize(3);
    }

    // ---------- 5. UNRECOGNIZED ----------

    @Test
    @DisplayName("UNRECOGNIZED SourceType — 전체 실패 + 사용자 안내")
    void unrecognized_source_type() {
        ParsedWorkbook wb = workbook("unknown.xlsx", sheet("재고", List.of(
            List.of("품번", "재고")
        )));
        MappingResult result = service.map(wb, SourceType.UNRECOGNIZED);
        assertThat(result.successes()).isEmpty();
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).reason()).contains("UNRECOGNIZED");
    }

    // ---------- 6. KPI 메트릭 ----------

    @Test
    @DisplayName("MappingResult successRate — 100% 정상")
    void success_rate_perfect() {
        ParsedWorkbook wb = workbook("perfect.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기"),
            List.of("29673-2F900", "100", "2026-02-15"),
            List.of("28422-2M100", "200", "2026-02-20")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successRate()).isEqualTo(1.0);
        assertThat(result.requiresReviewModal()).isFalse();
    }

    @Test
    @DisplayName("requiresReviewModal — 실패율 1% 이상 시 true (REQ-FUNC-OC-004)")
    void review_modal_threshold() {
        // 1 success + 1 failure → 50% 실패율 → modal 필요
        ParsedWorkbook wb = workbook("threshold.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기"),
            List.of("29673-2F900", "100", "2026-02-15"),
            List.of("", "100", "2026-02-15")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.requiresReviewModal()).isTrue();
    }

    // ---------- 7. 데이터 변환 정확성 ----------

    @Test
    @DisplayName("수량 콤마 표기 → 정수 변환")
    void qty_with_comma() {
        ParsedWorkbook wb = workbook("comma.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기"),
            List.of("29673-2F900", "1,000", "2026-02-15")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successes().get(0).qty()).isEqualTo(1000);
    }

    @Test
    @DisplayName("날짜 다중 포맷 — yyyy/MM/dd, yyyyMMdd 모두 수용")
    void date_multi_format() {
        ParsedWorkbook wb = workbook("dates.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기"),
            List.of("29673-2F900", "100", "2026/02/15"),
            List.of("28422-2M100", "200", "20260220")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successes()).hasSize(2);
        assertThat(result.successes().get(0).deliveryDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(result.successes().get(1).deliveryDate()).isEqualTo(LocalDate.of(2026, 2, 20));
    }

    @Test
    @DisplayName("품번 대소문자 정규화 — part-001 → PART-001")
    void hose_id_uppercase() {
        ParsedWorkbook wb = workbook("case.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기"),
            List.of("part-001", "100", "2026-02-15")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successes().get(0).hoseId()).isEqualTo("PART-001");
    }

    @Test
    @DisplayName("customer 컬럼 없으면 룰셋 default 적용")
    void customer_default_applied() {
        ParsedWorkbook wb = workbook("nocust.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기"),
            List.of("29673-2F900", "100", "2026-02-15")
        )));
        MappingResult result = service.map(wb, SourceType.KD_ORDER);
        assertThat(result.successes().get(0).customer()).isEqualTo("KD");
    }

    @Test
    @DisplayName("customer 컬럼 있고 값 있으면 사용 (default override)")
    void customer_explicit_overrides_default() {
        ParsedWorkbook wb = workbook("cust.xlsx", sheet("Sheet1", List.of(
            List.of("품번", "수량", "납기", "거래처"),
            List.of("29673-2F900", "100", "2026-02-15", "현대모비스")
        )));
        MappingResult result = service.map(wb, SourceType.MONTHLY_FORECAST);
        assertThat(result.successes().get(0).customer()).isEqualTo("현대모비스");
    }
}
