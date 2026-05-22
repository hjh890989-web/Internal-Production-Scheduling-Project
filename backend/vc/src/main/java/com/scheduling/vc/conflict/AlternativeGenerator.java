package com.scheduling.vc.conflict;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 카테고리별 대안 생성기 — TK-VC15-1-2 (REQ-FUNC-VC-015).
 *
 * <p>SRS 명시: 모든 분류 conflict 에 ≥ 3 distinct 대안. POLICY 맵에 카테고리별 적용 가능
 * 대안 정의 + defaultPolicy fallback.
 *
 * <p>주요 제외 케이스:
 * <ul>
 *   <li>SPEC_LT7 → IC_ROUTING 제외 (BR-V17 은 가류기당 cap, IC 전환은 효과 없음)</li>
 *   <li>MACHINE_PIN → IC_ROUTING 제외 (핀된 호기 사용이 본 룰)</li>
 *   <li>LEFT_RIGHT → IC_ROUTING 제외 (좌/우는 LP 한정)</li>
 * </ul>
 */
@Component
public class AlternativeGenerator {

    public static final int MIN_ALTERNATIVES = 3;

    private static final Map<ConflictCategory, List<AlternativeType>> POLICY = Map.of(
        ConflictCategory.SLOT_OX,       List.of(
            AlternativeType.NIGHT_ROTATION, AlternativeType.EXPAND_CAPA,
            AlternativeType.OUTSOURCE, AlternativeType.SWAP_ORDER),
        ConflictCategory.ANGLE_CAPA,    List.of(
            AlternativeType.NIGHT_ROTATION, AlternativeType.IC_ROUTING,
            AlternativeType.EXPAND_CAPA, AlternativeType.OUTSOURCE),
        ConflictCategory.DAILY_CAPA,    List.of(
            AlternativeType.NIGHT_ROTATION, AlternativeType.EXPAND_CAPA,
            AlternativeType.OUTSOURCE, AlternativeType.SWAP_ORDER),
        ConflictCategory.DEADLINE_D2,   List.of(
            AlternativeType.NIGHT_ROTATION, AlternativeType.DEADLINE_NEGOTIATE,
            AlternativeType.OUTSOURCE, AlternativeType.EXPAND_CAPA),
        ConflictCategory.DAY_LOCK,      List.of(
            AlternativeType.NIGHT_ROTATION, AlternativeType.SWAP_ORDER,
            AlternativeType.EXPAND_CAPA, AlternativeType.OUTSOURCE),
        ConflictCategory.LEFT_RIGHT,    List.of(
            AlternativeType.SWAP_ORDER, AlternativeType.OUTSOURCE,
            AlternativeType.EXPAND_CAPA, AlternativeType.DEADLINE_NEGOTIATE),
        ConflictCategory.MACHINE_PIN,   List.of(
            AlternativeType.DEADLINE_NEGOTIATE, AlternativeType.OUTSOURCE,
            AlternativeType.EXPAND_CAPA, AlternativeType.SWAP_ORDER),
        ConflictCategory.SPEC_LT7,      List.of(
            AlternativeType.NIGHT_ROTATION, AlternativeType.EXPAND_CAPA,
            AlternativeType.SWAP_ORDER, AlternativeType.OUTSOURCE),
        ConflictCategory.HOSE_CAP,      List.of(
            AlternativeType.NIGHT_ROTATION, AlternativeType.EXPAND_CAPA,
            AlternativeType.OUTSOURCE, AlternativeType.SWAP_ORDER),
        ConflictCategory.UNSCHEDULABLE, List.of(
            AlternativeType.OUTSOURCE, AlternativeType.DEADLINE_NEGOTIATE,
            AlternativeType.SWAP_ORDER, AlternativeType.EXPAND_CAPA)
    );

    private static final List<AlternativeType> DEFAULT_POLICY = List.of(
        AlternativeType.NIGHT_ROTATION, AlternativeType.OUTSOURCE,
        AlternativeType.EXPAND_CAPA, AlternativeType.DEADLINE_NEGOTIATE,
        AlternativeType.SWAP_ORDER);

    /**
     * ClassifiedConflict → ≥ 3 distinct 대안.
     *
     * <p>POLICY 적용 → distinct 보장 → MIN_ALTERNATIVES 미만 시 defaultPolicy 보완.
     */
    public List<Alternative> generate(ClassifiedConflict cc) {
        List<AlternativeType> primary = POLICY.getOrDefault(cc.category(), DEFAULT_POLICY);
        Set<AlternativeType> distinctSet = new LinkedHashSet<>(primary);

        if (distinctSet.size() < MIN_ALTERNATIVES) {
            distinctSet.addAll(DEFAULT_POLICY);
        }

        List<Alternative> out = new ArrayList<>(distinctSet.size());
        for (AlternativeType t : distinctSet) {
            out.add(Alternative.of(t, cc));
        }
        return out;
    }
}
