package com.scheduling.master.api;

import java.util.Set;

/**
 * SlotCompatibilityMatrix 조회 facade — TK-05-3-2 (Modulith cross-module).
 *
 * <p>{@code com.scheduling.master.vc.SlotCompatibilityMatrixService} 는 master 모듈 내부.
 * vc 모듈 (GreedyRotationAllocator) 은 본 facade 만 사용 (allowedDependencies = master::api).
 *
 * <p>{@code slotPosition} 은 enum name (예: "LP_TOP", "LP_UPMID", "IC_BOT") — string 으로
 * cross-module 안전 전달.
 *
 * <p>구현체: {@code com.scheduling.master.vc.SlotCompatibilityQueryImpl}.
 */
public interface SlotCompatibilityQuery {

    /** 단일 (품번, 슬롯) 적합성. matrix 미초기화 시 false. */
    boolean isEligible(String hoseId, String slotPosition);

    /** BR-V11 Unschedulable 품번. */
    Set<String> unschedulableHoseIds();

    /** 현재 매트릭스 버전 — caller 의 audit 정합. */
    int currentVersion();
}
