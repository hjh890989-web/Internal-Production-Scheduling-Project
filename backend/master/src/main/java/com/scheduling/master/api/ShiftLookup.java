package com.scheduling.master.api;

import java.util.List;
import java.util.Optional;

/**
 * 압출 Shift 마스터 facade — TK-08-1-1 (Modulith cross-module).
 *
 * <p>ex 모듈이 본 인터페이스로 effective_min 조회 (BR-E05 yield 수식 입력).
 *
 * @see com.scheduling.master.shift.ShiftLookupImpl
 */
public interface ShiftLookup {

    Optional<ShiftSummary> findByCode(String shiftCode);

    /** sort_order ASC 정렬된 전체 shift. */
    List<ShiftSummary> findAll();

    void invalidate(String shiftCode);

    void invalidateAll();
}
