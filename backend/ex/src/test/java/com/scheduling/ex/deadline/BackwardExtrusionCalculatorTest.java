package com.scheduling.ex.deadline;

import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.events.VcConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BackwardExtrusionCalculator 단위 — TK-07-2-3 (BR-E01).
 *
 * <p>WorkingCalendar mock — 캘린더 동작 자체 검증은 EP-06 책임, 본 Task 는 압출 로직 격리.
 */
class BackwardExtrusionCalculatorTest {

    private WorkingCalendar calendar;
    private BackwardExtrusionCalculator calc;

    @BeforeEach
    void setUp() {
        calendar = mock(WorkingCalendar.class);
        calc = new BackwardExtrusionCalculator(calendar);
    }

    private VcConfirmedEvent.VcConfirmedRow row(String hose, LocalDate date) {
        return new VcConfirmedEvent.VcConfirmedRow(
            UUID.randomUUID(), hose, date, "LP-01", (short) 1, (short) 1, 100);
    }

    // ---------- 7 요일 ParameterizedTest ----------

    @ParameterizedTest
    @CsvSource({
        "2026-03-09, 2026-03-06",     // 월 → 금
        "2026-03-10, 2026-03-09",     // 화 → 월
        "2026-03-11, 2026-03-10",     // 수 → 화
        "2026-03-12, 2026-03-11",     // 목 → 수
        "2026-03-13, 2026-03-12",     // 금 → 목
        "2026-03-07, 2026-03-06",     // 토 → 금
        "2026-03-08, 2026-03-06"      // 일 → 금
    })
    @DisplayName("7 요일 deadline 매핑 — 월~금 1영업일 차감, 토·일 → 직전 금요일")
    void seven_day_combinations(String vcDateStr, String expectedDeadlineStr) {
        LocalDate vcDate = LocalDate.parse(vcDateStr);
        LocalDate expected = LocalDate.parse(expectedDeadlineStr);
        when(calendar.subtractWorkingDays(eq(vcDate), eq(1))).thenReturn(expected);

        assertThat(calc.deadlineFor(vcDate)).isEqualTo(expected);
    }

    // ---------- compute(event) ----------

    @Test
    @DisplayName("빈 event → 빈 deadline map")
    void empty_event_yields_empty_map() {
        VcConfirmedEvent empty = new VcConfirmedEvent(UUID.randomUUID(), Instant.now(), List.of());
        assertThat(calc.compute(empty).map()).isEmpty();
    }

    @Test
    @DisplayName("null event → 빈 deadline map (defensive)")
    void null_event_yields_empty_map() {
        assertThat(calc.compute(null).map()).isEmpty();
    }

    @Test
    @DisplayName("다중 hose × 다중 row → hose 별 가장 이른 vc_date 기준 deadline")
    void multi_hose_multi_row_uses_earliest_per_hose() {
        LocalDate early = LocalDate.of(2026, 3, 5);
        LocalDate late1 = LocalDate.of(2026, 3, 12);
        LocalDate other = LocalDate.of(2026, 3, 10);

        when(calendar.subtractWorkingDays(eq(early), eq(1))).thenReturn(LocalDate.of(2026, 3, 4));
        when(calendar.subtractWorkingDays(eq(other), eq(1))).thenReturn(LocalDate.of(2026, 3, 9));

        VcConfirmedEvent event = new VcConfirmedEvent(UUID.randomUUID(), Instant.now(), List.of(
            row("28912", late1),
            row("28912", early),                // earliest for 28912
            row("28912", LocalDate.of(2026, 3, 19)),
            row("29689", other)
        ));

        ExDeadlineMap deadlines = calc.compute(event);

        assertThat(deadlines.get("28912")).contains(LocalDate.of(2026, 3, 4));
        assertThat(deadlines.get("29689")).contains(LocalDate.of(2026, 3, 9));
    }

    @Test
    @DisplayName("단일 hose × 단일 row → 1:1 매핑")
    void single_row_single_hose() {
        LocalDate vc = LocalDate.of(2026, 3, 11);
        when(calendar.subtractWorkingDays(eq(vc), eq(1))).thenReturn(LocalDate.of(2026, 3, 10));

        VcConfirmedEvent event = new VcConfirmedEvent(UUID.randomUUID(), Instant.now(),
            List.of(row("A6722", vc)));
        ExDeadlineMap deadlines = calc.compute(event);

        assertThat(deadlines.get("A6722")).contains(LocalDate.of(2026, 3, 10));
    }

    // ---------- ExDeadlineMap ----------

    @Test
    @DisplayName("DeadlineMap.isWithinDeadline — 경계 (≤, ≥, unknown)")
    void deadline_map_within_check() {
        ExDeadlineMap map = new ExDeadlineMap(Map.of("X", LocalDate.of(2026, 3, 5)));

        assertThat(map.isWithinDeadline("X", LocalDate.of(2026, 3, 5))).isTrue();   // 동일
        assertThat(map.isWithinDeadline("X", LocalDate.of(2026, 3, 4))).isTrue();   // 이전
        assertThat(map.isWithinDeadline("X", LocalDate.of(2026, 3, 6))).isFalse();  // 이후
        assertThat(map.isWithinDeadline("UNK", LocalDate.of(2026, 3, 5))).isTrue(); // 미등록 → pass
    }

    @Test
    @DisplayName("DeadlineMap — Map.copyOf 불변 보장")
    void deadline_map_immutable() {
        when(calendar.subtractWorkingDays(any(), eq(1))).thenReturn(LocalDate.of(2026, 3, 4));
        VcConfirmedEvent event = new VcConfirmedEvent(UUID.randomUUID(), Instant.now(),
            List.of(row("X", LocalDate.of(2026, 3, 5))));
        ExDeadlineMap map = calc.compute(event);

        assertThatThrownBy(() -> map.map().put("Y", LocalDate.of(2026, 3, 10)))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("BACKWARD_DAYS = 1 (BR-E01 상수)")
    void backward_days_is_one() {
        assertThat(BackwardExtrusionCalculator.BACKWARD_DAYS).isEqualTo(1);
    }

    @Test
    @DisplayName("설날 직후 vc_date 2026-02-19(목) → calendar.subtractWorkingDays 1회 호출 검증")
    void post_lunar_new_year_delegate() {
        LocalDate vc = LocalDate.of(2026, 2, 19);
        when(calendar.subtractWorkingDays(eq(vc), eq(1))).thenReturn(LocalDate.of(2026, 2, 13));

        assertThat(calc.deadlineFor(vc)).isEqualTo(LocalDate.of(2026, 2, 13));
    }

    @Test
    @DisplayName("추석 직후 vc_date 2026-09-29(화) → calendar.subtractWorkingDays 위임")
    void post_chuseok_delegate() {
        LocalDate vc = LocalDate.of(2026, 9, 29);
        when(calendar.subtractWorkingDays(eq(vc), eq(1))).thenReturn(LocalDate.of(2026, 9, 23));

        assertThat(calc.deadlineFor(vc)).isEqualTo(LocalDate.of(2026, 9, 23));
    }
}
