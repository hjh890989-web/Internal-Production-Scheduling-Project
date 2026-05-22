package com.scheduling.master.api;

import java.util.List;

/**
 * 압출 라인 라우팅 facade — TK-14-1-1·2 (Modulith cross-module, BR-E08).
 *
 * <p>ex 모듈 {@code ExLineRoutingPolicy} 가 본 인터페이스로 라인 우선순위 + 포드 전용
 * 품번 차단 조회.
 */
public interface LineRoutingLookup {

    /** 활성 라인 priority ASC 정렬 (NEW 우선 → FORD fallback). */
    List<LineTypeSummary> findAllActive();

    /** 본 hose 가 포드 전용 (신규 라인 시도 차단) 인가. */
    boolean isFordOnly(String hoseId);

    /** 본 hose 호환 라인 IDs. (호환 미등록 hose → 빈 list, 모든 라인 자유). */
    List<String> findCompatibleLineIds(String hoseId);
}
