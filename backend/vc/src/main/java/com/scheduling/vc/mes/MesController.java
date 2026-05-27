package com.scheduling.vc.mes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;

/**
 * Sprint 17 EP-DAY-LOCK BR-X06 MES endpoint — TK-DAY-LOCK-4-1 + degraded status GET.
 *
 * <p>RBAC:
 * <ul>
 *   <li>{@code POST /api/v1/mes/shift/fallback} — PLANNER + IT_OPS (Excel 수동 폴백 입력)</li>
 *   <li>{@code GET /api/v1/mes/degraded/status} — 4 role 모두 (Frontend 배너 표시 용)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/mes")
@Profile("with-infra")
public class MesController {

    private final MesShiftPort mesPort;
    private final DegradedModeService degradedService;

    public MesController(MesShiftPort mesPort, DegradedModeService degradedService) {
        this.mesPort = mesPort;
        this.degradedService = degradedService;
    }

    public record ShiftFallbackPayload(
        @NotBlank String machineId,
        @NotNull LocalDate shiftDate,
        @NotNull Short shiftNo,
        @Min(0) int plannedQty,
        Integer actualQty,
        String note
    ) {}

    public record ShiftFallbackResponse(
        String shiftEventId, String machineId, LocalDate shiftDate, short shiftNo,
        int plannedQty, Integer actualQty, MesShiftSource source, String reportedBy
    ) {
        static ShiftFallbackResponse from(MesShiftEvent e) {
            return new ShiftFallbackResponse(
                e.getShiftEventId().toString(), e.getMachineId(), e.getShiftDate(),
                e.getShiftNo(), e.getPlannedQty(), e.getActualQty(), e.getSource(),
                e.getReportedBy());
        }
    }

    @PostMapping("/shift/fallback")
    @PreAuthorize("hasAnyRole('PLANNER','IT_OPS')")
    public ResponseEntity<ShiftFallbackResponse> excelFallback(
        @Valid @RequestBody ShiftFallbackPayload payload,
        Principal principal
    ) {
        String actor = principal != null ? principal.getName() : "anonymousUser";
        MesShiftEvent event = mesPort.reportProduction(
            payload.machineId(), payload.shiftDate(), payload.shiftNo(),
            payload.plannedQty(), payload.actualQty(),
            MesShiftSource.EXCEL_FALLBACK, actor, payload.note());
        return ResponseEntity.ok(ShiftFallbackResponse.from(event));
    }

    @GetMapping("/degraded/status")
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<DegradedModeService.DegradedSnapshot> degradedStatus() {
        return ResponseEntity.ok(degradedService.snapshot());
    }
}
