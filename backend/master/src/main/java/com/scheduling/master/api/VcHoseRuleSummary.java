package com.scheduling.master.api;

/**
 * 품번 운영 룰 요약 — TK-21-2-1 (cross-module).
 *
 * <p>vc 모듈은 {@link HoseRuleLookup} facade 로 본 record 만 받음.
 *
 * @param hoseId              품번
 * @param machinePin          고정 가류기 (LP-01 등), null = 자유 배치
 * @param maxConcurrentSlots  동시 다중 슬롯 상한 (1~99)
 * @param sideLock            'LEFT'/'RIGHT' 또는 null
 * @param lpOnly              IC 사용 금지
 */
public record VcHoseRuleSummary(
    String hoseId,
    String machinePin,
    int maxConcurrentSlots,
    String sideLock,
    boolean lpOnly
) {
    public boolean hasMachinePin() { return machinePin != null; }
    public boolean hasSideLock() { return sideLock != null; }
}
