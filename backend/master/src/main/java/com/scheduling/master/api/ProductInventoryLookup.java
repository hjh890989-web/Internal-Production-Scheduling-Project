package com.scheduling.master.api;

import java.util.Optional;

/**
 * 품번 재고 facade — TK-08-3-1 (ProductInventory cross-module).
 *
 * <p>ex 모듈 {@code ExtrusionDemandCalculator} 가 본 인터페이스로 target/current 조회.
 */
public interface ProductInventoryLookup {

    Optional<ProductInventorySummary> findById(String hoseId);
}
