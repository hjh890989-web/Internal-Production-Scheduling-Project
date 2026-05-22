package com.scheduling.ex.event;

import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.vc.events.VcChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ImpactedRowFinder 단위 — TK-EX13-1-2 (BR-E11).
 */
class ImpactedRowFinderTest {

    private ExScheduleCandidateRepository repo;
    private ImpactedRowFinder finder;

    @BeforeEach
    void setUp() {
        repo = mock(ExScheduleCandidateRepository.class);
        finder = new ImpactedRowFinder(repo);
    }

    private ExScheduleCandidate candidate(UUID id, String hose, LocalDate deadline) {
        Instant now = Instant.parse("2026-05-22T00:00:00Z");
        return new ExScheduleCandidate(
            id, UUID.randomUUID(), hose, UUID.randomUUID(),
            deadline.plusDays(1), deadline, 100,
            CandidateStatus.PENDING, now, now);
    }

    private VcChangedEvent.VcChangedRow changedRow(UUID rowId, String hose,
                                                    LocalDate prevDate, LocalDate newDate,
                                                    VcChangedEvent.ChangeType type) {
        return new VcChangedEvent.VcChangedRow(rowId, hose, prevDate, newDate, 100, 100, type);
    }

    // ---------- 빈 입력 ----------

    @Test
    @DisplayName("빈 / null event → 빈 impacted list")
    void empty_event() {
        assertThat(finder.findImpacted(null)).isEmpty();
        assertThat(finder.findImpacted(new VcChangedEvent(UUID.randomUUID(), Instant.now(), List.of())))
            .isEmpty();
    }

    // ---------- QUANTITY 변경 ----------

    @Test
    @DisplayName("QUANTITY 변경 → vc_row_id 직접 매핑 candidate 만")
    void quantity_change_direct_only() {
        UUID rowId = UUID.randomUUID();
        UUID exId = UUID.randomUUID();
        when(repo.findByVcRowId(eq(rowId))).thenReturn(Optional.of(
            candidate(exId, "X", LocalDate.of(2026, 3, 5))));

        var event = new VcChangedEvent(UUID.randomUUID(), Instant.now(),
            List.of(changedRow(rowId, "X", null, null, VcChangedEvent.ChangeType.QUANTITY)));

        List<UUID> impacted = finder.findImpacted(event);
        assertThat(impacted).containsExactly(exId);
    }

    // ---------- DATE 변경 ----------

    @Test
    @DisplayName("DATE 변경 → vc_row_id 직접 + horizon ±3일 인접 candidate")
    void date_change_includes_nearby() {
        UUID rowId = UUID.randomUUID();
        UUID directExId = UUID.randomUUID();
        UUID nearbyExId1 = UUID.randomUUID();
        UUID nearbyExId2 = UUID.randomUUID();

        when(repo.findByVcRowId(eq(rowId))).thenReturn(Optional.of(
            candidate(directExId, "X", LocalDate.of(2026, 3, 5))));
        when(repo.findByHoseIdAndExtrusionDeadlineBetween(eq("X"), any(), any()))
            .thenReturn(List.of(
                candidate(nearbyExId1, "X", LocalDate.of(2026, 3, 6)),
                candidate(nearbyExId2, "X", LocalDate.of(2026, 3, 8))));

        var event = new VcChangedEvent(UUID.randomUUID(), Instant.now(), List.of(
            changedRow(rowId, "X",
                LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 8),
                VcChangedEvent.ChangeType.DATE)));

        List<UUID> impacted = finder.findImpacted(event);
        assertThat(impacted).contains(directExId, nearbyExId1, nearbyExId2);
    }

    // ---------- DELETED 변경 ----------

    @Test
    @DisplayName("DELETED 변경 → vc_row_id 직접 매핑만 (인접 검색 안 함)")
    void deleted_direct_only() {
        UUID rowId = UUID.randomUUID();
        UUID exId = UUID.randomUUID();
        when(repo.findByVcRowId(eq(rowId))).thenReturn(Optional.of(
            candidate(exId, "X", LocalDate.of(2026, 3, 5))));

        var event = new VcChangedEvent(UUID.randomUUID(), Instant.now(),
            List.of(changedRow(rowId, "X", null, null, VcChangedEvent.ChangeType.DELETED)));

        List<UUID> impacted = finder.findImpacted(event);
        assertThat(impacted).containsExactly(exId);
    }

    // ---------- 매핑 없음 ----------

    @Test
    @DisplayName("매핑 candidate 없음 → 빈 impacted list")
    void no_mapping_returns_empty() {
        UUID rowId = UUID.randomUUID();
        when(repo.findByVcRowId(eq(rowId))).thenReturn(Optional.empty());

        var event = new VcChangedEvent(UUID.randomUUID(), Instant.now(),
            List.of(changedRow(rowId, "X", null, null, VcChangedEvent.ChangeType.QUANTITY)));

        assertThat(finder.findImpacted(event)).isEmpty();
    }

    // ---------- 중복 제거 ----------

    @Test
    @DisplayName("같은 candidate 중복 영향 → distinct 제거 (Set 보장)")
    void duplicate_candidates_deduplicated() {
        UUID rowId1 = UUID.randomUUID();
        UUID rowId2 = UUID.randomUUID();
        UUID exId = UUID.randomUUID();

        when(repo.findByVcRowId(eq(rowId1))).thenReturn(Optional.of(
            candidate(exId, "X", LocalDate.of(2026, 3, 5))));
        when(repo.findByVcRowId(eq(rowId2))).thenReturn(Optional.of(
            candidate(exId, "X", LocalDate.of(2026, 3, 5))));  // 같은 candidate

        var event = new VcChangedEvent(UUID.randomUUID(), Instant.now(), List.of(
            changedRow(rowId1, "X", null, null, VcChangedEvent.ChangeType.QUANTITY),
            changedRow(rowId2, "X", null, null, VcChangedEvent.ChangeType.MACHINE)));

        assertThat(finder.findImpacted(event)).containsExactly(exId);
    }
}
