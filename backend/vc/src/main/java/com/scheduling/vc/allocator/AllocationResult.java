package com.scheduling.vc.allocator;

import com.scheduling.vc.domain.VcSchedule;

import java.util.List;

/**
 * GreedyRotationAllocator 결과 — TK-05-3-2.
 *
 * @param schedules     생성된 VcSchedule Candidate 목록 (BR-X01 사용자 확정 전)
 * @param conflicts     Unschedulable / 용량 부족 / 앵글 위반 사유
 */
public record AllocationResult(
    List<VcSchedule> schedules,
    List<AllocationConflict> conflicts
) {
    public AllocationResult {
        schedules = schedules == null ? List.of() : List.copyOf(schedules);
        conflicts = conflicts == null ? List.of() : List.copyOf(conflicts);
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    public int scheduleCount() {
        return schedules.size();
    }
}
