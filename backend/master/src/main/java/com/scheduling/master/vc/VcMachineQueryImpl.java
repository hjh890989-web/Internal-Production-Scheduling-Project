package com.scheduling.master.vc;

import com.scheduling.master.api.VcMachineQuery;
import com.scheduling.master.api.VcMachineSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link VcMachineQuery} 기본 구현 — TK-05-1-1.
 *
 * <p>{@link VcMachineRepository} 를 {@link VcMachineSummary} 로 변환 (cross-module DTO).
 * vc 모듈은 {@link VcMachineQuery} (master.api) 만 import 가능 — Modulith verify 통과.
 *
 * <p>{@code @Profile("with-infra")} — JPA Repository 의존.
 */
@Component
@Profile("with-infra")
class VcMachineQueryImpl implements VcMachineQuery {

    private final VcMachineRepository repository;

    VcMachineQueryImpl(VcMachineRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<VcMachineSummary> findAllActive() {
        return repository.findByActive(true).stream().map(VcMachineQueryImpl::toSummary).toList();
    }

    @Override
    public Optional<VcMachineSummary> findById(String machineId) {
        return repository.findById(machineId).map(VcMachineQueryImpl::toSummary);
    }

    private static VcMachineSummary toSummary(VcMachine m) {
        return new VcMachineSummary(
            m.getMachineId(),
            m.getMachineType().name(),
            m.getTotalSlots(),
            m.getDayRotations(),
            m.getNightRotations(),
            m.isActive()
        );
    }
}
