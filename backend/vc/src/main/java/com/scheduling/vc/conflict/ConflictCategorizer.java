package com.scheduling.vc.conflict;

import com.scheduling.vc.allocator.AllocationConflict;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AllocationConflict → ConflictCategory 분류 — TK-VC15-1-1 (REQ-FUNC-VC-015).
 *
 * <p>1차로 {@link AllocationConflict.Category} (5종) 를 {@link ConflictCategory} (11종) 로 매핑.
 * 2차로 reason 텍스트의 BR-Xxx 코드를 추출해 보다 정확한 카테고리 결정.
 *
 * <p>다중 BR 매칭 시 LinkedHashMap 선언 순서 (가장 엄격한 hard 제약 우선).
 */
@Component
public class ConflictCategorizer {

    /** BR 코드 → ConflictCategory 매핑 (선언 순서 = 우선순위). */
    private static final Map<String, ConflictCategory> BR_TO_CATEGORY;

    static {
        Map<String, ConflictCategory> m = new LinkedHashMap<>();
        m.put("BR-V17", ConflictCategory.SPEC_LT7);
        m.put("BR-X07", ConflictCategory.DEADLINE_D2);
        m.put("BR-V14", ConflictCategory.MACHINE_PIN);
        m.put("BR-V15", ConflictCategory.LEFT_RIGHT);
        m.put("BR-V16", ConflictCategory.LEFT_RIGHT);
        m.put("BR-V13", ConflictCategory.SLOT_OX);
        m.put("BR-V11", ConflictCategory.UNSCHEDULABLE);
        m.put("BR-V07", ConflictCategory.DAY_LOCK);
        m.put("BR-V06", ConflictCategory.ANGLE_CAPA);
        m.put("BR-V03", ConflictCategory.DAILY_CAPA);
        BR_TO_CATEGORY = Map.copyOf(m);
    }

    /**
     * 단일 conflict 분류 — Category enum 1차 + reason 텍스트 BR 코드 2차.
     */
    public ClassifiedConflict classify(AllocationConflict conflict) {
        // 1차 — Allocator Category 에서 직접 매핑
        ConflictCategory direct = fromAllocatorCategory(conflict.category());

        // 2차 — reason 텍스트에서 BR 코드 추출 (Allocator Category 가 UNKNOWN 인 경우 보강)
        if (direct == ConflictCategory.UNKNOWN) {
            String reason = conflict.reason() == null ? "" : conflict.reason();
            ConflictCategory fromReason = BR_TO_CATEGORY.entrySet().stream()
                .filter(e -> reason.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(ConflictCategory.UNKNOWN);
            return new ClassifiedConflict(conflict, fromReason);
        }
        return new ClassifiedConflict(conflict, direct);
    }

    public List<ClassifiedConflict> classifyAll(List<AllocationConflict> conflicts) {
        return conflicts.stream().map(this::classify).toList();
    }

    /** 카테고리별 conflict 카운트 — UI 요약. */
    public Map<ConflictCategory, Long> countByCategory(List<AllocationConflict> conflicts) {
        return classifyAll(conflicts).stream()
            .collect(Collectors.groupingBy(ClassifiedConflict::category, Collectors.counting()));
    }

    /** AllocationConflict.Category → ConflictCategory 직접 매핑. */
    private static ConflictCategory fromAllocatorCategory(AllocationConflict.Category cat) {
        return switch (cat) {
            case UNSCHEDULABLE        -> ConflictCategory.UNSCHEDULABLE;
            case INSUFFICIENT_CAPACITY -> ConflictCategory.DAILY_CAPA;
            case ANGLE_VIOLATION       -> ConflictCategory.ANGLE_CAPA;
            case DEADLINE_EXCEEDED     -> ConflictCategory.DEADLINE_D2;
            case LEFT_RIGHT_VIOLATION  -> ConflictCategory.LEFT_RIGHT;
            case MACHINE_PIN_VIOLATION -> ConflictCategory.MACHINE_PIN;
            case HOSE_CAP_VIOLATION    -> ConflictCategory.HOSE_CAP;
        };
    }
}
