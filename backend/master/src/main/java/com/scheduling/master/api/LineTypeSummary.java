package com.scheduling.master.api;

/**
 * 라인 타입 요약 — TK-14-1-1 (cross-module).
 */
public record LineTypeSummary(
    String lineId,
    String lineType,
    short priority,
    boolean active,
    String description
) {
    public boolean isNew() { return "NEW".equals(lineType); }
    public boolean isFord() { return "FORD".equals(lineType); }
}
