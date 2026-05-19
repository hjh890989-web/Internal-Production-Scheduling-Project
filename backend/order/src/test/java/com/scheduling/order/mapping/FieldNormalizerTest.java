package com.scheduling.order.mapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FieldNormalizer 회귀 — TK-01-2-1.
 *
 * <p>날짜·정수·hose_id·canonical 4 종 변환 검증.
 */
class FieldNormalizerTest {

    private final FieldNormalizer norm = new FieldNormalizer();

    @Test
    @DisplayName("canonical — 대소문자·공백·언더스코어 무시")
    void canonical_strips_whitespace_and_underscore() {
        assertThat(norm.canonical("Hose_Id")).isEqualTo("HOSEID");
        assertThat(norm.canonical(" 품번 ")).isEqualTo("품번");
        assertThat(norm.canonical("delivery date")).isEqualTo("DELIVERYDATE");
        assertThat(norm.canonical(null)).isEmpty();
    }

    @Test
    @DisplayName("parseDate — 다중 포맷 시도, ISO datetime 자동 인식")
    void parse_date_multi_format() {
        List<String> hints = List.of("yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd");
        assertThat(norm.parseDate("2026-05-20", hints, true)).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(norm.parseDate("2026/05/20", hints, true)).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(norm.parseDate("20260520", hints, true)).isEqualTo(LocalDate.of(2026, 5, 20));
        // ISO datetime (TK-01-1-1 ExcelParserService 출력 형식)
        assertThat(norm.parseDate("2026-05-20T05:30:00Z", hints, true)).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    @DisplayName("parseDate — 모든 포맷 실패 시 MappingException")
    void parse_date_all_formats_fail() {
        assertThatThrownBy(() -> norm.parseDate("2026.05.20", List.of("yyyy-MM-dd"), true))
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("포맷 인식 실패");
    }

    @Test
    @DisplayName("parseDate — required=false + blank → null")
    void parse_date_optional_blank_returns_null() {
        assertThat(norm.parseDate("", List.of("yyyy-MM-dd"), false)).isNull();
        assertThat(norm.parseDate(null, List.of("yyyy-MM-dd"), false)).isNull();
    }

    @Test
    @DisplayName("parseInt — 콤마·공백·과학표기·소수형 정수")
    void parse_int_handles_variations() {
        assertThat(norm.parseInt("1000", true)).isEqualTo(1000);
        assertThat(norm.parseInt("1,000", true)).isEqualTo(1000);
        assertThat(norm.parseInt(" 1 000 ", true)).isEqualTo(1000);
        assertThat(norm.parseInt("1.0e3", true)).isEqualTo(1000);
        assertThat(norm.parseInt("1000.0", true)).isEqualTo(1000);
    }

    @Test
    @DisplayName("parseInt — 음수·0·소수 거부")
    void parse_int_rejects_invalid() {
        assertThatThrownBy(() -> norm.parseInt("0", true))
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("양수 정수");
        assertThatThrownBy(() -> norm.parseInt("-5", true))
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("양수 정수");
        assertThatThrownBy(() -> norm.parseInt("1.5", true))
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("정수만 허용");
        assertThatThrownBy(() -> norm.parseInt("abc", true))
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("숫자 변환");
    }

    @Test
    @DisplayName("normalizeHoseId — 대문자 + 정규식 검증")
    void normalize_hose_id() {
        assertThat(norm.normalizeHoseId("part-001", "", true)).isEqualTo("PART-001");
        assertThat(norm.normalizeHoseId("29673-2R060", "", true)).isEqualTo("29673-2R060");
        assertThat(norm.normalizeHoseId(" A 672 203 09 00 ", "", true)).isEqualTo("A 672 203 09 00");
    }

    @Test
    @DisplayName("normalizeHoseId — regexStrip 적용")
    void normalize_hose_id_with_strip() {
        assertThat(norm.normalizeHoseId("PART_001", "[_]+", true)).isEqualTo("PART001");
    }

    @Test
    @DisplayName("normalizeHoseId — 정규식 위반 시 MappingException")
    void normalize_hose_id_invalid_pattern() {
        // 한글은 정규식 ^[A-Z0-9 -]+$ 에 매치 안 됨
        assertThatThrownBy(() -> norm.normalizeHoseId("품번001", "", true))
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("정규식 위반");
    }

    @Test
    @DisplayName("normalizeHoseId — 빈 값 거부")
    void normalize_hose_id_blank() {
        assertThatThrownBy(() -> norm.normalizeHoseId("", "", true))
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("값 없음");
        assertThatThrownBy(() -> norm.normalizeHoseId(null, "", true))
            .isInstanceOf(MappingException.class)
            .hasMessageContaining("값 없음");
    }
}
