package com.scheduling.master.api;

/**
 * VcMachine 요약 DTO — TK-05-1-1 (cross-module).
 *
 * <p>{@code com.scheduling.master.vc.VcMachine} JPA 엔티티의 immutable 사본 — Modulith
 * 경계 회피용. 다른 모듈 (vc 등) 은 {@link VcMachineQuery} 로 본 record 만 받음.
 *
 * @param machineId         LP-01 ~ LP-04 / IC-01
 * @param machineType       'LP' 또는 'IC' (string — enum cross-module 회피)
 * @param totalSlots        LP=8, IC=6
 * @param dayRotations      BR-V04 주간 (기본 8)
 * @param nightRotations    BR-V04 야간 (기본 10)
 * @param active            정비·교체 시 false
 */
public record VcMachineSummary(
    String machineId,
    String machineType,
    short totalSlots,
    short dayRotations,
    short nightRotations,
    boolean active
) {
    /** BR-V04: 주간 + 야간 = 18 회전/일. */
    public int totalRotationsPerDay() {
        return dayRotations + nightRotations;
    }

    /** 일일 capa = 회전 × slot. LP=144, IC=108. */
    public int dailyCapacity() {
        return totalRotationsPerDay() * totalSlots;
    }

    public boolean isLp() {
        return "LP".equals(machineType);
    }

    public boolean isIc() {
        return "IC".equals(machineType);
    }
}
