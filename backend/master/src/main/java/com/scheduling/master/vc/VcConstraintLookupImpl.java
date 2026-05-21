package com.scheduling.master.vc;

import com.scheduling.master.api.VcConstraintLookup;
import com.scheduling.master.api.VcConstraintSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link VcConstraintLookup} 기본 구현 — TK-05-2-1.
 *
 * <p>{@link VcConstraintRepository} → {@link VcConstraintSummary} 변환. vc 모듈은
 * master.api.* 만 import — Modulith verify 통과.
 *
 * <p>{@code @Profile("with-infra")} — JPA Repository 의존.
 */
@Component
@Profile("with-infra")
class VcConstraintLookupImpl implements VcConstraintLookup {

    private final VcConstraintRepository repository;

    VcConstraintLookupImpl(VcConstraintRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<VcConstraintSummary> findAll() {
        return repository.findAll().stream().map(VcConstraintLookupImpl::toSummary).toList();
    }

    @Override
    public Optional<VcConstraintSummary> findById(String hoseId) {
        return repository.findById(hoseId).map(VcConstraintLookupImpl::toSummary);
    }

    private static VcConstraintSummary toSummary(VcConstraint c) {
        return new VcConstraintSummary(
            c.getHoseId(),
            c.getCompositeCount(),
            c.getLpMoldsPerAngle(),
            c.getLpAngleQty(),
            c.getIcMoldsPerAngle(),
            c.getIcAngleQty(),
            c.getLpLeftSetting(),
            c.getLpRightSetting()
        );
    }
}
