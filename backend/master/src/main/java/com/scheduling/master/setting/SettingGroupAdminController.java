package com.scheduling.master.setting;

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
 * Sprint 21 ST-CRUD-2 — SETTING_GROUP REST endpoint.
 *
 * <p>RBAC — read 4 role, write IT_OPS only (BR-X02 audit 강제).
 * setting_group_id 범위 1~8 위반 시 400 + ProblemDetail (BR-V12·BR-V13 cross-ref).
 *
 * @see BR-X02
 * @see BR-V12
 * @see BR-V13
 */
@RestController
@RequestMapping("/api/v1/master/setting-groups")
public class SettingGroupAdminController {

    private final SettingGroupAdminService service;

    public SettingGroupAdminController(SettingGroupAdminService service) {
        this.service = service;
    }

    // -------------------------------------------------------------------------
    // DTOs (records)
    // -------------------------------------------------------------------------

    public record SettingGroupSummary(
        short groupNumber, String groupName, String description,
        boolean active, Instant updatedAt, String updatedBy
    ) {
        public static SettingGroupSummary from(SettingGroup g) {
            return new SettingGroupSummary(g.getGroupNumber(), g.getGroupName(), g.getDescription(),
                g.isActive(), g.getUpdatedAt(), g.getUpdatedBy());
        }
    }

    public record SettingGroupCreatePayload(
        @NotNull @Min(1) @Max(8) Short groupNumber,
        @NotBlank String groupName,
        String description,
        boolean active
    ) {}

    public record SettingGroupUpdatePayload(
        @NotBlank String groupName,
        boolean active
    ) {}

    public record DeactivateResponse(short groupNumber, boolean hasLinks, String message) {}

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<List<SettingGroupSummary>> list() {
        return ResponseEntity.ok(service.list().stream().map(SettingGroupSummary::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> create(@RequestBody @Valid SettingGroupCreatePayload payload,
                                    Principal principal) {
        try {
            SettingGroup g = service.create(payload.groupNumber(), payload.groupName(),
                payload.description(), payload.active, actorOf(principal));
            return ResponseEntity.status(HttpStatus.CREATED).body(SettingGroupSummary.from(g));
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (EntityExistsException e) {
            return problem(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> update(@PathVariable short groupId,
                                    @RequestBody @Valid SettingGroupUpdatePayload payload,
                                    Principal principal) {
        try {
            SettingGroup g = service.update(groupId, payload.groupName(), payload.active(),
                actorOf(principal));
            return ResponseEntity.ok(SettingGroupSummary.from(g));
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> deactivate(@PathVariable short groupId, Principal principal) {
        try {
            SettingGroupAdminService.DeactivateResult result =
                service.deactivate(groupId, actorOf(principal));
            String msg = result.hasLinks()
                ? "그룹 비활성 완료. product_setting_group 연결 row 가 존재하므로 데이터는 보존됩니다."
                : "그룹 비활성 완료.";
            return ResponseEntity.ok(new DeactivateResponse(result.groupNumber(), result.hasLinks(), msg));
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String actorOf(Principal principal) {
        return principal != null ? principal.getName() : "anonymousUser";
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("SETTING_GROUP 관리 오류");
        return ResponseEntity.status(status).body(pd);
    }
}
