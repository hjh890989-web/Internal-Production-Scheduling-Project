package com.scheduling.vc.conflict;

import java.util.List;

/**
 * 충돌 리포트 단일 항목 — TK-VC15-1-3.
 */
public record ConflictReportItem(
    String hoseId,
    ConflictCategory category,
    String reason,
    int targetQty,
    int placedQty,
    List<Alternative> alternatives
) {
    public ConflictReportItem {
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
    }
}
