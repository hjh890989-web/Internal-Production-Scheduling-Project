package com.scheduling.vc.swap;

/**
 * Swap proposal 상태 머신 — TK-15-2-2 (EP-15 ST-15-2).
 *
 * <pre>
 *   PROPOSED → ACCEPTED   (Planner 1클릭 수용 — 총량 보존 swap)
 *   PROPOSED → REJECTED   (Planner 거절 + 사유)
 * </pre>
 */
public enum SwapStatus {
    PROPOSED, ACCEPTED, REJECTED
}
