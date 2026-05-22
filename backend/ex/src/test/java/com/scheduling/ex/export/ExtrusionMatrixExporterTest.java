package com.scheduling.ex.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BR-E09 — 압출 시트명 정규식 + 포맷터 단위 테스트.
 */
class ExtrusionMatrixExporterTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "2026-05-25, 5월25일(압출)",
        "2026-01-01, 1월1일(압출)",
        "2026-12-31, 12월31일(압출)",
        "2026-02-09, 2월9일(압출)",   // 한 자리 일자
    })
    @DisplayName("formatSheetName — M월d일(압출) leading zero 없음 (BR-E09)")
    void format_sheet_name(String input, String expected) {
        assertThat(ExtrusionMatrixExporter.formatSheetName(LocalDate.parse(input)))
            .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → matches regex")
    @CsvSource({
        "5월25일(압출), true",
        "1월1일(압출), true",
        "12월31일(압출), true",
        "5월25일(성형), false",        // 다른 공정
        "5/25(압출), false",            // 한글 누락
        "5월25일 (압출), false",       // 공백
    })
    @DisplayName("SHEET_NAME_REGEX — BR-E09 정규식 일치/불일치")
    void sheet_name_regex(String name, boolean expected) {
        assertThat(ExtrusionMatrixExporter.SHEET_NAME_REGEX.matcher(name).matches())
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("null date → NPE (방어적 검증은 호출자)")
    void null_date_throws_npe() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
            () -> ExtrusionMatrixExporter.formatSheetName(null));
    }
}
