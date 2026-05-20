package com.scheduling.order.diff;

/**
 * row-level diff 분류 — TK-03-1-1 (REQ-FUNC-OC-007).
 *
 * <ul>
 *   <li>{@link #NEW} — 신규 마스터에만 존재 (이전 버전 없음)</li>
 *   <li>{@link #MODIFIED} — 양쪽 존재 + 필드 ≥1 차이</li>
 *   <li>{@link #DELETED} — 이전 마스터에만 존재 (신규 셋에서 누락)</li>
 *   <li>{@link #UNCHANGED} — 양쪽 존재 + 모든 필드 동일 (audit/통계 정합용)</li>
 * </ul>
 */
public enum DiffType {
    NEW,
    MODIFIED,
    DELETED,
    UNCHANGED
}
