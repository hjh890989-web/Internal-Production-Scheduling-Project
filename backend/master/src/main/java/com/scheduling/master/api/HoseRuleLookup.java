package com.scheduling.master.api;

import java.util.List;
import java.util.Optional;

/**
 * 품번 운영 룰 facade — TK-21-2-1 (Modulith cross-module).
 *
 * <p>vc 모듈 RuleEngine 이 본 인터페이스로 hose-level 운영 룰 조회.
 *
 * @see com.scheduling.master.vc.HoseRuleLookupImpl
 */
public interface HoseRuleLookup {

    Optional<VcHoseRuleSummary> findById(String hoseId);

    List<VcHoseRuleSummary> findAll();

    /** 캐시 무효화 — LISTEN/NOTIFY 또는 HoseRuleController 변경 후 호출. */
    void invalidate(String hoseId);

    void invalidateAll();
}
