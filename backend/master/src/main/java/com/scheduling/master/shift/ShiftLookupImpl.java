package com.scheduling.master.shift;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scheduling.master.api.ShiftLookup;
import com.scheduling.master.api.ShiftSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * {@link ShiftLookup} 구현 — TK-08-1-1.
 *
 * <p>Caffeine 캐시 (maximumSize 20, 1h expireAfterWrite). LISTEN/NOTIFY
 * {@code shift_changed} 알림 도착 시 invalidate.
 */
@Component
@Profile("with-infra")
class ShiftLookupImpl implements ShiftLookup {

    private final ShiftRepository repository;
    private final Cache<String, ShiftSummary> cache;

    ShiftLookupImpl(ShiftRepository repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
            .maximumSize(20)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();
    }

    @Override
    public Optional<ShiftSummary> findByCode(String shiftCode) {
        ShiftSummary cached = cache.getIfPresent(shiftCode);
        if (cached != null) return Optional.of(cached);
        return repository.findById(shiftCode).map(s -> {
            ShiftSummary summary = toSummary(s);
            cache.put(shiftCode, summary);
            return summary;
        });
    }

    @Override
    public List<ShiftSummary> findAll() {
        return repository.findAllByOrderBySortOrderAsc().stream()
            .map(ShiftLookupImpl::toSummary).toList();
    }

    @Override
    public void invalidate(String shiftCode) {
        cache.invalidate(shiftCode);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    private static ShiftSummary toSummary(Shift s) {
        return new ShiftSummary(s.getShiftCode(), s.getName(),
            s.getStartTime(), s.getEndTime(), s.getNominalMin(),
            s.getEfficiency(), s.getEffectiveMin(), s.getSortOrder());
    }
}
