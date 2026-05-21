package com.scheduling.master.vc;

import java.util.List;

/**
 * Unschedulable 분리 결과 — TK-04-2-1.
 *
 * <p>{@link UnschedulableFilterService#separate} 의 immutable 결과.
 * caller (EP-05 회전 배치 등) 는 {@link #schedulable} 만 후속 단계로 전달.
 *
 * @param schedulable    BR-V11 통과 hose_id (≥ 1 슬롯 가능)
 * @param unschedulable  BR-V11 zero-slot hose_id — 외주·재고 대응 권고 (ASM-10)
 * @param matrixVersion  분리 당시 매트릭스 버전 (audit 정합)
 */
public record FilterResult(
    List<String> schedulable,
    List<String> unschedulable,
    int matrixVersion
) {
    public FilterResult {
        schedulable = schedulable == null ? List.of() : List.copyOf(schedulable);
        unschedulable = unschedulable == null ? List.of() : List.copyOf(unschedulable);
    }

    public int unschedulableCount() {
        return unschedulable.size();
    }

    public boolean hasUnschedulable() {
        return !unschedulable.isEmpty();
    }
}
