package com.scheduling.master.api;

/**
 * 47품번 ↔ 셋팅 그룹 매핑 요약 — TK-09-1-1 (cross-module).
 *
 * @param hoseId        품번
 * @param groupNumber   셋팅 그룹 1~8
 * @param primaryGroup  우선 추천 (TRUE) vs secondary (FALSE)
 */
public record HoseSettingGroupSummary(
    String hoseId,
    short groupNumber,
    boolean primaryGroup
) {}
