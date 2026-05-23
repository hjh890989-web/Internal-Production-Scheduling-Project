package com.scheduling.master.priority;

import com.scheduling.master.api.ProductPriorityLookup;
import com.scheduling.master.api.ProductPrioritySummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("with-infra")
class ProductPriorityLookupImpl implements ProductPriorityLookup {

    private final ProductPriorityRepository repository;

    ProductPriorityLookupImpl(ProductPriorityRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductPrioritySummary> findEffectiveAt(LocalDate at) {
        return repository.findEffectiveAt(at).stream()
            .map(p -> new ProductPrioritySummary(
                p.getHoseId(), p.getPriorityRank(), p.getRationale(),
                p.getEffectiveFrom(), p.getEffectiveTo()))
            .toList();
    }
}
