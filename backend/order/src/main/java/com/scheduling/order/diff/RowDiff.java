package com.scheduling.order.diff;

import com.scheduling.order.domain.Order;
import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.domain.OrderKey;

import java.util.List;

/**
 * row 단위 diff — TK-03-1-1.
 *
 * @param key         복합 키 (hose_id, delivery_date)
 * @param type        분류
 * @param newRow      NEW·MODIFIED·UNCHANGED 시 not null, DELETED 시 null
 * @param oldRow      DELETED·MODIFIED·UNCHANGED 시 not null, NEW 시 null
 * @param fieldDiffs  MODIFIED 시 필드별 차이 (그 외 빈 리스트)
 */
public record RowDiff(
    OrderKey key,
    DiffType type,
    OrderDraft newRow,
    Order oldRow,
    List<FieldDiff> fieldDiffs
) {
    public RowDiff {
        fieldDiffs = fieldDiffs == null ? List.of() : List.copyOf(fieldDiffs);
    }
}
