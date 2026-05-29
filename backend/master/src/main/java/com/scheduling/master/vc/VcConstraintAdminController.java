package com.scheduling.master.vc;

import com.scheduling.master.vc.VcConstraintAdminService.VcConstraintPayload;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Sprint 21 ST-CRUD-3 — VcConstraint 합금형 슬롯 적합성 관리 REST (IT_OPS 전용 write).
 *
 * <p>GET  /api/v1/master/vc-constraints — 4 role read (BR-V14 조회)
 * <p>POST /api/v1/master/vc-constraints — IT_OPS 신규 (BR-V14 + BR-X02)
 * <p>PUT  /api/v1/master/vc-constraints/{hoseId} — IT_OPS 수정 (BR-V14 + BR-X02)
 *
 * <p>기존 {@code HoseRuleController} (/api/v1/master/vc-hose-rule) 와 path 충돌 없음.
 *
 * @see BR-V14
 * @see BR-X02
 */
@RestController
@RequestMapping("/api/v1/master/vc-constraints")
public class VcConstraintAdminController {

    private final VcConstraintAdminService service;

    public VcConstraintAdminController(VcConstraintAdminService service) {
        this.service = service;
    }

    // ----------------------------------------------------------------------------------
    // Response record
    // ----------------------------------------------------------------------------------

    public record VcConstraintResponse(
        String hoseId,
        short compositeCount,
        int lpMoldQty,
        boolean slot1, boolean slot2, boolean slot3, boolean slot4,
        boolean slot5, boolean slot6, boolean slot7,
        Instant updatedAt,
        String updatedBy
    ) {
        public static VcConstraintResponse from(VcConstraint v) {
            return new VcConstraintResponse(
                v.getHoseId(),
                v.getCompositeCount(),
                v.getMoldQty(),
                v.isLpSlotTop(), v.isLpSlotUpmid(), v.isLpSlotLowmid(), v.isLpSlotBot(),
                v.isIcSlotTop(), v.isIcSlotMid(), v.isIcSlotBot(),
                v.getUpdatedAt(),
                v.getUpdatedBy()
            );
        }
    }

    // ----------------------------------------------------------------------------------
    // Request record — @Valid bean validation
    // ----------------------------------------------------------------------------------

    public record VcConstraintRequest(
        @NotBlank String hoseId,
        @NotNull Short compositeCount,
        @NotNull @Min(0) Integer lpMoldQty,
        @NotNull @Min(0) Integer icMoldQty,
        @NotNull Boolean slot1,
        @NotNull Boolean slot2,
        @NotNull Boolean slot3,
        @NotNull Boolean slot4,
        @NotNull Boolean slot5,
        @NotNull Boolean slot6,
        @NotNull Boolean slot7
    ) {
        VcConstraintPayload toPayload() {
            return new VcConstraintPayload(
                hoseId, compositeCount, lpMoldQty, icMoldQty,
                slot1, slot2, slot3, slot4, slot5, slot6, slot7
            );
        }
    }

    // ----------------------------------------------------------------------------------
    // Endpoints
    // ----------------------------------------------------------------------------------

    /**
     * 전체 조회 — PLANNER, STK_USER, IT_OPS, READ_ONLY.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<List<VcConstraintResponse>> list() {
        List<VcConstraintResponse> result = service.list().stream()
            .map(VcConstraintResponse::from)
            .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * 신규 생성 — IT_OPS only.
     *
     * @see BR-V14 compositeCount ∈ {1, 2, 3, 6}
     * @see BR-X02 audit_log 강제
     */
    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> create(@RequestBody @Valid VcConstraintRequest request,
                                    Principal principal) {
        try {
            VcConstraint saved = service.create(request.toPayload(), actorOf(principal));
            return ResponseEntity.status(HttpStatus.CREATED).body(VcConstraintResponse.from(saved));
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage(), "BR-V14 위반");
        } catch (IllegalStateException e) {
            return problem(HttpStatus.CONFLICT, e.getMessage(), "VcConstraint 관리 오류");
        }
    }

    /**
     * 수정 — IT_OPS only.
     *
     * @see BR-V14 compositeCount ∈ {1, 2, 3, 6}
     * @see BR-X02 audit_log 강제
     */
    @PutMapping("/{hoseId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> update(@PathVariable String hoseId,
                                    @RequestBody @Valid VcConstraintRequest request,
                                    Principal principal) {
        try {
            VcConstraint saved = service.update(hoseId, request.toPayload(), actorOf(principal));
            return ResponseEntity.ok(VcConstraintResponse.from(saved));
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage(), "BR-V14 위반");
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage(), "VcConstraint 관리 오류");
        }
    }

    // ----------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------

    private static String actorOf(Principal principal) {
        return principal != null ? principal.getName() : "anonymousUser";
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, String title) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return ResponseEntity.status(status).body(pd);
    }
}
