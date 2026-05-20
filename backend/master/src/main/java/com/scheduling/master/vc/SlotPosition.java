package com.scheduling.master.vc;

/**
 * 성형 슬롯 위치 — TK-04-1-1.
 *
 * <p>LP 4 슬롯 (TOP / UPMID / LOWMID / BOT) + IC 3 슬롯 (TOP / MID / BOT) = 총 7.
 * SlotPosition enum 의 정의 순서는 REF-09 컬럼 순서 (G~J·M~O) 와 정합.
 */
public enum SlotPosition {
    LP_TOP,
    LP_UPMID,
    LP_LOWMID,
    LP_BOT,
    IC_TOP,
    IC_MID,
    IC_BOT;

    public MachineType machineType() {
        return name().startsWith("LP_") ? MachineType.LP : MachineType.IC;
    }

    /** 같은 가류기 유형의 슬롯 배열 반환. */
    public static SlotPosition[] of(MachineType type) {
        return switch (type) {
            case LP -> new SlotPosition[]{LP_TOP, LP_UPMID, LP_LOWMID, LP_BOT};
            case IC -> new SlotPosition[]{IC_TOP, IC_MID, IC_BOT};
        };
    }
}
