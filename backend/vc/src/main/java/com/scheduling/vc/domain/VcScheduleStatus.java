package com.scheduling.vc.domain;

/**
 * VcSchedule 라이프사이클 — TK-05-1-1.
 *
 * <pre>
 *   CANDIDATE → CONFIRMED → DONE
 * </pre>
 *
 * <ul>
 *   <li>{@link #CANDIDATE} — greedy 알고리즘 (TK-05-3-2) 결과, 사용자 확정 대기</li>
 *   <li>{@link #CONFIRMED} — 사용자 확정 (BR-X01 D-2 ~ D-1)</li>
 *   <li>{@link #DONE} — 실 가류 완료 (BR-V07 당일 락)</li>
 * </ul>
 */
public enum VcScheduleStatus {
    CANDIDATE,
    CONFIRMED,
    DONE
}
