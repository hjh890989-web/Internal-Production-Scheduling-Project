package com.scheduling.vc.yield;

import com.scheduling.master.api.VcConstraintLookup;
import com.scheduling.master.api.VcConstraintSummary;
import com.scheduling.vc.domain.RotationSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 앵글 capa 검증기 — TK-05-2-2 (BR-V06).
 *
 * <p>BR-V06 해석:
 * <ul>
 *   <li>한 hose_id 는 같은 (date, machine_type, rotation_no) 그룹의 동시 점유 슬롯 수가
 *       lp_angle_qty (LP) 또는 ic_angle_qty (IC) 를 초과할 수 없음.</li>
 *   <li>같은 hose_id 라도 다른 회전 시점은 별도 그룹 — 앵글 재사용.</li>
 *   <li>다른 머신 (LP-01 vs LP-02) 동일 회전 — 별도 앵글 셋업 (별도 그룹).</li>
 * </ul>
 *
 * <p>{@code @Profile("with-infra")} — VcConstraintLookup (JPA) 의존.
 */
@Component
@Profile("with-infra")
public class AngleCapacityValidator {

    private static final Logger log = LoggerFactory.getLogger(AngleCapacityValidator.class);

    private final VcConstraintLookup lookup;

    public AngleCapacityValidator(VcConstraintLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * 후보 배치 검증.
     *
     * @param assignments hose_id → 배치된 RotationSlot 목록 (caller 가 그룹핑)
     * @return 위반 목록 (빈 리스트 = 통과)
     */
    public List<AngleCapacityViolation> validate(Map<String, ? extends Collection<RotationSlot>> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return List.of();
        }

        // 1. (date, hoseId, machineType, rotationNo, machineId) 그룹 → slotPosition Set
        //    BR-V06: 같은 머신·회전 = 1 앵글 그룹
        Map<GroupKey, Set<Integer>> usage = new HashMap<>();
        for (Map.Entry<String, ? extends Collection<RotationSlot>> e : assignments.entrySet()) {
            String hoseId = e.getKey();
            for (RotationSlot slot : e.getValue()) {
                String machineType = slot.machineId().startsWith("LP-") ? "LP" : "IC";
                GroupKey key = new GroupKey(slot.date(), hoseId, machineType,
                    slot.rotationNo(), slot.machineId());
                usage.computeIfAbsent(key, k -> new HashSet<>()).add(slot.slotPosition());
            }
        }

        // 2. 같은 (date, hoseId, machineType, rotationNo) 의 다중 머신 합산 (다른 머신 = 별도 앵글)
        //    각 머신 인스턴스별 capa 체크 — 같은 머신·회전 그룹의 슬롯 수만 비교
        List<AngleCapacityViolation> violations = new ArrayList<>();
        for (Map.Entry<GroupKey, Set<Integer>> entry : usage.entrySet()) {
            GroupKey k = entry.getKey();
            int concurrentSlots = entry.getValue().size();
            int allowed = getAngleQty(k.hoseId(), k.machineType());

            if (concurrentSlots > allowed) {
                AngleCapacityViolation v = new AngleCapacityViolation(
                    k.date(), k.hoseId(), k.machineType(), k.rotationNo(),
                    concurrentSlots, allowed);
                violations.add(v);
                log.debug("Angle capa violation: {}", v.userMessage());
            }
        }
        return violations;
    }

    /** 빠른 체크 — caller 가 단일 (hose, machine, slot count) 확인용. */
    public boolean isWithinCapacity(String hoseId, String machineType, int requestedSlots) {
        return requestedSlots <= getAngleQty(hoseId, machineType);
    }

    private int getAngleQty(String hoseId, String machineType) {
        return lookup.findById(hoseId)
            .map(c -> {
                Short qty = switch (machineType) {
                    case "LP" -> c.lpAngleQty();
                    case "IC" -> c.icAngleQty();
                    default -> null;
                };
                return qty != null ? qty.intValue() : 0;
            })
            .orElse(0);
    }

    /** 내부 그룹 키 — (date, hoseId, machineType, rotation, machineId). */
    private record GroupKey(LocalDate date, String hoseId, String machineType,
                             int rotationNo, String machineId) {}
}
