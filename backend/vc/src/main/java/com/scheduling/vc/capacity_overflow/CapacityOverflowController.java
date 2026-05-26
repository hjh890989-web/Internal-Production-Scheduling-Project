package com.scheduling.vc.capacity_overflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
import java.util.Map;
import java.util.UUID;

/**
 * BR-V12·V13 capa 분기 REST — Sprint 7 carry-over (REQ-FUNC-VC-022·023).
 *
 * <p>활성 조건 — 수주통합 후. UI 진입점:
 * <ul>
 *   <li>{@code POST /capacity-overflow/split} — Planner 추가 요청 큐 미리보기 (BR-V12)</li>
 *   <li>{@code POST /capacity-overflow/supplement} — Planner 1클릭 KD 잔량 보충 (BR-V13)</li>
 * </ul>
 *
 * <p>RBAC ROLE_PLANNER 단독 — 추가 요청 승인 + 보충 의사결정 권한.
 */
@RestController
@RequestMapping("/api/v1/schedule/vc/capacity-overflow")
@Profile("with-infra")
public class CapacityOverflowController {

    private final CapacityOverflowQueueService overflowService;
    private final KdSupplementService supplementService;
    private final CapacityOverflowApprovalService approvalService;

    public CapacityOverflowController(CapacityOverflowQueueService overflowService,
                                      KdSupplementService supplementService,
                                      CapacityOverflowApprovalService approvalService) {
        this.overflowService = overflowService;
        this.supplementService = supplementService;
        this.approvalService = approvalService;
    }

    public record SplitPayload(
        @NotNull Map<String, Integer> required,
        @NotNull @Min(1) Integer dailyCapa
    ) {}

    public record SupplementPayload(
        @NotNull String hoseId,
        @NotNull @Min(1) Integer shortage
    ) {}

    public record EnqueuePayload(
        @NotNull Map<String, Integer> queue
    ) {}

    public record DecisionPayload(
        @NotBlank String reason
    ) {}

    public record EnqueueResponse(List<UUID> requestIds) {}

    /** BR-V12 — capa 분리 (a) 자동 채택 + (b) Planner 승인 대기 큐. */
    @PostMapping("/split")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<CapacityOverflowQueueService.SplitResult> split(
        @RequestBody @Valid SplitPayload payload
    ) {
        CapacityOverflowQueueService.SplitResult result =
            overflowService.split(payload.required(), payload.dailyCapa());
        return ResponseEntity.ok(result);
    }

    /** BR-V13 — capa 부족 KD 잔량 보충 (동일 hose 1차 + 셋팅 그룹 2차). */
    @PostMapping("/supplement")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<KdSupplementService.SupplementResult> supplement(
        @RequestBody @Valid SupplementPayload payload,
        Principal principal
    ) {
        KdSupplementService.SupplementResult result =
            supplementService.supplement(payload.hoseId(), payload.shortage(),
                principal.getName());
        return ResponseEntity.ok(result);
    }

    /** Sprint 8 BR-V12 — split() requestQueue 영속화 (Planner 명시 등록). */
    @PostMapping("/enqueue")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<EnqueueResponse> enqueue(
        @RequestBody @Valid EnqueuePayload payload,
        Principal principal
    ) {
        List<UUID> ids = approvalService.enqueue(payload.queue(), principal.getName());
        return ResponseEntity.ok(new EnqueueResponse(ids));
    }

    /** Sprint 8 BR-V12 — Planner 1클릭 승인 (note 선택). */
    @PostMapping("/queue/{requestId}/accept")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<CapacityOverflowRequest> accept(
        @PathVariable UUID requestId,
        @RequestBody(required = false) DecisionPayload payload,
        Principal principal
    ) {
        String note = payload != null ? payload.reason() : null;
        CapacityOverflowRequest req = approvalService.accept(requestId, principal.getName(), note);
        return ResponseEntity.ok(req);
    }

    /** Sprint 8 BR-V12 — Planner 1클릭 거절 (reason 필수). */
    @PostMapping("/queue/{requestId}/reject")
    @PreAuthorize("hasRole('PLANNER')")
    public ResponseEntity<CapacityOverflowRequest> reject(
        @PathVariable UUID requestId,
        @RequestBody @Valid DecisionPayload payload,
        Principal principal
    ) {
        CapacityOverflowRequest req = approvalService.reject(requestId, principal.getName(),
            payload.reason());
        return ResponseEntity.ok(req);
    }
}
