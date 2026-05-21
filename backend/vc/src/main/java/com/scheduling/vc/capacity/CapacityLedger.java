package com.scheduling.vc.capacity;

import com.scheduling.vc.domain.RotationSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 회전 격자 가용 상태 immutable snapshot — TK-05-1-2 (BR-V05).
 *
 * <p>{@link CapacityLedgerBuilder} 가 생성. greedy 배치 알고리즘 (TK-05-3-2) 의 입력.
 *
 * <p>구조:
 * <ul>
 *   <li>1일 격자 — LP 4대 × 18 회전 × 8 슬롯 = 576 셀 + IC 1대 × 18 × 6 = 108 셀 = <b>684 셀/일</b></li>
 *   <li>BR-V05 일일 저압 회전 capa = 4대 × 18 = <b>72 회전</b> ({@link #countLpRotationsAvailable})</li>
 * </ul>
 *
 * @param fromDate 호라이즌 시작일 (inclusive)
 * @param toDate   호라이즌 종료일 (inclusive)
 * @param cells    RotationSlot → SlotAvailability (immutable)
 */
public record CapacityLedger(
    LocalDate fromDate,
    LocalDate toDate,
    Map<RotationSlot, SlotAvailability> cells
) {
    public CapacityLedger {
        cells = cells == null ? Map.of() : Map.copyOf(cells);
    }

    /** key 조회 — 미존재 시 UNAVAILABLE (방어). */
    public SlotAvailability check(RotationSlot slot) {
        return cells.getOrDefault(slot, SlotAvailability.UNAVAILABLE);
    }

    /** 특정 머신·일자의 AVAILABLE 슬롯 목록 — greedy 배치 입력. */
    public List<RotationSlot> findAvailableForMachineOnDate(LocalDate date, String machineId) {
        return cells.entrySet().stream()
            .filter(e -> e.getKey().date().equals(date))
            .filter(e -> e.getKey().machineId().equals(machineId))
            .filter(e -> e.getValue() == SlotAvailability.AVAILABLE)
            .map(Map.Entry::getKey)
            .sorted((a, b) -> {
                int byRot = Integer.compare(a.rotationNo(), b.rotationNo());
                return byRot != 0 ? byRot : Integer.compare(a.slotPosition(), b.slotPosition());
            })
            .toList();
    }

    /** BR-V05 — LP 회전 (machine, rotation_no) distinct 카운트. 슬롯 단위 X. */
    public long countLpRotationsAvailable(LocalDate date) {
        Set<String> distinctRotations = cells.entrySet().stream()
            .filter(e -> e.getKey().date().equals(date))
            .filter(e -> e.getKey().machineId().startsWith("LP-"))
            .filter(e -> e.getValue() == SlotAvailability.AVAILABLE)
            .map(e -> e.getKey().machineId() + "#" + e.getKey().rotationNo())
            .collect(Collectors.toUnmodifiableSet());
        return distinctRotations.size();
    }

    /** BR-V05 — IC 회전 (machine, rotation_no) distinct 카운트. */
    public long countIcRotationsAvailable(LocalDate date) {
        Set<String> distinctRotations = cells.entrySet().stream()
            .filter(e -> e.getKey().date().equals(date))
            .filter(e -> e.getKey().machineId().startsWith("IC-"))
            .filter(e -> e.getValue() == SlotAvailability.AVAILABLE)
            .map(e -> e.getKey().machineId() + "#" + e.getKey().rotationNo())
            .collect(Collectors.toUnmodifiableSet());
        return distinctRotations.size();
    }

    public int totalCellCount() {
        return cells.size();
    }
}
