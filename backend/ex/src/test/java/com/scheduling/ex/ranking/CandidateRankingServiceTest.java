package com.scheduling.ex.ranking;

import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CandidateRankingService — TK-18-1-1 (REQ-FUNC-XT-001 ≥ 3 ranked candidates).
 */
class CandidateRankingServiceTest {

    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.ofInstant(T0, ZoneOffset.UTC);

    private ExScheduleCandidate candidate(LocalDate deadline, String hose, int yield) {
        return new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), hose,
            UUID.randomUUID(), deadline.plusDays(1), deadline, yield,
            CandidateStatus.SCHEDULED, T0, T0);
    }

    @Test
    @DisplayName("기간 내 후보 0건 → 빈 리스트")
    void empty_returns_empty() {
        ExScheduleCandidateRepository repo = mock(ExScheduleCandidateRepository.class);
        when(repo.findAll()).thenReturn(List.of());
        CandidateRankingService svc = new CandidateRankingService(repo, CLOCK);

        assertThat(svc.rank(TODAY, TODAY.plusDays(7))).isEmpty();
    }

    @Test
    @DisplayName("totalScore = 0.4·slack + 0.3·balance + 0.3·setting, 정렬 desc")
    void scores_sum_to_total_desc_sorted() {
        ExScheduleCandidateRepository repo = mock(ExScheduleCandidateRepository.class);
        // hose A — 단일, deadline +7일 → slack 높음
        // hose B — 2 row, deadline +1일 → slack 낮음
        when(repo.findAll()).thenReturn(List.of(
            candidate(TODAY.plusDays(7), "A", 1000),
            candidate(TODAY.plusDays(1), "B", 1000),
            candidate(TODAY.plusDays(1), "B", 1000)
        ));
        CandidateRankingService svc = new CandidateRankingService(repo, CLOCK);

        List<CandidateRankingService.RankedCandidate> ranked =
            svc.rank(TODAY, TODAY.plusDays(7));

        assertThat(ranked).hasSize(3);
        // 첫 위치 = A (slack ~1.0, setting 1.0)
        assertThat(ranked.get(0).hoseId()).isEqualTo("A");
        // 정렬 desc 검증
        for (int i = 0; i < ranked.size() - 1; i++) {
            assertThat(ranked.get(i).totalScore())
                .isGreaterThanOrEqualTo(ranked.get(i + 1).totalScore());
        }
        // 각 점수 0..1 범위
        ranked.forEach(r -> {
            assertThat(r.slackDaysScore()).isBetween(0.0, 1.0);
            assertThat(r.balanceScore()).isBetween(0.0, 1.0);
            assertThat(r.settingScore()).isBetween(0.0, 1.0);
        });
    }

    @Test
    @DisplayName("REQ-FUNC-XT-001 — ≥ 3 distinct candidates 반환")
    void at_least_three_distinct_candidates() {
        ExScheduleCandidateRepository repo = mock(ExScheduleCandidateRepository.class);
        when(repo.findAll()).thenReturn(List.of(
            candidate(TODAY.plusDays(2), "A", 1000),
            candidate(TODAY.plusDays(3), "B", 2000),
            candidate(TODAY.plusDays(4), "C", 1500),
            candidate(TODAY.plusDays(5), "D", 1800)
        ));
        CandidateRankingService svc = new CandidateRankingService(repo, CLOCK);
        List<CandidateRankingService.RankedCandidate> ranked =
            svc.rank(TODAY, TODAY.plusDays(7));
        assertThat(ranked.size()).isGreaterThanOrEqualTo(3);
        assertThat(ranked.stream().map(r -> r.exCandidateId()).distinct()).hasSize(4);
    }
}
