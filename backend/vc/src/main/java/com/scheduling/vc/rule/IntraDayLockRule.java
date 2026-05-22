package com.scheduling.vc.rule;

import com.scheduling.vc.domain.RotationSlot;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * BR-V07 일중 락 — TK-13-2-1 (EP-13 ST-13-2).
 *
 * <p>"당일 투입 (가류기, 슬롯) 은 rotation 1~18 동일 앵글 연속 운전"
 *
 * <p>Allocator 후보 생성 시 candidate slot 의 (machine_id, slot_position, production_date)
 * 안에 다른 angle_id 가 이미 있다면 차단. override 모드는 본 룰을 우회 (DB trigger 가 reason 강제).
 *
 * <p>DB trigger {@code trg_vc_intra_day_lock} 와 이중 안전망 — 본 룰은 Allocator 후보 생성
 * 단계에서 미리 차단해 사용자 confirm UX 개선 (DB trigger 는 최후 방어선).
 */
@Component
@Profile("with-infra")
public class IntraDayLockRule {

    private final VcScheduleRepository repository;

    public IntraDayLockRule(VcScheduleRepository repository) {
        this.repository = repository;
    }

    /**
     * @param slot       후보 slot (machine_id + slot_position + date)
     * @param angleId    배치하려는 angle_id
     * @return true = 일중 락 통과 (같은 angle 만 있거나 비어있음), false = 다른 angle 존재 (차단)
     */
    public boolean validate(RotationSlot slot, String angleId) {
        if (angleId == null || angleId.isBlank()) return true;     // angle 미지정 — 별도 룰
        List<VcSchedule> existing = repository.findByMachineIdAndSlotPositionAndProductionDate(
            slot.machineId(), (short) slot.slotPosition(), slot.date());
        for (VcSchedule s : existing) {
            if (!angleId.equals(s.getAngleId())) {
                return false;       // 다른 angle 존재 → 일중 락 위반
            }
        }
        return true;
    }
}
