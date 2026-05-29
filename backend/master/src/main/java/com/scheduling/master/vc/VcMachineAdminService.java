package com.scheduling.master.vc;

import com.scheduling.audit.aop.Auditable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Sprint 21 ST-CRUD-1 VcMachine 관리 Service (IT_OPS 전용).
 *
 * <p>{@code @Auditable} BR-X02 — 모든 mutation audit_log 기록.
 * DELETE 는 실제 row 삭제 대신 {@code active=false} toggle (의존 FK 보존).
 *
 * @see BR-X02
 */
@Service
public class VcMachineAdminService {

    private static final Logger log = LoggerFactory.getLogger(VcMachineAdminService.class);

    private final VcMachineRepository repository;
    private final Clock clock;

    public VcMachineAdminService(VcMachineRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<VcMachine> list() {
        return repository.findAll();
    }

    @Auditable("ST-CRUD-1 VcMachine 추가 (IT_OPS)")
    @Transactional
    public VcMachine create(String machineId, MachineType machineType,
                            short totalSlots, short dayRotations, short nightRotations,
                            boolean active, String actor) {
        if (repository.existsById(machineId)) {
            throw new EntityExistsException("machine_id 중복: " + machineId);
        }
        VcMachine m = new VcMachine(machineId, machineType, totalSlots,
            dayRotations, nightRotations, active, clock.instant(), actor);
        log.info("ST-CRUD-1 VcMachine create — id={} type={}", machineId, machineType);
        return repository.save(m);
    }

    /**
     * PUT — machine_type 은 updatable=false (엔티티 제약).
     * 기존 row 를 새 인스턴스로 대체 (save = merge).
     * total_slots, day_rotations, night_rotations, active 변경 허용.
     *
     * @see BR-X02
     */
    @Auditable("ST-CRUD-1 VcMachine 수정 (IT_OPS)")
    @Transactional
    public VcMachine update(String machineId, short totalSlots,
                            short dayRotations, short nightRotations,
                            boolean active, String actor) {
        VcMachine existing = repository.findById(machineId)
            .orElseThrow(() -> new EntityNotFoundException("machine_id 미존재: " + machineId));
        // machine_type 은 기존 값 유지 (updatable=false 컬럼 보존)
        VcMachine updated = new VcMachine(
            existing.getMachineId(),
            existing.getMachineType(),
            totalSlots, dayRotations, nightRotations,
            active, clock.instant(), actor
        );
        log.info("ST-CRUD-1 VcMachine update — id={} active={}", machineId, active);
        return repository.save(updated);
    }

    /**
     * DELETE → active=false toggle.
     * app.vc_schedule.machine_id FK row 는 그대로 보존.
     *
     * @see BR-X02
     */
    @Auditable("ST-CRUD-1 VcMachine 비활성화 (IT_OPS)")
    @Transactional
    public VcMachine deactivate(String machineId, String actor) {
        VcMachine existing = repository.findById(machineId)
            .orElseThrow(() -> new EntityNotFoundException("machine_id 미존재: " + machineId));
        VcMachine deactivated = new VcMachine(
            existing.getMachineId(),
            existing.getMachineType(),
            existing.getTotalSlots(),
            existing.getDayRotations(),
            existing.getNightRotations(),
            false,
            clock.instant(),
            actor
        );
        log.info("ST-CRUD-1 VcMachine deactivate — id={}", machineId);
        return repository.save(deactivated);
    }
}
