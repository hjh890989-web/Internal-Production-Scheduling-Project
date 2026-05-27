package com.scheduling.vc.allocator;

import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.required.OrderInput;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * GreedyRotationAllocator 입력 묶음 — TK-05-3-2.
 *
 * @param qRequired       hose_id → Q_required (TK-05-3-1 결과)
 * @param ordersByHose    hose_id → linkedOrderIds 추적용 (caller 가 groupByHose 결과 전달)
 * @param ledger          CapacityLedger (TK-05-1-2)
 * @param workingDays     호라이즌 영업일 (정렬, 납기 우선 순회)
 * @param requestedBy     Sprint 17 BR-X05 dual-review — INSERT actor (PLANNER 사번 또는 'system')
 */
public record AllocationContext(
    Map<String, Integer> qRequired,
    Map<String, List<OrderInput>> ordersByHose,
    CapacityLedger ledger,
    List<LocalDate> workingDays,
    String requestedBy
) {
    public AllocationContext {
        qRequired = qRequired == null ? Map.of() : Map.copyOf(qRequired);
        ordersByHose = ordersByHose == null ? Map.of() : Map.copyOf(ordersByHose);
        workingDays = workingDays == null ? List.of() : List.copyOf(workingDays);
        if (requestedBy == null || requestedBy.isBlank()) {
            requestedBy = "system";
        }
    }

    /**
     * Backward-compat 4-arg 생성자 — Sprint 17 이전 caller (테스트, ConflictController) 호환.
     * requestedBy 기본값 'system' (VcSchedule.createdBy NOT NULL legacy fallback).
     */
    public AllocationContext(
        Map<String, Integer> qRequired,
        Map<String, List<OrderInput>> ordersByHose,
        CapacityLedger ledger,
        List<LocalDate> workingDays
    ) {
        this(qRequired, ordersByHose, ledger, workingDays, "system");
    }
}
