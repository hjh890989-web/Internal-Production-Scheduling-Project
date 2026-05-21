package com.scheduling.vc.domain;

import java.time.LocalDate;

/**
 * 회전 격자 단일 셀 — TK-05-1-1 (ADR-005).
 *
 * <p>(date, machineId, rotationNo, slotPosition) 4-tuple PK 의 도메인 표현.
 * {@code rotationNo} 1~18 (BR-V04: 주간 8 + 야간 10).
 * {@code slotPosition} 1~8 LP / 1~6 IC (BR-V05 VcMachine.totalSlots).
 *
 * <p>immutable record — HashMap key 안전.
 */
public record RotationSlot(
    LocalDate date,
    String machineId,
    int rotationNo,
    int slotPosition
) {
    public RotationSlot {
        if (date == null) {
            throw new IllegalArgumentException("date 필수");
        }
        if (machineId == null || machineId.isBlank()) {
            throw new IllegalArgumentException("machineId 필수");
        }
        if (rotationNo < 1 || rotationNo > 18) {
            throw new IllegalArgumentException("rotationNo 1..18: " + rotationNo);
        }
        if (slotPosition < 1) {
            throw new IllegalArgumentException("slotPosition ≥ 1: " + slotPosition);
        }
    }

    /** 1-8 회전 — 주간 (BR-V04). */
    public boolean isDayRotation() {
        return rotationNo <= 8;
    }

    /** 9-18 회전 — 야간 (BR-V04). */
    public boolean isNightRotation() {
        return rotationNo >= 9;
    }
}
