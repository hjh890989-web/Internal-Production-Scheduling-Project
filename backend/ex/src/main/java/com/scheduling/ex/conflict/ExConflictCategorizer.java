package com.scheduling.ex.conflict;

import com.scheduling.ex.gate.ExGateResult;
import com.scheduling.ex.gate.ExGateViolation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 압출 충돌 분류기 — TK-EX12-1-1 (REQ-FUNC-EX-012).
 *
 * <p>{@link ExGateResult} 의 violations → {@link ExClassifiedConflict} 변환.
 * 1차로 {@link ExGateViolation.Category} 매핑, 2차로 reason 텍스트의 BR-Xxx 패턴 추출.
 */
@Component
public class ExConflictCategorizer {

    /**
     * 게이트 결과의 violations 분류.
     *
     * @param result   게이트 결과 (passed=false 인 경우 의미)
     * @param hoseId   대상 hose (UI 표시용)
     */
    public List<ExClassifiedConflict> classify(ExGateResult result, String hoseId) {
        if (result == null || result.passed() || result.violations().isEmpty()) {
            return List.of();
        }
        List<ExClassifiedConflict> out = new ArrayList<>();
        for (ExGateViolation v : result.violations()) {
            ExConflictCategory category = mapCategory(v);
            out.add(new ExClassifiedConflict(result.candidateId(), hoseId, category, v));
        }
        return out;
    }

    /** 카테고리별 위반 카운트. */
    public Map<ExConflictCategory, Long> countByCategory(List<ExClassifiedConflict> conflicts) {
        return conflicts.stream()
            .collect(Collectors.groupingBy(ExClassifiedConflict::category, Collectors.counting()));
    }

    private static ExConflictCategory mapCategory(ExGateViolation violation) {
        return switch (violation.category()) {
            case CUMULATIVE_YIELD_SHORT  -> ExConflictCategory.CUMULATIVE_YIELD_SHORT;
            case SHIFT_CAPACITY_EXCEEDED -> ExConflictCategory.SHIFT_CAPACITY_EXCEEDED;
        };
    }
}
