package com.scheduling.vc.rule;

import com.scheduling.master.api.HoseRuleLookup;
import com.scheduling.master.api.VcHoseRuleSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * VC_HOSE_RULE max_concurrent_slots / side_lock 검증 — TK-21-3-2 / TK-21-4-1
 * (BR-V14·V15·V16, REQ-FUNC-VC-024·025·026).
 *
 * <p>같은 (hose, date, machine, rotation) 의 누적 슬롯 + 1 ≤ max_concurrent_slots 검증.
 * side_lock 도 함께 검증 (LeftRightRule 보조 — 데이터 정합 가드).
 *
 * <p>예시:
 * <ul>
 *   <li>28422-08HA0 (max=1) — 같은 회전에 2번째 슬롯 시도 → fail</li>
 *   <li>28422-2M800 (max=2, lock=RIGHT) — 3번째 슬롯 또는 LEFT 시도 → fail</li>
 *   <li>28421-2M800 (max=2, lock=LEFT)  — 3번째 슬롯 또는 RIGHT 시도 → fail</li>
 * </ul>
 *
 * <p>룰 없는 hose → max=99 (사실상 무제한) + side 자유.
 */
@Component
@Profile("with-infra")
public class HoseSlotCapRule {

    private static final int UNLIMITED = 99;

    private final HoseRuleLookup lookup;

    public HoseSlotCapRule(HoseRuleLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * 본 hose 의 max_concurrent_slots — 없으면 99 (무제한).
     */
    public int maxConcurrentSlots(String hoseId) {
        return lookup.findById(hoseId).map(VcHoseRuleSummary::maxConcurrentSlots).orElse(UNLIMITED);
    }

    /**
     * 동시 슬롯 상한 검증.
     *
     * @param hoseId               품번
     * @param currentSlotCount     이미 점유된 슬롯 수 (같은 hose+machine+date+rotation)
     * @return true = 추가 1슬롯 허용, false = 상한 초과
     */
    public boolean fitsCap(String hoseId, int currentSlotCount) {
        return (currentSlotCount + 1) <= maxConcurrentSlots(hoseId);
    }

    /**
     * side_lock 검증 — VcHoseRule.side_lock 이 후보 slot 측면과 일치하는지.
     *
     * @param hoseId    품번
     * @param machineId 머신 ID (LP-* 만 의미, IC 는 항상 pass)
     * @return true = 허용, false = side_lock 위반
     */
    public boolean fitsSide(String hoseId, String machineId) {
        Optional<VcHoseRuleSummary> ruleOpt = lookup.findById(hoseId);
        if (ruleOpt.isEmpty()) return true;
        VcHoseRuleSummary r = ruleOpt.get();
        if (!r.hasSideLock()) return true;
        Optional<SlotSide> sideOpt = SlotSide.ofLp(machineId);
        if (sideOpt.isEmpty()) return true;     // IC — rule 미적용
        return r.sideLock().equals(sideOpt.get().name());
    }
}
