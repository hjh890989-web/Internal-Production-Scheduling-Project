package com.scheduling.vc.mes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 17 MesShiftEvent 영속 — TK-DAY-LOCK-3-3.
 *
 * <p>{@link #findTopByMachineIdOrderByReceivedAtDesc} = DegradedModeService hot path
 * (1 shift 미수신 임계 감지).
 */
public interface MesShiftEventRepository extends JpaRepository<MesShiftEvent, UUID> {

    Optional<MesShiftEvent> findTopByMachineIdOrderByReceivedAtDesc(String machineId);

    Optional<MesShiftEvent> findByMachineIdAndShiftDateAndShiftNo(
        String machineId, LocalDate shiftDate, short shiftNo);

    List<MesShiftEvent> findByMachineIdAndShiftDateOrderByShiftNo(String machineId, LocalDate shiftDate);
}
