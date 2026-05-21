package com.scheduling.vc.allocator;

import com.scheduling.vc.yield.AngleCapacityViolation;

import java.time.LocalDate;

/**
 * Greedy 배치 실패 사유 — TK-05-3-2 / TK-06-1-2.
 *
 * <p>4 카테고리:
 * <ul>
 *   <li>UNSCHEDULABLE — matrix.unschedulableHoseIds 포함 (BR-V11)</li>
 *   <li>INSUFFICIENT_CAPACITY — 호라이즌 내 가용 슬롯 부족 (Q_required 미달)</li>
 *   <li>ANGLE_VIOLATION — 사후 검증에서 발견된 BR-V06 위반</li>
 *   <li>DEADLINE_EXCEEDED — D-2 deadline 내 capa 부족 (BR-X07)</li>
 * </ul>
 *
 * @param hoseId       품번
 * @param category     UNSCHEDULABLE / INSUFFICIENT_CAPACITY / ANGLE_VIOLATION
 * @param reason       한국어 사유
 * @param targetQty    Q_required (0 = 적용 안 됨)
 * @param placedQty    누적 yield (0 = 적용 안 됨)
 */
public record AllocationConflict(
    String hoseId,
    Category category,
    String reason,
    int targetQty,
    int placedQty
) {
    public enum Category {
        UNSCHEDULABLE,
        INSUFFICIENT_CAPACITY,
        ANGLE_VIOLATION,
        DEADLINE_EXCEEDED,
        LEFT_RIGHT_VIOLATION
    }

    public static AllocationConflict unschedulable(String hose, int target) {
        return new AllocationConflict(hose, Category.UNSCHEDULABLE,
            "UNSCHEDULABLE — 모든 슬롯 X (BR-V11) — 외주·재고 대응 권고", target, 0);
    }

    public static AllocationConflict insufficientCapacity(String hose, int target, int placed, int slotsUsed) {
        return new AllocationConflict(hose, Category.INSUFFICIENT_CAPACITY,
            "용량 부족 — Q_required %d 충족 못 함, %d 슬롯 사용 후 %d 누적 yield".formatted(target, slotsUsed, placed),
            target, placed);
    }

    public static AllocationConflict fromAngleViolation(AngleCapacityViolation v) {
        return new AllocationConflict(v.hoseId(), Category.ANGLE_VIOLATION,
            v.userMessage(), 0, 0);
    }

    /** BR-X07 — D-2 deadline 내 capa 부족 (TK-06-1-2). */
    public static AllocationConflict deadlineExceeded(String hose, int target, int placed, LocalDate deadline) {
        return new AllocationConflict(hose, Category.DEADLINE_EXCEEDED,
            "납기 D-2 deadline %s 이내 %d 필요량 중 %d 만 배치 (BR-X07)".formatted(deadline, target, placed),
            target, placed);
    }

    /** BR-V15·V16 — LP 좌/우 셋팅 미충족 (TK-21-1-2). */
    public static AllocationConflict leftRightViolation(String hose, int target, int placed) {
        return new AllocationConflict(hose, Category.LEFT_RIGHT_VIOLATION,
            "LP 좌/우 셋팅 미충족 — %d 필요량 중 %d 만 배치 (BR-V15·V16)".formatted(target, placed),
            target, placed);
    }
}
