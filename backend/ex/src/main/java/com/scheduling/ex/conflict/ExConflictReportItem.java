package com.scheduling.ex.conflict;

import java.util.List;
import java.util.UUID;

/**
 * 압출 충돌 리포트 단일 항목 — TK-EX12-1-2.
 */
public record ExConflictReportItem(
    UUID candidateId,
    String hoseId,
    ExConflictCategory category,
    String reason,
    int targetQty,
    int actual,
    List<ExAlternative> alternatives
) {
    public ExConflictReportItem {
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
    }
}
