package com.scheduling.vc.capacity;

/**
 * CapacityLedger 셀 상태 — TK-05-1-2.
 *
 * <ul>
 *   <li>{@link #AVAILABLE} — 비어 있음, greedy 배치 후보</li>
 *   <li>{@link #RESERVED} — VcSchedule.CANDIDATE (사용자 검토 중)</li>
 *   <li>{@link #CONFIRMED} — VcSchedule.CONFIRMED 또는 DONE (배치 불가)</li>
 *   <li>{@link #UNAVAILABLE} — 마스터 정의 외 (key 미존재 fallback)</li>
 * </ul>
 */
public enum SlotAvailability {
    AVAILABLE,
    RESERVED,
    CONFIRMED,
    UNAVAILABLE
}
