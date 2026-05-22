package com.scheduling.vc.conflict;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 충돌 리포트 — TK-VC15-1-3 (REQ-FUNC-VC-015).
 *
 * <p>UI Conflict View 데이터 소스. items[].alternatives 는 ≥ 3 distinct 보장.
 *
 * @param items       카테고리화 + 대안 enrich 된 conflict 목록
 * @param summary     카테고리별 카운트 (UI 1차 그루핑)
 * @param totalItems  총 conflict 수
 * @param generatedAt 생성 시각 (UTC)
 */
public record ConflictReport(
    List<ConflictReportItem> items,
    Map<ConflictCategory, Long> summary,
    int totalItems,
    Instant generatedAt
) {
    public ConflictReport {
        items = items == null ? List.of() : List.copyOf(items);
        summary = summary == null ? Map.of() : Map.copyOf(summary);
    }
}
