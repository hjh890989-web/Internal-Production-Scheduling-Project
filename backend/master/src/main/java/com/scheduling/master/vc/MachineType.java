package com.scheduling.master.vc;

/**
 * 성형 가류기 유형 — TK-04-1-1 (SAD §6.2.2).
 *
 * <ul>
 *   <li>{@link #LP} — 저압 가류기 (4 슬롯: TOP / UPMID / LOWMID / BOT)</li>
 *   <li>{@link #IC} — IC 가류기 (3 슬롯: TOP / MID / BOT)</li>
 * </ul>
 */
public enum MachineType {
    LP,
    IC
}
