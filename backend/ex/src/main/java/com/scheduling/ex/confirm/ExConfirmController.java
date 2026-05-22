package com.scheduling.ex.confirm;

import com.scheduling.ex.schedule.ExScheduleCandidate;
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
 * EX Confirm REST — TK-10-2-1 (EP-10 ST-10-2, BR-X01).
 */
@RestController
@RequestMapping("/api/v1/schedule/ex")
@Profile("with-infra")
public class ExConfirmController {

    private final ExCandidateConfirmationService service;

    public ExConfirmController(ExCandidateConfirmationService service) {
        this.service = service;
    }

    public record ConfirmResponse(UUID candidateId, String status, String confirmedBy) {
        static ConfirmResponse from(ExScheduleCandidate c) {
            return new ConfirmResponse(c.getExCandidateId(), c.getStatus().name(), c.getConfirmedBy());
        }
    }

    public record BatchPayload(
        @NotEmpty List<UUID> candidateIds,
        @NotNull UUID batchId
    ) {}

    public record BatchResponse(UUID batchId, int confirmedCount, String confirmedBy) {}

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<ConfirmResponse> confirm(@PathVariable UUID id, Principal principal) {
        ExScheduleCandidate c = service.confirm(id, principal.getName());
        return ResponseEntity.ok(ConfirmResponse.from(c));
    }

    @PostMapping("/confirm-batch")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<BatchResponse> confirmBatch(@RequestBody BatchPayload payload,
                                                       Principal principal) {
        int count = service.confirmBatch(payload.candidateIds(), principal.getName(), payload.batchId());
        return ResponseEntity.ok(new BatchResponse(payload.batchId(), count, principal.getName()));
    }
}
