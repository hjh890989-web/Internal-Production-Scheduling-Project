package com.scheduling.master.ex;

import com.scheduling.master.api.ExConstraintLookup;
import com.scheduling.master.api.ExConstraintSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link ExConstraintLookup} 구현 — TK-08-2-1.
 *
 * <p>Phase 2+ Caffeine 캐시 + LISTEN/NOTIFY 도입 — Sprint 3 는 단순 pass-through.
 */
@Component
@Profile("with-infra")
class ExConstraintLookupImpl implements ExConstraintLookup {

    private final ExConstraintRepository repository;

    ExConstraintLookupImpl(ExConstraintRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ExConstraintSummary> findById(String hoseId) {
        return repository.findById(hoseId).map(c -> new ExConstraintSummary(
            c.getHoseId(), c.getSpecValue(), c.getAngleCount(),
            c.getSpeedMPerMin(), c.getLengthMm(), c.getDieCode(), c.getLineCode()));
    }
}
