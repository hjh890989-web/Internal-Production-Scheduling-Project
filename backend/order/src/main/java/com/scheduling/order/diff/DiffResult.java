package com.scheduling.order.diff;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 마스터 버전 간 diff 산출 — TK-03-1-1.
 *
 * @param trackingId       import 추적 ID (TK-01-1-3 OrderImportController 발급)
 * @param previousVersion  이전 master_version (없으면 0)
 * @param newVersion       신규 master_version (= previousVersion + 1)
 * @param computedAt       diff 계산 시각 (KST instant, BR-X04)
 * @param rows             분류된 RowDiff 전체 (UNCHANGED 포함)
 */
public record DiffResult(
    UUID trackingId,
    int previousVersion,
    int newVersion,
    Instant computedAt,
    List<RowDiff> rows
) {
    public DiffResult {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public int countByType(DiffType type) {
        int count = 0;
        for (RowDiff row : rows) {
            if (row.type() == type) count++;
        }
        return count;
    }

    public int totalChanges() {
        return rows.size() - countByType(DiffType.UNCHANGED);
    }

    public List<RowDiff> rowsOfType(DiffType type) {
        return rows.stream().filter(r -> r.type() == type).toList();
    }
}
