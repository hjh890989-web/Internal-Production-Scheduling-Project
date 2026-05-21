package com.scheduling.vc.calendar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WorkingCalendarService stub — TK-05-1-2 (EP-06 정식 구현 전).
 */
class WorkingCalendarServiceTest {

    private final WorkingCalendarService service = new WorkingCalendarService();

    @Test
    @DisplayName("월~금 → 영업일 true")
    void monday_to_friday_is_working_day() {
        // 2026-02-16(월) ~ 2026-02-20(금)
        for (int d = 16; d <= 20; d++) {
            assertThat(service.isWorkingDay(LocalDate.of(2026, 2, d)))
                .as("2026-02-%d", d).isTrue();
        }
    }

    @Test
    @DisplayName("토·일 → 영업일 false")
    void weekend_is_not_working_day() {
        assertThat(service.isWorkingDay(LocalDate.of(2026, 2, 14))).isFalse(); // 토
        assertThat(service.isWorkingDay(LocalDate.of(2026, 2, 15))).isFalse(); // 일
        assertThat(service.isWorkingDay(LocalDate.of(2026, 2, 21))).isFalse(); // 토
        assertThat(service.isWorkingDay(LocalDate.of(2026, 2, 22))).isFalse(); // 일
    }

    @Test
    @DisplayName("null → false (defensive)")
    void null_returns_false() {
        assertThat(service.isWorkingDay(null)).isFalse();
    }
}
