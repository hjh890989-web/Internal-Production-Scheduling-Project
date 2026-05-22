package com.scheduling.master.api;

import java.util.List;
import java.util.Optional;

/**
 * Cross-master ProductSpec facade — TK-21-5-1 (ADR-017).
 *
 * <p>vc 모듈 SpecLt7CapRule 이 본 인터페이스로 spec / angleCount / isSpecLt7 조회.
 *
 * @see com.scheduling.master.spec.ProductSpecCache
 */
public interface ProductSpecLookup {

    Optional<ProductSpecSummary> findById(String hoseId);

    List<ProductSpecSummary> findAllSpecLt7();

    void invalidate(String hoseId);

    void invalidateAll();
}
