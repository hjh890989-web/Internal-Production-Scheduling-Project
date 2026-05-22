package com.scheduling.master.inventory;

import com.scheduling.master.api.ProductInventoryLookup;
import com.scheduling.master.api.ProductInventorySummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link ProductInventoryLookup} 구현 — TK-08-3-1.
 *
 * <p>Phase 2+ MES 동기화 시 캐시 도입 — Sprint 3 는 단순 repository pass-through.
 */
@Component
@Profile("with-infra")
class ProductInventoryLookupImpl implements ProductInventoryLookup {

    private final ProductInventoryRepository repository;

    ProductInventoryLookupImpl(ProductInventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ProductInventorySummary> findById(String hoseId) {
        return repository.findById(hoseId)
            .map(p -> new ProductInventorySummary(
                p.getHoseId(), p.getTargetStock(), p.getCurrentStock()));
    }
}
