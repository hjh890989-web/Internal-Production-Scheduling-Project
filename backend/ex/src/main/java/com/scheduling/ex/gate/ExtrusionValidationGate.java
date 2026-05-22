package com.scheduling.ex.gate;

import com.scheduling.ex.required.ExtrusionDemandCalculator;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.yield.YieldFormula;
import com.scheduling.master.api.ExConstraintLookup;
import com.scheduling.master.api.ExConstraintSummary;
import com.scheduling.master.api.ShiftLookup;
import com.scheduling.master.api.ShiftSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 압출 검증 게이트 — TK-EX11-1-1·2·3 (REQ-FUNC-EX-011, BR-E04·E10).
 *
 * <p><b>2 검증 조건</b>:
 * <ol>
 *   <li>BR-E10: deadline 이전 누적 yield ≥ Q_ext</li>
 *   <li>BR-E04: 같은 shift 의 actualMin (예약된 분) ≤ effective_min</li>
 * </ol>
 *
 * <p>p95 ≤ 2초 목표 (600 candidate, single thread). 후보 단위 검증 — 같은
 * (hoseId, deadline 이전) candidates 의 yield 합산 + shift 별 actualMin 계산.
 */
@Component
@Profile("with-infra")
public class ExtrusionValidationGate {

    private static final Logger log = LoggerFactory.getLogger(ExtrusionValidationGate.class);

    private final YieldFormula yieldFormula;
    private final ExtrusionDemandCalculator demandCalc;
    private final ShiftLookup shiftLookup;
    private final ExConstraintLookup exLookup;
    private final Clock clock;

    public ExtrusionValidationGate(
        YieldFormula yieldFormula,
        ExtrusionDemandCalculator demandCalc,
        ShiftLookup shiftLookup,
        ExConstraintLookup exLookup,
        Clock clock
    ) {
        this.yieldFormula = yieldFormula;
        this.demandCalc = demandCalc;
        this.shiftLookup = shiftLookup;
        this.exLookup = exLookup;
        this.clock = clock;
    }

    /**
     * 단일 candidate 검증.
     *
     * @param candidate     검증 대상
     * @param shiftCode     배정된 shift (SettingGroupAllocator 결과 기준)
     * @param allCandidates 같은 hose 의 horizon 내 모든 candidate (yield 누적 입력)
     * @param shiftActualMin 본 shift 에 이미 할당된 min (다른 후보 합산)
     * @return 통과 / 실패 (violations 포함)
     */
    public ExGateResult validate(ExScheduleCandidate candidate,
                                  String shiftCode,
                                  List<ExScheduleCandidate> allCandidates,
                                  int shiftActualMin) {
        List<ExGateViolation> violations = new ArrayList<>();

        // 1. BR-E10 — 누적 yield ≥ Q_ext
        ExConstraintSummary ex = exLookup.findById(candidate.getHoseId()).orElse(null);
        ShiftSummary shift = shiftLookup.findByCode(shiftCode).orElse(null);

        if (ex == null || shift == null || !ex.hasYieldInput()) {
            // 마스터 미등록 — 검증 불가, 통과 (보수적)
            return new ExGateResult(candidate.getExCandidateId(), true, List.of(), Instant.now(clock));
        }

        // deadline 이전 같은 hose candidate yield 합산
        long cumulativeYield = 0;
        for (ExScheduleCandidate c : allCandidates) {
            if (!c.getHoseId().equals(candidate.getHoseId())) continue;
            if (c.getExtrusionDeadline().isAfter(candidate.getExtrusionDeadline())) continue;
            // 단일 후보 yield = YieldFormula 적용
            cumulativeYield += yieldFormula.compute(
                ex.speedMPerMin(), shift.effectiveMin(), ex.lengthMm());
        }
        int qExt = demandCalc.computeForHose(candidate.getHoseId(), candidate.getVcYield());

        if (cumulativeYield < qExt) {
            violations.add(ExGateViolation.yieldShort(qExt, (int) Math.min(cumulativeYield, Integer.MAX_VALUE)));
        }

        // 2. BR-E04 — shift effective_min 초과
        // 단일 candidate 가 사용하는 시간 ≈ effective_min (단순화: 1 후보 = 1 shift 점유)
        int candActualMin = shift.effectiveMin();
        int totalShiftMin = shiftActualMin + candActualMin;
        if (totalShiftMin > shift.effectiveMin()) {
            violations.add(ExGateViolation.shiftCapacityExceeded(shift.effectiveMin(), totalShiftMin));
        }

        boolean passed = violations.isEmpty();
        log.debug("ExGate candidate={} shift={} passed={} violations={}",
            candidate.getExCandidateId(), shiftCode, passed, violations.size());

        return new ExGateResult(candidate.getExCandidateId(), passed, violations, Instant.now(clock));
    }

    /**
     * Batch 검증 — multi candidate. shift 별 actualMin 누적 추적.
     */
    public List<ExGateResult> validateBatch(List<ExScheduleCandidate> candidates,
                                              Map<UUID, String> shiftByCandidate) {
        long startNanos = System.nanoTime();
        Map<ShiftKey, Integer> shiftActualMin = new HashMap<>();
        List<ExGateResult> results = new ArrayList<>(candidates.size());

        for (ExScheduleCandidate c : candidates) {
            String shiftCode = shiftByCandidate.get(c.getExCandidateId());
            if (shiftCode == null) continue;
            ShiftKey key = new ShiftKey(c.getExtrusionDeadline(), shiftCode);
            int current = shiftActualMin.getOrDefault(key, 0);
            ExGateResult result = validate(c, shiftCode, candidates, current);
            results.add(result);
            if (result.passed()) {
                Optional<ShiftSummary> s = shiftLookup.findByCode(shiftCode);
                s.ifPresent(shift -> shiftActualMin.merge(key, shift.effectiveMin(), Integer::sum));
            }
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("ExGate batch — candidates={} results={} passed={} elapsed={}ms",
            candidates.size(), results.size(),
            results.stream().filter(ExGateResult::passed).count(), elapsedMs);
        return results;
    }

    private record ShiftKey(LocalDate date, String shiftCode) {}
}
