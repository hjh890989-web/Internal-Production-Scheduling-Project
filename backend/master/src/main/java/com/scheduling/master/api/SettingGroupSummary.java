package com.scheduling.master.api;

/**
 * 셋팅 그룹 요약 — TK-09-1-1 (cross-module).
 */
public record SettingGroupSummary(
    short groupNumber,
    String groupName,
    String description
) {}
