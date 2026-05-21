package com.scheduling.vc.rule;

import java.util.Optional;

/**
 * LP 가류기 좌/우 셋팅 분류 — TK-21-1-2 (BR-V15·V16).
 *
 * <p>현장 설치 방향에 따라 LP 4대 중 LP-01·02 는 좌측 셋팅, LP-03·04 는 우측 셋팅으로 운영.
 * 품번의 {@code lp_left_setting}·{@code lp_right_setting} 마스터 값과 일치해야 배치 가능.
 *
 * <p>IC 는 단일 머신 (IC-01) — 좌/우 셋팅 차별화 없음 ({@link #ofLp(String)} 가 Optional.empty 반환).
 */
public enum SlotSide {
    LEFT, RIGHT;

    /**
     * 머신 ID → LP 좌/우 셋팅.
     *
     * <ul>
     *   <li>LP-01 / LP-02 → LEFT</li>
     *   <li>LP-03 / LP-04 → RIGHT</li>
     *   <li>IC-* 또는 형식 외 → Optional.empty (rule 적용 X)</li>
     * </ul>
     */
    public static Optional<SlotSide> ofLp(String machineId) {
        if (machineId == null || !machineId.startsWith("LP-")) return Optional.empty();
        try {
            int n = Integer.parseInt(machineId.substring(3));
            return Optional.of(n <= 2 ? LEFT : RIGHT);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
