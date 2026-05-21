package com.scheduling.master.api;

import com.scheduling.master.vc.SlotCompatibilityMatrix;
import com.scheduling.master.vc.SlotPosition;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * GET /api/v1/master/compat 응답 — TK-04-1-3.
 *
 * <p>{@link SlotCompatibilityMatrix} 의 wire format. byHose + bySlot + unschedulable +
 * version + builtAt. Frontend (ST-04-3 dnd-kit) 이 클라이언트 사이드 가드용으로 소비.
 *
 * @param version              매트릭스 버전 (monotonic — ETag 키)
 * @param builtAt              빌드 시각 (KST — BR-X04)
 * @param byHose               hose_id → (SlotPosition → boolean)
 * @param bySlot               SlotPosition → eligible hose_ids 역인덱스
 * @param unschedulableHoseIds BR-V11 7 슬롯 모두 X 인 품번 (분리 보고용)
 */
public record CompatibilityResponse(
    int version,
    Instant builtAt,
    Map<String, Map<SlotPosition, Boolean>> byHose,
    Map<SlotPosition, Set<String>> bySlot,
    Set<String> unschedulableHoseIds
) {
    public static CompatibilityResponse from(SlotCompatibilityMatrix m) {
        return new CompatibilityResponse(
            m.version(),
            m.builtAt(),
            m.byHose(),
            m.bySlot(),
            m.unschedulableHoseIds()
        );
    }

    /** 단일 (품번, 슬롯) 조회 응답 — 가벼운 payload. */
    public record PointCheck(String hoseId, String slotPosition, boolean eligible) {}
}
