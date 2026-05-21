package com.scheduling.master.vc;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 47품번 × 7 슬롯 적합성 매트릭스 (인메모리, immutable) — TK-04-1-2.
 *
 * <p>구조:
 * <ul>
 *   <li>{@code byHose} — hose_id → (SlotPosition → boolean)</li>
 *   <li>{@code bySlot} — SlotPosition → eligible hose_ids (역인덱스)</li>
 *   <li>{@code unschedulableHoseIds} — BR-V11 7 슬롯 모두 X 인 품번</li>
 * </ul>
 *
 * <p>매 빌드마다 {@link #version} +1 (monotonic). 한 번 빌드된 매트릭스는 immutable —
 * 멀티스레드 read-safe (조회 ≤ 1ms).
 *
 * @param version            빌드 버전 (monotonic counter)
 * @param builtAt            빌드 시각 (KST — BR-X04)
 * @param byHose             정방향 매핑
 * @param bySlot             슬롯 → 가능 품번 역인덱스
 * @param unschedulableHoseIds BR-V11
 */
public record SlotCompatibilityMatrix(
    int version,
    Instant builtAt,
    Map<String, Map<SlotPosition, Boolean>> byHose,
    Map<SlotPosition, Set<String>> bySlot,
    Set<String> unschedulableHoseIds
) {

    /** 단일 (품번, 슬롯) 적합성 조회 — HashMap O(1). */
    public boolean isEligible(String hoseId, SlotPosition slot) {
        Map<SlotPosition, Boolean> m = byHose.get(hoseId);
        return m != null && Boolean.TRUE.equals(m.get(slot));
    }

    /** 슬롯 → 가능 품번 집합 (역인덱스 조회). */
    public Set<String> eligibleHoseIdsFor(SlotPosition slot) {
        return bySlot.getOrDefault(slot, Set.of());
    }

    /** 품번 → 가능 슬롯 집합. */
    public Set<SlotPosition> eligibleSlotsFor(String hoseId) {
        Map<SlotPosition, Boolean> m = byHose.get(hoseId);
        if (m == null) return Set.of();
        return m.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableSet());
    }

    /** 매트릭스 빌드 헬퍼 — VcConstraint 리스트 → immutable matrix. */
    public static SlotCompatibilityMatrix build(
        int version,
        Instant builtAt,
        Iterable<VcConstraint> constraints
    ) {
        Map<String, Map<SlotPosition, Boolean>> byHose = new HashMap<>();
        Map<SlotPosition, Set<String>> bySlot = new EnumMap<>(SlotPosition.class);
        Set<String> unschedulable = new HashSet<>();

        for (SlotPosition s : SlotPosition.values()) {
            bySlot.put(s, new HashSet<>());
        }

        for (VcConstraint c : constraints) {
            Map<SlotPosition, Boolean> hoseSlots = new EnumMap<>(SlotPosition.class);
            for (SlotPosition s : SlotPosition.values()) {
                boolean ok = c.isEligibleFor(s);
                hoseSlots.put(s, ok);
                if (ok) {
                    bySlot.get(s).add(c.getHoseId());
                }
            }
            byHose.put(c.getHoseId(), Map.copyOf(hoseSlots));
            if (c.isUnschedulable()) {
                unschedulable.add(c.getHoseId());
            }
        }

        // 모든 컬렉션 immutable 복사
        Map<SlotPosition, Set<String>> bySlotImmutable = bySlot.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue())));

        return new SlotCompatibilityMatrix(
            version,
            builtAt,
            Map.copyOf(byHose),
            bySlotImmutable,
            Set.copyOf(unschedulable)
        );
    }
}
