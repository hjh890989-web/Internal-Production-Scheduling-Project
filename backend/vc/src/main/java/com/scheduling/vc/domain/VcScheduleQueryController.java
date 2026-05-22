package com.scheduling.vc.domain;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * VC 회전 격자 JSON 조회 — TK-15-1-1 (EP-15 ST-15-1, REQ-FUNC-VC-017).
 *
 * <p>{@code GET /api/v1/schedule/vc/slots?from=&to=}
 *
 * <p>Frontend AG Grid row = slot, col = rotation 1~18. 1주 horizon 의 회전 격자
 * (LP 4 × 8 slot + IC 1 × 6 slot × rotation 1~18).
 */
@RestController
@RequestMapping("/api/v1/schedule/vc")
@Profile("with-infra")
public class VcScheduleQueryController {

    private final VcScheduleRepository repository;

    public VcScheduleQueryController(VcScheduleRepository repository) {
        this.repository = repository;
    }

    public record SlotRow(
        UUID vcScheduleId,
        String hoseId,
        String machineId,
        short slotPosition,
        LocalDate productionDate,
        short rotationNo,
        String angleId,
        int plannedQty,
        String status
    ) {
        static SlotRow from(VcSchedule s) {
            return new SlotRow(
                s.getVcScheduleId(), s.getHoseId(), s.getMachineId(),
                s.getSlotPosition(), s.getProductionDate(), s.getRotationNo(),
                s.getAngleId(), s.getPlannedQty(), s.getStatus().name());
        }
    }

    @GetMapping("/slots")
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public List<SlotRow> findSlots(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return repository.findByDateRange(from, to).stream()
            .map(SlotRow::from)
            .toList();
    }
}
