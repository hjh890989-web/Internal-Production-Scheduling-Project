package com.scheduling.master.spec;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scheduling.master.api.ProductSpecLookup;
import com.scheduling.master.api.ProductSpecSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * ProductSpec cross-master 캐시 — TK-21-5-2 (ADR-017).
 *
 * <p>Caffeine maximumSize 500 + 1h expireAfterWrite (안전망). LISTEN/NOTIFY 알림 도착 시
 * {@link #invalidate} / {@link #invalidateAll} 호출.
 */
@Component
@Profile("with-infra")
class ProductSpecCache implements ProductSpecLookup {

    private final ProductSpecRepository repository;
    private final Cache<String, ProductSpecSummary> cache;

    ProductSpecCache(ProductSpecRepository repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();
    }

    @Override
    public Optional<ProductSpecSummary> findById(String hoseId) {
        ProductSpecSummary cached = cache.getIfPresent(hoseId);
        if (cached != null) return Optional.of(cached);
        return repository.findById(hoseId).map(spec -> {
            ProductSpecSummary s = toSummary(spec);
            cache.put(hoseId, s);
            return s;
        });
    }

    @Override
    public List<ProductSpecSummary> findAllSpecLt7() {
        return repository.findByIsSpecLt7True().stream()
            .map(ProductSpecCache::toSummary).toList();
    }

    @Override
    public void invalidate(String hoseId) {
        cache.invalidate(hoseId);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    private static ProductSpecSummary toSummary(ProductSpec p) {
        return new ProductSpecSummary(p.getHoseId(), p.getSpec(),
            p.getAngleCount(), p.isSpecLt7());
    }
}
