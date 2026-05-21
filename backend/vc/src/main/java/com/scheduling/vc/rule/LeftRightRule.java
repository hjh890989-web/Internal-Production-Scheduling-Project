package com.scheduling.vc.rule;

import com.scheduling.master.api.VcConstraintLookup;
import com.scheduling.master.api.VcConstraintSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * LP 좌/우 셋팅 검증 — TK-21-1-2 (BR-V15·V16, REQ-FUNC-VC-021).
 *
 * <p>{@link GreedyRotationAllocator} 가 슬롯 후보 검증 시 호출. LP 머신 ID 로 좌/우 셋팅 추론
 * (LP-01·02 LEFT, LP-03·04 RIGHT) → 품번의 {@code lpLeftSetting}/{@code lpRightSetting} 일치 검증.
 *
 * <p>IC 머신 또는 마스터 미등록 hose → fail-open (true) — 별도 rule (Unschedulable, slot O/X)
 * 가 차단. 본 rule 는 LP 좌/우 위반만 거부.
 */
@Component
@Profile("with-infra")
public class LeftRightRule {

    private final VcConstraintLookup lookup;

    public LeftRightRule(VcConstraintLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * 슬롯 배치 가능 여부.
     *
     * @param hoseId    품번
     * @param machineId 머신 ID (LP-01~04 / IC-01)
     * @return true = 배치 허용, false = BR-V15·V16 위반으로 거부
     */
    public boolean validate(String hoseId, String machineId) {
        Optional<SlotSide> sideOpt = SlotSide.ofLp(machineId);
        if (sideOpt.isEmpty()) {
            return true;  // IC 등 — LP 좌/우 rule 미적용
        }
        Optional<VcConstraintSummary> constraintOpt = lookup.findById(hoseId);
        if (constraintOpt.isEmpty()) {
            return true;  // 마스터 미등록 — Unschedulable rule 이 별도 차단
        }
        VcConstraintSummary c = constraintOpt.get();
        return switch (sideOpt.get()) {
            case LEFT -> c.allowsLeft();
            case RIGHT -> c.allowsRight();
        };
    }
}
