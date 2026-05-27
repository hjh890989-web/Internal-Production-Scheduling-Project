package com.scheduling.security.auth;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Sprint 12 EP-MASTER-UI 사용자 관리 REST endpoint (TK-MASTER-2-1, IT_OPS 권한).
 *
 * <p>RBAC: 모든 endpoint `IT_OPS` only. PLANNER/STK_USER/READ_ONLY → 403.
 *
 * <p>응답 — list/create 는 {@link UserSummary} (pin_hash 제외, NFR-SEC-005).
 * resetPin/unlock/delete 는 200 + 빈 body. 충돌 시 409, 미존재 시 404.
 */
@RestController
@RequestMapping("/api/v1/master/user")
public class UserAdminController {

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    public record UserSummary(
        String employeeId,
        AppUser.Role role,
        short failedAttempts,
        Instant lockedUntil,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static UserSummary from(AppUser u) {
            return new UserSummary(u.getEmployeeId(), u.getRole(), u.getFailedAttempts(),
                u.getLockedUntil(), u.getCreatedAt(), u.getUpdatedAt());
        }
    }

    public record CreatePayload(
        @NotNull @Pattern(regexp = "^[0-9]{8}$", message = "사번 8자리 숫자") String employeeId,
        @NotNull @Pattern(regexp = "^[0-9]{4}$", message = "PIN 4자리 숫자") String pin,
        @NotNull AppUser.Role role
    ) {}

    public record ResetPinPayload(
        @NotNull @Pattern(regexp = "^[0-9]{4}$", message = "새 PIN 4자리 숫자") String newPin
    ) {}

    @GetMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<List<UserSummary>> list() {
        return ResponseEntity.ok(service.list().stream().map(UserSummary::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> create(@RequestBody @Valid CreatePayload payload) {
        try {
            AppUser u = service.create(payload.employeeId(), payload.pin(), payload.role());
            return ResponseEntity.status(HttpStatus.CREATED).body(UserSummary.from(u));
        } catch (EntityExistsException e) {
            return problem(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{employeeId}/reset-pin")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> resetPin(@PathVariable String employeeId,
                                       @RequestBody @Valid ResetPinPayload payload) {
        try {
            service.resetPin(employeeId, payload.newPin());
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{employeeId}/unlock")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> unlock(@PathVariable String employeeId) {
        try {
            service.unlock(employeeId);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> delete(@PathVariable String employeeId) {
        try {
            service.delete(employeeId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("사용자 관리 오류");
        return ResponseEntity.status(status).body(pd);
    }
}
