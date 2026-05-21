package com.scheduling.master.vc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scheduling.master.api.HoseRuleLookup;
import com.scheduling.master.api.VcHoseRuleSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * {@link HoseRuleLookup} 구현 — TK-21-2-1·2-3.
 *
 * <p>Caffeine 캐시 (maximumSize 500, expireAfterWrite 1h 안전망). LISTEN/NOTIFY 알림
 * 도착 시 {@link #invalidate} / {@link #invalidateAll} 호출.
 */
@Component
@Profile("with-infra")
class HoseRuleLookupImpl implements HoseRuleLookup {

    private final VcHoseRuleRepository repository;
    private final Cache<String, VcHoseRuleSummary> cache;

    HoseRuleLookupImpl(VcHoseRuleRepository repository) {
        this.repository = repository;
        this.cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();
    }

    @Override
    public Optional<VcHoseRuleSummary> findById(String hoseId) {
        VcHoseRuleSummary cached = cache.getIfPresent(hoseId);
        if (cached != null) return Optional.of(cached);
        return repository.findById(hoseId).map(rule -> {
            VcHoseRuleSummary s = toSummary(rule);
            cache.put(hoseId, s);
            return s;
        });
    }

    @Override
    public List<VcHoseRuleSummary> findAll() {
        return repository.findAll().stream().map(HoseRuleLookupImpl::toSummary).toList();
    }

    @Override
    public void invalidate(String hoseId) {
        cache.invalidate(hoseId);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    private static VcHoseRuleSummary toSummary(VcHoseRule r) {
        return new VcHoseRuleSummary(
            r.getHoseId(), r.getMachinePin(), r.getMaxConcurrentSlots(),
            r.getSideLock(), r.isLpOnly());
    }
}
