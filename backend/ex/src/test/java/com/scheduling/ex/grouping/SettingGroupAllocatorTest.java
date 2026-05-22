package com.scheduling.ex.grouping;

import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.master.api.HoseSettingGroupSummary;
import com.scheduling.master.api.SettingGroupLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SettingGroupAllocator 단위 — TK-09-1-2 (BR-E06·E07).
 *
 * <p>SettingGroupLookup mock — shift 내 단일 그룹 강제 알고리즘 격리.
 */
class SettingGroupAllocatorTest {

    private SettingGroupLookup lookup;
    private SettingGroupAllocator allocator;

    @BeforeEach
    void setUp() {
        lookup = mock(SettingGroupLookup.class);
        allocator = new SettingGroupAllocator(lookup);
    }

    private ExScheduleCandidate candidate(String hose, LocalDate deadline) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-22T00:00:00Z");
        return new ExScheduleCandidate(
            id, UUID.randomUUID(), hose, UUID.randomUUID(),
            deadline.plusDays(1), deadline, 100,
            CandidateStatus.PENDING, now, now);
    }

    private void mockGroup(String hose, short group) {
        when(lookup.findPrimaryGroup(eq(hose))).thenReturn(Optional.of(
            new HoseSettingGroupSummary(hose, group, true)));
    }

    // ---------- 빈/null 입력 ----------

    @Test
    @DisplayName("빈 / null candidates → 빈 assignment")
    void empty_input() {
        assertThat(allocator.allocate(null)).isEmpty();
        assertThat(allocator.allocate(List.of())).isEmpty();
    }

    // ---------- 단일 그룹 ----------

    @Test
    @DisplayName("같은 그룹 hose 3건 같은 일자 → 1 shift 배정 (BR-E07 동시생산)")
    void same_group_same_date_single_shift() {
        LocalDate d = LocalDate.of(2026, 3, 5);
        mockGroup("28442-6T010", (short) 1);
        mockGroup("28415-08400", (short) 1);
        mockGroup("25490-03HA0", (short) 1);

        List<ShiftAssignment> out = allocator.allocate(List.of(
            candidate("28442-6T010", d),
            candidate("28415-08400", d),
            candidate("25490-03HA0", d)));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).shiftCode()).isEqualTo("DAY_EARLY");
        assertThat(out.get(0).groupNumber()).isEqualTo((short) 1);
        assertThat(out.get(0).candidateIds()).hasSize(3);
    }

    @Test
    @DisplayName("다른 그룹 2건 같은 일자 → 2 shift 분리 (BR-E06 셋업 0건)")
    void different_groups_different_shifts() {
        LocalDate d = LocalDate.of(2026, 3, 5);
        mockGroup("28442-6T010", (short) 1);
        mockGroup("29673-2R060", (short) 5);

        List<ShiftAssignment> out = allocator.allocate(List.of(
            candidate("28442-6T010", d),
            candidate("29673-2R060", d)));

        assertThat(out).hasSize(2);
        assertThat(out).extracting(ShiftAssignment::shiftCode)
            .containsExactly("DAY_EARLY", "DAY_LATE");
        assertThat(out).extracting(ShiftAssignment::groupNumber)
            .containsExactlyInAnyOrder((short) 1, (short) 5);
    }

    @Test
    @DisplayName("4 그룹 같은 일자 → 4 shift 모두 사용 (DAY_EARLY → NIGHT_LATE)")
    void four_groups_fill_all_shifts() {
        LocalDate d = LocalDate.of(2026, 3, 5);
        mockGroup("A", (short) 1);
        mockGroup("B", (short) 2);
        mockGroup("C", (short) 3);
        mockGroup("D", (short) 4);

        List<ShiftAssignment> out = allocator.allocate(List.of(
            candidate("A", d), candidate("B", d), candidate("C", d), candidate("D", d)));

        assertThat(out).hasSize(4);
        assertThat(out).extracting(ShiftAssignment::shiftCode)
            .containsExactly("DAY_EARLY", "DAY_LATE", "NIGHT_EARLY", "NIGHT_LATE");
    }

    @Test
    @DisplayName("5 그룹 같은 일자 → shift 부족, 5번째 배정 실패 (4 shift 한도)")
    void five_groups_overflow_skipped() {
        LocalDate d = LocalDate.of(2026, 3, 5);
        for (int i = 1; i <= 5; i++) {
            mockGroup("hose" + i, (short) i);
        }

        List<ShiftAssignment> out = allocator.allocate(List.of(
            candidate("hose1", d), candidate("hose2", d), candidate("hose3", d),
            candidate("hose4", d), candidate("hose5", d)));

        assertThat(out).hasSize(4);  // 5번째는 shift 부족으로 제외
    }

    // ---------- 다중 일자 ----------

    @Test
    @DisplayName("다른 일자 2 그룹 → 일자별 독립 shift 배정")
    void different_dates_independent_shifts() {
        LocalDate d1 = LocalDate.of(2026, 3, 5);
        LocalDate d2 = LocalDate.of(2026, 3, 6);
        mockGroup("A", (short) 1);
        mockGroup("B", (short) 2);

        List<ShiftAssignment> out = allocator.allocate(List.of(
            candidate("A", d1), candidate("B", d1),
            candidate("A", d2), candidate("B", d2)));

        assertThat(out).hasSize(4);
        // d1: A→DAY_EARLY, B→DAY_LATE / d2: A→DAY_EARLY, B→DAY_LATE
        long dayEarlyCount = out.stream().filter(s -> "DAY_EARLY".equals(s.shiftCode())).count();
        assertThat(dayEarlyCount).as("일자별 DAY_EARLY 1회씩").isEqualTo(2L);
    }

    @Test
    @DisplayName("미매핑 hose → group 0 (fallback)")
    void unmapped_hose_fallback_group_zero() {
        LocalDate d = LocalDate.of(2026, 3, 5);
        when(lookup.findPrimaryGroup(eq("UNKNOWN"))).thenReturn(Optional.empty());

        List<ShiftAssignment> out = allocator.allocate(List.of(candidate("UNKNOWN", d)));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).groupNumber()).isZero();
    }
}
