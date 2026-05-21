package com.scheduling.vc.yield;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 47품번 yield 매트릭스 (immutable) — TK-05-2-1 (BR-V03).
 *
 * <p>구조:
 * <ul>
 *   <li>{@code lpYields} — hose_id → LP yield/rotation (composite × lp_molds_per_angle)</li>
 *   <li>{@code icYields} — hose_id → IC yield/rotation (composite × ic_molds_per_angle)</li>
 *   <li>{@code unschedulableYields} — 양쪽 모두 0 인 품번 (BR-V11 정합)</li>
 * </ul>
 *
 * <p>{@link #lookup} → Optional. 한 가지 머신만 가능한 품번 — 다른 머신 lookup = empty.
 *
 * @param version              매트릭스 버전 (monotonic counter)
 * @param builtAt              빌드 시각 (KST — BR-X04)
 */
public record YieldMatrix(
    int version,
    Instant builtAt,
    Map<String, Integer> lpYields,
    Map<String, Integer> icYields,
    Set<String> unschedulableYields
) {
    public YieldMatrix {
        lpYields = lpYields == null ? Map.of() : Map.copyOf(lpYields);
        icYields = icYields == null ? Map.of() : Map.copyOf(icYields);
        unschedulableYields = unschedulableYields == null ? Set.of() : Set.copyOf(unschedulableYields);
    }

    /** LP / IC yield/rotation 조회. 미정의 (0) → Optional.empty. */
    public Optional<Integer> lookup(String hoseId, String machineType) {
        Map<String, Integer> map = "LP".equals(machineType) ? lpYields : icYields;
        return Optional.ofNullable(map.get(hoseId));
    }

    public boolean isUnschedulable(String hoseId) {
        return unschedulableYields.contains(hoseId);
    }
}
