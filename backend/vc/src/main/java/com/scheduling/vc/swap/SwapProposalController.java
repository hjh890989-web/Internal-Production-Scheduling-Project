package com.scheduling.vc.swap;

import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * VC swap proposal REST — TK-15-2-2 (EP-15 ST-15-2, REQ-FUNC-VC-018).
 *
 * <p>endpoints:
 * <ul>
 *   <li>{@code POST /proposals} STK_USER — 제안 등록</li>
 *   <li>{@code POST /proposals/{id}/accept} PLANNER — 1클릭 수용 + atomic swap</li>
 *   <li>{@code POST /proposals/{id}/reject} PLANNER — 거절 + 사유</li>
 *   <li>{@code GET /proposals?status=PROPOSED} — 처리 대기 목록</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/schedule/vc/proposals")
@Profile("with-infra")
public class SwapProposalController {

    private final SwapProposalService service;
    private final SwapProposalRepository repository;

    public SwapProposalController(SwapProposalService service,
                                  SwapProposalRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    public record ProposePayload(@NotNull UUID sourceRowId,
                                  @NotNull UUID targetRowId,
                                  String reason) {}

    public record ResolvePayload(String note) {}

    public record ProposalResponse(UUID proposalId, UUID sourceRowId, UUID targetRowId,
                                    String proposedBy, Instant proposedAt, String status,
                                    String resolvedBy, Instant resolvedAt) {
        static ProposalResponse from(SwapProposal p) {
            return new ProposalResponse(
                p.getProposalId(), p.getSourceRowId(), p.getTargetRowId(),
                p.getProposedBy(), p.getProposedAt(), p.getStatus().name(),
                p.getResolvedBy(), p.getResolvedAt());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STK_USER','PLANNER')")
    public ResponseEntity<ProposalResponse> propose(@RequestBody ProposePayload payload,
                                                     Principal principal) {
        SwapProposal p = service.propose(
            payload.sourceRowId(), payload.targetRowId(),
            principal.getName(), payload.reason());
        return ResponseEntity.ok(ProposalResponse.from(p));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<ProposalResponse> accept(@PathVariable UUID id,
                                                    @RequestBody ResolvePayload payload,
                                                    Principal principal) {
        SwapProposal p = service.accept(id, principal.getName(), payload.note());
        return ResponseEntity.ok(ProposalResponse.from(p));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<ProposalResponse> reject(@PathVariable UUID id,
                                                    @RequestBody ResolvePayload payload,
                                                    Principal principal) {
        SwapProposal p = service.reject(id, principal.getName(), payload.note());
        return ResponseEntity.ok(ProposalResponse.from(p));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STK_USER','PLANNER','IT_OPS','READ_ONLY')")
    public List<ProposalResponse> list(@RequestParam(required = false) SwapStatus status) {
        List<SwapProposal> rows = status == null
            ? repository.findAll()
            : repository.findByStatus(status);
        return rows.stream().map(ProposalResponse::from).toList();
    }
}
