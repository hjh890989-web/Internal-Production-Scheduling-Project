package com.scheduling.master.api;

import java.util.List;
import java.util.Optional;

/**
 * VcMachine 조회 facade — TK-05-1-1 (Modulith 경계).
 *
 * <p>{@code com.scheduling.master.vc.VcMachineRepository} 는 master 모듈 내부.
 * 다른 모듈 (vc) 은 본 인터페이스만 사용 (allowedDependencies = master::api).
 *
 * <p>구현체: {@code com.scheduling.master.vc.VcMachineQueryImpl}.
 */
public interface VcMachineQuery {

    /** active 가류기 전체 — CapacityLedgerBuilder hot path. */
    List<VcMachineSummary> findAllActive();

    Optional<VcMachineSummary> findById(String machineId);
}
