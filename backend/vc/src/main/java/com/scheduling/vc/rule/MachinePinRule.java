package com.scheduling.vc.rule;

import com.scheduling.master.api.HoseRuleLookup;
import com.scheduling.master.api.VcHoseRuleSummary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * VC_HOSE_RULE machine_pin / lp_only 검증 — TK-21-3-1 (BR-V14, REQ-FUNC-VC-024).
 *
 * <p>{@code 28422-08HA0 → machine_pin=LP-01, lp_only=TRUE} 패턴.
 *
 * <ul>
 *   <li>{@code machine_pin} NULL → 자유 배치 (pass)</li>
 *   <li>{@code machine_pin='LP-01'} → machineId == 'LP-01' 만 pass</li>
 *   <li>{@code lp_only=TRUE} + IC 머신 → fail</li>
 * </ul>
 *
 * <p>마스터 미등록 hose → fail-open (Unschedulable rule 이 별도 차단).
 */
@Component
@Profile("with-infra")
public class MachinePinRule {

    private final HoseRuleLookup lookup;

    public MachinePinRule(HoseRuleLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * @param hoseId    품번
     * @param machineId 머신 ID (LP-01~04 / IC-01)
     * @return true = 배치 허용, false = BR-V14 위반
     */
    public boolean validate(String hoseId, String machineId) {
        Optional<VcHoseRuleSummary> ruleOpt = lookup.findById(hoseId);
        if (ruleOpt.isEmpty()) return true;     // 룰 없음 = 자유

        VcHoseRuleSummary rule = ruleOpt.get();

        // lp_only 검증 — IC 머신 거부
        if (rule.lpOnly() && machineId.startsWith("IC-")) {
            return false;
        }

        // machine_pin 검증
        if (rule.hasMachinePin() && !rule.machinePin().equals(machineId)) {
            return false;
        }

        return true;
    }
}
