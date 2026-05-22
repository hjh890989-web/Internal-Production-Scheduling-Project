package com.scheduling.vc.confirm;

import com.scheduling.vc.domain.VcSchedule;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * VC Confirm REST — TK-10-1-2 (EP-10 ST-10-1, BR-X01).
 *
 * <p>{@code POST /api/v1/schedule/vc/{id}/confirm} — Planner role 단건 확정.
 * {@code POST /api/v1/schedule/vc/confirm-batch} — Planner role 다건 확정 + 이벤트 발행.
 */
@RestController
@RequestMapping("/api/v1/schedule/vc")
@Profile("with-infra")
public class VcConfirmController {

    private final VcScheduleConfirmationService service;

    public VcConfirmController(VcScheduleConfirmationService service) {
        this.service = service;
    }

    public record ConfirmResponse(UUID vcScheduleId, String status, String confirmedBy) {
        static ConfirmResponse from(VcSchedule s) {
            return new ConfirmResponse(s.getVcScheduleId(), s.getStatus().name(), s.getConfirmedBy());
        }
    }

    public record BatchPayload(
        @NotEmpty List<UUID> scheduleIds,
        @NotNull UUID batchId
    ) {}

    public record BatchResponse(UUID batchId, int confirmedCount, String confirmedBy) {}

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<ConfirmResponse> confirm(@PathVariable UUID id, Principal principal) {
        VcSchedule s = service.confirm(id, principal.getName());
        return ResponseEntity.ok(ConfirmResponse.from(s));
    }

    @PostMapping("/confirm-batch")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<BatchResponse> confirmBatch(@RequestBody BatchPayload payload,
                                                       Principal principal) {
        int count = service.confirmBatch(payload.scheduleIds(), principal.getName(), payload.batchId());
        return ResponseEntity.ok(new BatchResponse(payload.batchId(), count, principal.getName()));
    }
}
