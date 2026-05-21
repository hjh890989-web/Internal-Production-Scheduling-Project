package com.scheduling.master.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WorkingCalendarService 단위 — TK-06-1-1.
 *
 * <p>2026 KR 법정공휴일 fixture (설날 2/16~18, 부처님오신날 5/22, 추석 9/24~26) 로
 * 월~금 + 공휴일 분기 검증.
 */
class WorkingCalendarServiceTest {

    private static final Set<LocalDate> KR_2026_LEGAL = Set.of(
        LocalDate.of(2026, 1, 1),    // 신정
        LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18),  // 설날
        LocalDate.of(2026, 3, 1),    // 삼일절
        LocalDate.of(2026, 5, 5),    // 어린이날
        LocalDate.of(2026, 5, 22),   // 부처님 오신 날
        LocalDate.of(2026, 6, 6),    // 현충일
        LocalDate.of(2026, 8, 15),   // 광복절
        LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26),  // 추석
        LocalDate.of(2026, 10, 3),   // 개천절
        LocalDate.of(2026, 10, 9),   // 한글날
        LocalDate.of(2026, 12, 25)   // 성탄절
    );

    private WorkingCalendarService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        HolidayRepository repo = mock(HolidayRepository.class);
        when(repo.findAllHolidayDates()).thenReturn(List.copyOf(KR_2026_LEGAL));

        ObjectProvider<HolidayRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(repo);

        service = new WorkingCalendarService(provider);
        service.initCache();
    }

    @Test
    @DisplayName("월~금 (휴일 미등록) → isWorkingDay true")
    void weekday_working() {
        // 2026-02-23(월) ~ 27(금) — 설날 직후 평일 주
        for (int d = 23; d <= 27; d++) {
            assertThat(service.isWorkingDay(LocalDate.of(2026, 2, d)))
                .as("2026-02-%d", d).isTrue();
        }
    }

    @Test
    @DisplayName("토·일 → isWorkingDay false")
    void weekend_not_working() {
        assertThat(service.isWorkingDay(LocalDate.of(2026, 2, 21))).isFalse(); // 토
        assertThat(service.isWorkingDay(LocalDate.of(2026, 2, 22))).isFalse(); // 일
    }

    @Test
    @DisplayName("법정공휴일 → isWorkingDay false + isHoliday true")
    void legal_holiday_excluded() {
        LocalDate seollal = LocalDate.of(2026, 2, 17);
        assertThat(service.isWorkingDay(seollal)).isFalse();
        assertThat(service.isHoliday(seollal)).isTrue();
    }

    @Test
    @DisplayName("null → isWorkingDay/isHoliday false (defensive)")
    void null_input_defensive() {
        assertThat(service.isWorkingDay(null)).isFalse();
        assertThat(service.isHoliday(null)).isFalse();
    }

    @Test
    @DisplayName("subtractWorkingDays(2026-02-20 금, 2) = 2026-02-19 목 — 영업일 카운트 2/19, 2/20 직전")
    void subtract_simple_friday() {
        LocalDate delivery = LocalDate.of(2026, 2, 20); // 금
        // 2/20→2/19(목, working) = 1차감, 2/19→2/18(수, 설날) skip, 2/17(화, 설날) skip, 2/16(월, 설날) skip,
        // 2/13(금, working) = 2차감
        LocalDate deadline = service.subtractWorkingDays(delivery, 2);
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 2, 13));
    }

    @Test
    @DisplayName("subtractWorkingDays(2026-02-23 월, 2) = 2026-02-19 목 — 설날 연휴 3일 + 주말 skip")
    void subtract_after_lunar_holiday() {
        LocalDate delivery = LocalDate.of(2026, 2, 23); // 월 (휴일 아님)
        // 2/23→2/20(금)=1차감, 2/20→2/19(목)=2차감
        LocalDate deadline = service.subtractWorkingDays(delivery, 2);
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 2, 19));
    }

    @Test
    @DisplayName("subtractWorkingDays(2026-03-09 월, 2) = 2026-03-05 목 — 주말만 skip")
    void subtract_simple_after_weekend() {
        LocalDate delivery = LocalDate.of(2026, 3, 9); // 월
        // 3/9→3/6(금)=1차감, 3/6→3/5(목)=2차감
        LocalDate deadline = service.subtractWorkingDays(delivery, 2);
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    @DisplayName("subtractWorkingDays(2026-09-29 화, 2) = 2026-09-22 화 — 추석 연휴 3일 + 주말 skip")
    void subtract_after_chuseok() {
        LocalDate delivery = LocalDate.of(2026, 9, 29); // 화 (추석 다음 주)
        // 9/29→9/28(월)=1, 9/28→9/27(일) skip, 9/26(토 + 추석연휴) skip, 9/25(추석) skip, 9/24(추석연휴) skip,
        // 9/23(수)=2
        LocalDate deadline = service.subtractWorkingDays(delivery, 2);
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 9, 23));
    }

    @Test
    @DisplayName("addWorkingDays(2026-02-13 금, 5) = 2026-02-23 월 (설날 연휴 3일 skip)")
    void add_skips_lunar_new_year() {
        LocalDate start = LocalDate.of(2026, 2, 13); // 금
        // 2/13→2/16(월, 설날) skip → 2/17 skip → 2/18 skip → 2/19(목)=1, 2/20(금)=2, 2/21(토) skip,
        // 2/22(일) skip, 2/23(월)=3, 2/24(화)=4, 2/25(수)=5
        LocalDate end = service.addWorkingDays(start, 5);
        assertThat(end).isEqualTo(LocalDate.of(2026, 2, 25));
    }

    @Test
    @DisplayName("workingDaysBetween(2026-02-16 월, 2026-02-20 금) = 2 — 설날 3일 제외")
    void working_days_between_lunar_week() {
        long count = service.workingDaysBetween(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 20));
        // 2/16~18 설날, 2/19 목 + 2/20 금 = 2 영업일
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("workingDaysBetween from > to → 음수 (대칭)")
    void working_days_between_reversed_negative() {
        long forward = service.workingDaysBetween(LocalDate.of(2026, 2, 23), LocalDate.of(2026, 2, 27));
        long backward = service.workingDaysBetween(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 2, 23));
        assertThat(backward).isEqualTo(-forward);
    }

    @Test
    @DisplayName("workingDaysInRange — 5개 영업일 리스트 (설날 주 = 화·수·목·금 + 직전 금)")
    void working_days_in_range() {
        List<LocalDate> days = service.workingDaysInRange(
            LocalDate.of(2026, 2, 19), LocalDate.of(2026, 2, 25)); // 목~수 (7일 중 영업일?)
        // 2/19 목, 2/20 금, 2/21 토(X), 2/22 일(X), 2/23 월, 2/24 화, 2/25 수 = 5일
        assertThat(days).containsExactly(
            LocalDate.of(2026, 2, 19),
            LocalDate.of(2026, 2, 20),
            LocalDate.of(2026, 2, 23),
            LocalDate.of(2026, 2, 24),
            LocalDate.of(2026, 2, 25)
        );
    }

    @Test
    @DisplayName("addWorkingDays(n=0) = date 자체 (영업일 가정)")
    void add_zero_returns_self() {
        LocalDate d = LocalDate.of(2026, 2, 23); // 월
        assertThat(service.addWorkingDays(d, 0)).isEqualTo(d);
    }

    @Test
    @DisplayName("negative n → 부호 반전 (subtract ↔ add 대칭)")
    void negative_n_inverts() {
        LocalDate d = LocalDate.of(2026, 2, 20); // 금
        assertThat(service.addWorkingDays(d, -2)).isEqualTo(service.subtractWorkingDays(d, 2));
        assertThat(service.subtractWorkingDays(d, -2)).isEqualTo(service.addWorkingDays(d, 2));
    }

    @Test
    @DisplayName("null date → IllegalArgumentException (addWorkingDays / subtractWorkingDays / workingDaysBetween)")
    void null_throws_iae() {
        assertThatThrownBy(() -> service.addWorkingDays(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.subtractWorkingDays(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.workingDaysBetween(null, LocalDate.of(2026, 2, 20))).isInstanceOf(IllegalArgumentException.class);
    }
}
