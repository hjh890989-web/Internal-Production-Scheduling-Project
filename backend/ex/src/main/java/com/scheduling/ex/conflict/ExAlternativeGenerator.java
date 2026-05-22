package com.scheduling.ex.conflict;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 압출 충돌 대안 생성기 — TK-EX12-1-1 (REQ-FUNC-EX-012).
 *
 * <p>SRS 명시: 모든 분류 conflict 에 ≥ 3 distinct 대안. POLICY 맵 + defaultPolicy fallback.
 * EP-VC15 패턴 재사용 (성형 AlternativeGenerator) — 도메인만 압출 특화.
 *
 * <p>주요 제외 케이스:
 * <ul>
 *   <li>SHIFT_CAPACITY_EXCEEDED → EARLIER_START 제외 (시간 부족 문제, 시작 시점 변경 무의미)</li>
 * </ul>
 */
@Component
public class ExAlternativeGenerator {

    public static final int MIN_ALTERNATIVES = 3;

    private static final Map<ExConflictCategory, List<ExAlternativeType>> POLICY = Map.of(
        ExConflictCategory.CUMULATIVE_YIELD_SHORT,  List.of(
            ExAlternativeType.EARLIER_START, ExAlternativeType.NIGHT_SECOND_BOOST,
            ExAlternativeType.OVERTIME, ExAlternativeType.OUTSOURCE),
        ExConflictCategory.SHIFT_CAPACITY_EXCEEDED, List.of(
            ExAlternativeType.SWAP_CANDIDATE, ExAlternativeType.VC_DATE_NEGOTIATE,
            ExAlternativeType.OUTSOURCE, ExAlternativeType.OVERTIME),
        ExConflictCategory.DEADLINE_D1_VIOLATION,   List.of(
            ExAlternativeType.EARLIER_START, ExAlternativeType.VC_DATE_NEGOTIATE,
            ExAlternativeType.OUTSOURCE, ExAlternativeType.NIGHT_SECOND_BOOST),
        ExConflictCategory.SETUP_REQUIRED,          List.of(
            ExAlternativeType.SWAP_CANDIDATE, ExAlternativeType.OVERTIME,
            ExAlternativeType.OUTSOURCE, ExAlternativeType.VC_DATE_NEGOTIATE),
        ExConflictCategory.GROUP_MIXED,             List.of(
            ExAlternativeType.SWAP_CANDIDATE, ExAlternativeType.NIGHT_SECOND_BOOST,
            ExAlternativeType.OUTSOURCE, ExAlternativeType.VC_DATE_NEGOTIATE)
    );

    private static final List<ExAlternativeType> DEFAULT_POLICY = List.of(
        ExAlternativeType.EARLIER_START, ExAlternativeType.NIGHT_SECOND_BOOST,
        ExAlternativeType.VC_DATE_NEGOTIATE, ExAlternativeType.OUTSOURCE,
        ExAlternativeType.OVERTIME, ExAlternativeType.SWAP_CANDIDATE);

    /**
     * ExClassifiedConflict → ≥ 3 distinct 대안.
     */
    public List<ExAlternative> generate(ExClassifiedConflict cc) {
        List<ExAlternativeType> primary = POLICY.getOrDefault(cc.category(), DEFAULT_POLICY);
        Set<ExAlternativeType> distinctSet = new LinkedHashSet<>(primary);

        if (distinctSet.size() < MIN_ALTERNATIVES) {
            distinctSet.addAll(DEFAULT_POLICY);
        }

        List<ExAlternative> out = new ArrayList<>(distinctSet.size());
        for (ExAlternativeType t : distinctSet) {
            out.add(ExAlternative.of(t, cc));
        }
        return out;
    }
}
