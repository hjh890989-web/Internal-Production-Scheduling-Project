package com.scheduling.master.api;

import java.util.List;
import java.util.Optional;

/**
 * 압출 셋팅 그룹 facade — TK-09-1-1 (Modulith cross-module).
 *
 * <p>ex 모듈 {@code SettingGroupAllocator} 가 본 인터페이스로 hose ↔ group 조회 +
 * 호환 hose 리스트 (BR-E07 같은 그룹 동시 생산 후보) 조회.
 */
public interface SettingGroupLookup {

    /** 셋팅 그룹 1~8 전체 (sort by group_number ASC). */
    List<SettingGroupSummary> findAllGroups();

    /** 본 hose 의 호환 그룹 목록 (primary 먼저). */
    List<HoseSettingGroupSummary> findGroupsForHose(String hoseId);

    /** 본 hose 의 primary 그룹 (없으면 첫 secondary). */
    Optional<HoseSettingGroupSummary> findPrimaryGroup(String hoseId);

    /** 본 그룹과 호환되는 모든 hose. */
    List<HoseSettingGroupSummary> findHosesInGroup(short groupNumber);
}
