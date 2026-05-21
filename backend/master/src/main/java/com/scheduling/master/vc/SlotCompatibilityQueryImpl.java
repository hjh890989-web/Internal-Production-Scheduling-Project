package com.scheduling.master.vc;

import com.scheduling.master.api.SlotCompatibilityQuery;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * {@link SlotCompatibilityQuery} 기본 구현 — TK-05-3-2.
 *
 * <p>{@link SlotCompatibilityMatrixService} 를 cross-module facade 형태로 노출.
 * vc 모듈은 master.api.* 만 import — Modulith verify 통과.
 *
 * <p>{@code @Profile("with-infra")} — JPA 의존.
 */
@Component
@Profile("with-infra")
class SlotCompatibilityQueryImpl implements SlotCompatibilityQuery {

    private final SlotCompatibilityMatrixService matrixService;

    SlotCompatibilityQueryImpl(SlotCompatibilityMatrixService matrixService) {
        this.matrixService = matrixService;
    }

    @Override
    public boolean isEligible(String hoseId, String slotPosition) {
        SlotCompatibilityMatrix matrix = matrixService.current();
        if (matrix == null) return false;
        try {
            return matrix.isEligible(hoseId, SlotPosition.valueOf(slotPosition));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Set<String> unschedulableHoseIds() {
        SlotCompatibilityMatrix matrix = matrixService.current();
        return matrix == null ? Set.of() : matrix.unschedulableHoseIds();
    }

    @Override
    public int currentVersion() {
        SlotCompatibilityMatrix matrix = matrixService.current();
        return matrix == null ? 0 : matrix.version();
    }
}
