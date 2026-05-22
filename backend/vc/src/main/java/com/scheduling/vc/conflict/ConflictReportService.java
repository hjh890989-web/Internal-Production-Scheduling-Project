package com.scheduling.vc.conflict;

import com.scheduling.vc.allocator.AllocationConflict;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 충돌 리포트 빌더 — TK-VC15-1-3 (REQ-FUNC-VC-015).
 *
 * <p>{@link ConflictCategorizer} 분류 + {@link AlternativeGenerator} 대안 enrich 통합.
 * BR-X04 — Clock 주입.
 */
@Service
public class ConflictReportService {

    private final ConflictCategorizer categorizer;
    private final AlternativeGenerator generator;
    private final Clock clock;

    public ConflictReportService(ConflictCategorizer categorizer,
                                  AlternativeGenerator generator,
                                  Clock clock) {
        this.categorizer = categorizer;
        this.generator = generator;
        this.clock = clock;
    }

    public ConflictReport buildReport(List<AllocationConflict> conflicts) {
        if (conflicts == null) conflicts = List.of();

        List<ClassifiedConflict> classified = categorizer.classifyAll(conflicts);
        List<ConflictReportItem> items = classified.stream()
            .map(cc -> new ConflictReportItem(
                cc.hoseId(), cc.category(), cc.reason(),
                cc.conflict().targetQty(), cc.conflict().placedQty(),
                generator.generate(cc)))
            .toList();

        Map<ConflictCategory, Long> summary = categorizer.countByCategory(conflicts);

        return new ConflictReport(items, summary, items.size(), Instant.now(clock));
    }
}
