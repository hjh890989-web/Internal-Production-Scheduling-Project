package com.scheduling.order.diff;

/**
 * RowDiff Critical 분류 — TK-03-2-1 (BR-O02 / REQ-FUNC-OC-008).
 *
 * <p>{@link #CRITICAL} — 알림 즉시 발송 (납기 변경·수량 ±20%·품번 변경·NEW·DELETED).
 * False Negative 0 정신 — 의심 시 항상 CRITICAL (conservative).
 *
 * <p>{@link #NORMAL} — customer / order_type 변경 등 운영 영향 적은 항목만 NORMAL.
 */
public enum Severity {
    CRITICAL,
    NORMAL
}
