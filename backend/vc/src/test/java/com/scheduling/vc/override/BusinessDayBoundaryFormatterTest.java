package com.scheduling.vc.override;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BusinessDayBoundaryFormatter — TK-13-3-1·3 (BR-V07).
 */
class BusinessDayBoundaryFormatterTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "2026-03-02, 2026-03-02_END",       // 월
        "2026-03-06, 2026-03-06_END",       // 금
        "2026-02-13, 2026-02-13_END",       // 설날 직전
        "2026-12-31, 2026-12-31_END",       // 연말
    })
    @DisplayName("DO-04 영업일 경계 키 YYYY-MM-DD_END (월~금 + 연휴 직전)")
    void boundary_key_format(String input, String expected) {
        assertThat(BusinessDayBoundaryFormatter.formatBoundaryKey(LocalDate.parse(input)))
            .isEqualTo(expected);
    }

    @Test
    @DisplayName("null productionDate → IllegalArgumentException")
    void null_date_rejected() {
        assertThatThrownBy(() -> BusinessDayBoundaryFormatter.formatBoundaryKey(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("productionDate");
    }
}
