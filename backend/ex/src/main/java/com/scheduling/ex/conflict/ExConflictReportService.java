package com.scheduling.ex.conflict;

import com.scheduling.ex.gate.ExGateResult;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 압출 충돌 리포트 빌더 — TK-EX12-1-2 (REQ-FUNC-EX-012).
 *
 * <p>{@link ExConflictCategorizer} 분류 + {@link ExAlternativeGenerator} 대안 enrich 통합.
 * BR-X04 — Clock 주입.
 */
@Service
public class ExConflictReportService {

    private final ExConflictCategorizer categorizer;
    private final ExAlternativeGenerator generator;
    private final Clock clock;

    public ExConflictReportService(ExConflictCategorizer categorizer,
                                    ExAlternativeGenerator generator,
                                    Clock clock) {
        this.categorizer = categorizer;
        this.generator = generator;
        this.clock = clock;
    }

    /**
     * Gate 결과 + hose 매핑 → 충돌 리포트.
     *
     * @param results       검증 게이트 결과 (실패만 포함될 수도, 전체 포함될 수도)
     * @param hoseByCandidate candidateId → hoseId 매핑 (UI 표시용)
     */
    public ExConflictReport buildReport(List<ExGateResult> results,
                                          Map<java.util.UUID, String> hoseByCandidate) {
        if (results == null) results = List.of();
        if (hoseByCandidate == null) hoseByCandidate = Map.of();

        List<ExConflictReportItem> items = new ArrayList<>();
        List<ExClassifiedConflict> allClassified = new ArrayList<>();

        for (ExGateResult r : results) {
            if (r.passed()) continue;     // pass 는 리포트 미포함
            String hose = hoseByCandidate.getOrDefault(r.candidateId(), "UNKNOWN");
            List<ExClassifiedConflict> classified = categorizer.classify(r, hose);
            allClassified.addAll(classified);

            for (ExClassifiedConflict cc : classified) {
                items.add(new ExConflictReportItem(
                    cc.candidateId(), cc.hoseId(), cc.category(),
                    cc.violation().reason(),
                    cc.violation().targetQty(),
                    cc.violation().actual(),
                    generator.generate(cc)));
            }
        }

        Map<ExConflictCategory, Long> summary = categorizer.countByCategory(allClassified);
        return new ExConflictReport(items, summary, items.size(), Instant.now(clock));
    }

    /**
     * Convenience overload — hoseByCandidate 없는 경우 (hose 정보 UNKNOWN 으로 표시).
     */
    public ExConflictReport buildReport(List<ExGateResult> results) {
        return buildReport(results, new HashMap<>());
    }
}
