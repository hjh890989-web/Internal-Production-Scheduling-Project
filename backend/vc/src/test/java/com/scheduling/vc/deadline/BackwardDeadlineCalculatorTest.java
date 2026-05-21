package com.scheduling.vc.deadline;

import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.required.OrderInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BackwardDeadlineCalculator 단위 — TK-06-1-2 (REQ-FUNC-VC-008 / BR-X07).
 *
 * <p>WorkingCalendar mock 으로 D-2 영업일 역산 + 같은 hose 다중 수주 = 가장 이른 납기 우선 검증.
 */
class BackwardDeadlineCalculatorTest {

    private WorkingCalendar calendar;
    private BackwardDeadlineCalculator calc;

    @BeforeEach
    void setUp() {
        calendar = mock(WorkingCalendar.class);
        calc = new BackwardDeadlineCalculator(calendar);
    }

    @Test
    @DisplayName("compute — 단일 hose 단일 수주 → calendar.subtractWorkingDays(delivery, 2)")
    void compute_single_hose() {
        LocalDate delivery = LocalDate.of(2026, 3, 9); // 월
        LocalDate expected = LocalDate.of(2026, 3, 5); // 목
        when(calendar.subtractWorkingDays(eq(delivery), eq(2))).thenReturn(expected);

        DeadlineMap map = calc.compute(Map.of(
            "A", List.of(new OrderInput(UUID.randomUUID(), "A", delivery, 100))
        ));

        assertThat(map.get("A")).contains(expected);
    }

    @Test
    @DisplayName("compute — 같은 hose 다중 수주 → 가장 이른 납기 기준 deadline (hard 제약)")
    void compute_multiple_orders_uses_earliest_delivery() {
        LocalDate early = LocalDate.of(2026, 2, 27); // 금
        LocalDate mid = LocalDate.of(2026, 3, 6);
        LocalDate late = LocalDate.of(2026, 3, 13);
        LocalDate expected = LocalDate.of(2026, 2, 25); // early - 2 영업일
        when(calendar.subtractWorkingDays(eq(early), eq(2))).thenReturn(expected);

        DeadlineMap map = calc.compute(Map.of(
            "A", List.of(
                new OrderInput(UUID.randomUUID(), "A", mid, 100),
                new OrderInput(UUID.randomUUID(), "A", early, 50),    // 가장 이른 납기
                new OrderInput(UUID.randomUUID(), "A", late, 80)
            )
        ));

        assertThat(map.get("A")).contains(expected);
    }

    @Test
    @DisplayName("compute — 다중 hose 각각 별도 deadline")
    void compute_multi_hose_independent() {
        LocalDate dA = LocalDate.of(2026, 3, 6);
        LocalDate dB = LocalDate.of(2026, 3, 9);
        when(calendar.subtractWorkingDays(eq(dA), eq(2))).thenReturn(LocalDate.of(2026, 3, 4));
        when(calendar.subtractWorkingDays(eq(dB), eq(2))).thenReturn(LocalDate.of(2026, 3, 5));

        DeadlineMap map = calc.compute(Map.of(
            "A", List.of(new OrderInput(UUID.randomUUID(), "A", dA, 10)),
            "B", List.of(new OrderInput(UUID.randomUUID(), "B", dB, 10))
        ));

        assertThat(map.get("A")).contains(LocalDate.of(2026, 3, 4));
        assertThat(map.get("B")).contains(LocalDate.of(2026, 3, 5));
    }

    @Test
    @DisplayName("compute — null/빈 입력 → empty DeadlineMap")
    void compute_empty_input() {
        assertThat(calc.compute(null).map()).isEmpty();
        assertThat(calc.compute(Map.of()).map()).isEmpty();
    }

    @Test
    @DisplayName("deadlineFor — 단일 납기일 직접 변환")
    void deadline_for_single_date() {
        LocalDate delivery = LocalDate.of(2026, 9, 29); // 추석 직후 화
        LocalDate expected = LocalDate.of(2026, 9, 23);
        when(calendar.subtractWorkingDays(eq(delivery), eq(2))).thenReturn(expected);

        assertThat(calc.deadlineFor(delivery)).isEqualTo(expected);
    }

    @Test
    @DisplayName("DeadlineMap.isWithinDeadline — production_date ≤ deadline")
    void deadline_map_within_check() {
        DeadlineMap map = new DeadlineMap(Map.of("A", LocalDate.of(2026, 3, 5)));

        assertThat(map.isWithinDeadline("A", LocalDate.of(2026, 3, 5))).isTrue();   // 동일
        assertThat(map.isWithinDeadline("A", LocalDate.of(2026, 3, 4))).isTrue();   // 이전
        assertThat(map.isWithinDeadline("A", LocalDate.of(2026, 3, 6))).isFalse();  // 이후
        assertThat(map.isWithinDeadline("UNKNOWN", LocalDate.of(2026, 3, 10))).isTrue(); // 미등록 → true
    }

    @Test
    @DisplayName("DeadlineMap — Map.copyOf 불변 보장")
    void deadline_map_immutable() {
        DeadlineMap map = new DeadlineMap(Map.of("A", LocalDate.of(2026, 3, 5)));
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                map.map().put("X", LocalDate.of(2026, 3, 10))
            ).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("BACKWARD_DAYS = 2 (BR-X07 상수)")
    void backward_days_is_two() {
        assertThat(BackwardDeadlineCalculator.BACKWARD_DAYS).isEqualTo(2);
    }
}
