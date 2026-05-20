package com.scheduling.order.diff;

/**
 * 단일 필드 before/after — TK-03-1-1.
 *
 * <p>fieldName 표준화 (snake_case, DB 컬럼명 정합):
 * <ul>
 *   <li>{@code hose_id}</li>
 *   <li>{@code delivery_date}</li>
 *   <li>{@code qty}</li>
 *   <li>{@code customer}</li>
 *   <li>{@code order_type}</li>
 * </ul>
 */
public record FieldDiff(
    String fieldName,
    Object before,
    Object after
) {}
