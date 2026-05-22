package com.scheduling.ex.conflict;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 압출 충돌 리포트 — TK-EX12-1-2 (REQ-FUNC-EX-012).
 *
 * <p>UI Conflict View 데이터 소스. items[].alternatives 는 ≥ 3 distinct 보장.
 */
public record ExConflictReport(
    List<ExConflictReportItem> items,
    Map<ExConflictCategory, Long> summary,
    int totalItems,
    Instant generatedAt
) {
    public ExConflictReport {
        items = items == null ? List.of() : List.copyOf(items);
        summary = summary == null ? Map.of() : Map.copyOf(summary);
    }
}
