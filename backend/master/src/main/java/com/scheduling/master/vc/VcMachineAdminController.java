package com.scheduling.master.vc;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

/**
 * Sprint 21 ST-CRUD-1 VcMachine 관리 REST endpoint.
 *
 * <p>RBAC — read: 4 role (PLANNER/STK_USER/IT_OPS/READ_ONLY),
 * write: IT_OPS only (BR-X02 audit 강제).
 * DELETE 는 실제 삭제 대신 active=false toggle — FK 의존 row 보존.
 *
 * @see BR-X02
 */
@RestController
@RequestMapping("/api/v1/master/vc-machines")
public class VcMachineAdminController {

    private final VcMachineAdminService service;

    public VcMachineAdminController(VcMachineAdminService service) {
        this.service = service;
    }

    // =========================================================================
    // Record DTOs
    // =========================================================================

    public record MachineSummary(
        String machineId,
        String machineType,
        short totalSlots,
        short dayRotations,
        short nightRotations,
        boolean active,
        Instant updatedAt,
        String updatedBy
    ) {
        public static MachineSummary from(VcMachine m) {
            return new MachineSummary(
                m.getMachineId(),
                m.getMachineType().name(),
                m.getTotalSlots(),
                m.getDayRotations(),
                m.getNightRotations(),
                m.isActive(),
                m.getUpdatedAt(),
                m.getUpdatedBy()
            );
        }
    }

    /** POST payload — machineType 포함 (신규 생성 시 필요). */
    public record MachineCreatePayload(
        @NotBlank String machineId,
        @NotNull MachineType machineType,
        @NotNull @Min(1) @Max(20) Short totalSlots,
        @NotNull @Min(1) @Max(24) Short dayRotations,
        @NotNull @Min(1) @Max(24) Short nightRotations,
        boolean active
    ) {}

    /** PUT payload — machine_type 은 updatable=false 이므로 제외. */
    public record MachineUpdatePayload(
        @NotNull @Min(1) @Max(20) Short totalSlots,
        @NotNull @Min(1) @Max(24) Short dayRotations,
        @NotNull @Min(1) @Max(24) Short nightRotations,
        boolean active
    ) {}

    // =========================================================================
    // Endpoints
    // =========================================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<List<MachineSummary>> list() {
        return ResponseEntity.ok(service.list().stream().map(MachineSummary::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> create(@RequestBody @Valid MachineCreatePayload payload,
                                    Principal principal) {
        try {
            VcMachine m = service.create(
                payload.machineId(), payload.machineType(),
                payload.totalSlots(), payload.dayRotations(), payload.nightRotations(),
                payload.active(), actorOf(principal)
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(MachineSummary.from(m));
        } catch (EntityExistsException e) {
            return problem(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{machineId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> update(@PathVariable String machineId,
                                    @RequestBody @Valid MachineUpdatePayload payload,
                                    Principal principal) {
        try {
            VcMachine m = service.update(
                machineId,
                payload.totalSlots(), payload.dayRotations(), payload.nightRotations(),
                payload.active(), actorOf(principal)
            );
            return ResponseEntity.ok(MachineSummary.from(m));
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{machineId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> deactivate(@PathVariable String machineId, Principal principal) {
        try {
            service.deactivate(machineId, actorOf(principal));
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static String actorOf(Principal principal) {
        return principal != null ? principal.getName() : "anonymousUser";
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("VC_MACHINE 관리 오류");
        return ResponseEntity.status(status).body(pd);
    }
}
