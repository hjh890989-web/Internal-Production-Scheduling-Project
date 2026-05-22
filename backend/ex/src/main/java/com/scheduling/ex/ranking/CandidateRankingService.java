package com.scheduling.ex.ranking;

import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 다중 후보 ranking — TK-18-1-1 (EP-18, REQ-FUNC-XT-001).
 *
 * <p>3 점수 — 기한 여유 (days to deadline) + 셋업 수 (그룹 변경) + 라인 균형 (yield std-dev).
 * 가중 합산 score 정렬. UI 비교 테이블에 ≥ 3 distinct 반환.
 *
 * <p>Sprint 5 Could — 단순 구현 (기존 후보 데이터 기반 ranking 메타데이터 산출).
 * 정식 multi-candidate Allocator 는 Phase 2+ (ML 추천 검토).
 */
@Service
@Profile("with-infra")
public class CandidateRankingService {

    public record RankedCandidate(
        UUID exCandidateId,
        String hoseId,
        LocalDate extrusionDeadline,
        int vcYield,
        double slackDaysScore,      // 0~1 (높을수록 여유)
        double balanceScore,        // 0~1 (높을수록 균형)
        double settingScore,        // 0~1 (높을수록 그룹 단일)
        double totalScore
    ) {}

    private final ExScheduleCandidateRepository repository;
    private final Clock clock;

    public CandidateRankingService(ExScheduleCandidateRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * 기간 내 candidate ranking 정렬 — ≥ 3 distinct 반환 (REQ-FUNC-XT-001).
     */
    public List<RankedCandidate> rank(LocalDate from, LocalDate to) {
        List<ExScheduleCandidate> rows = repository.findAll().stream()
            .filter(c -> !c.getExtrusionDeadline().isBefore(from)
                      && !c.getExtrusionDeadline().isAfter(to))
            .toList();
        if (rows.isEmpty()) return List.of();

        LocalDate today = LocalDate.now(clock);
        long horizonDays = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(today, to));

        // hose 별 yield 합 (라인 균형 산정용)
        Map<String, Integer> yieldByHose = rows.stream()
            .collect(Collectors.groupingBy(
                ExScheduleCandidate::getHoseId,
                Collectors.summingInt(ExScheduleCandidate::getVcYield)));
        double avgYield = yieldByHose.values().stream().mapToInt(i -> i).average().orElse(1.0);

        return rows.stream()
            .map(c -> {
                long daysToDeadline = java.time.temporal.ChronoUnit.DAYS.between(
                    today, c.getExtrusionDeadline());
                double slack = clamp(daysToDeadline / (double) horizonDays);
                int hoseTotal = yieldByHose.getOrDefault(c.getHoseId(), c.getVcYield());
                double balance = clamp(1.0 - Math.abs(hoseTotal - avgYield) / (avgYield * 2));
                // setting score — 같은 hose 의 row 수 가 적으면 setting 단일성↑ (간소화 proxy)
                long sameHoseCount = rows.stream()
                    .filter(r -> r.getHoseId().equals(c.getHoseId())).count();
                double setting = clamp(1.0 / sameHoseCount);

                double total = 0.4 * slack + 0.3 * balance + 0.3 * setting;
                return new RankedCandidate(
                    c.getExCandidateId(), c.getHoseId(), c.getExtrusionDeadline(),
                    c.getVcYield(), slack, balance, setting, total);
            })
            .sorted(Comparator.comparingDouble(RankedCandidate::totalScore).reversed())
            .toList();
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
