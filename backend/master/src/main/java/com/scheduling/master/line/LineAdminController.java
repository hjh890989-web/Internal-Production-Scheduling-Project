package com.scheduling.master.line;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
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
import java.util.Set;

/**
 * Sprint 21 ST-CRUD-4 LINE_TYPE 관리 REST 엔드포인트.
 *
 * <p>RBAC — read 는 4 role, write 는 IT_OPS only (BR-X02 audit 강제).
 * 비활성 처리: active=false toggle (schedule 의존 row 보존).
 *
 * @see BR-X02
 */
@RestController
@RequestMapping("/api/v1/master/lines")
public class LineAdminController {

    private final LineAdminService service;

    public LineAdminController(LineAdminService service) {
        this.service = service;
    }

    // -------------------------------------------------------------------------
    // Records
    // -------------------------------------------------------------------------

    public record LineSummary(
        String lineId, String lineType, short priority,
        boolean active, String description, Instant updatedAt, String updatedBy
    ) {
        public static LineSummary from(LineType lt) {
            return new LineSummary(lt.getLineId(), lt.getLineType(), lt.getPriority(),
                lt.isActive(), lt.getDescription(), lt.getUpdatedAt(), lt.getUpdatedBy());
        }
    }

    public record LinePayload(
        @NotBlank String lineId,
        @NotBlank String lineType,
        @NotNull Short priority,
        String description
    ) {}

    public record ProductMappingPayload(
        @NotNull Set<String> hoseIds
    ) {}

    public record CompatibilitySummary(
        String hoseId, String lineId, boolean fordOnly, Instant updatedAt, String updatedBy
    ) {
        public static CompatibilitySummary from(LineProductCompatibility c) {
            return new CompatibilitySummary(c.getHoseId(), c.getLineId(), c.isFordOnly(),
                c.getUpdatedAt(), c.getUpdatedBy());
        }
    }

    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<List<LineSummary>> list() {
        return ResponseEntity.ok(service.list().stream().map(LineSummary::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> create(@RequestBody @Valid LinePayload payload, Principal principal) {
        try {
            LineType lt = service.create(payload.lineId(), payload.lineType(),
                payload.priority(), payload.description(), actorOf(principal));
            return ResponseEntity.status(HttpStatus.CREATED).body(LineSummary.from(lt));
        } catch (EntityExistsException e) {
            return problem(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/{lineCode}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> update(@PathVariable String lineCode,
                                    @RequestBody @Valid LinePayload payload,
                                    Principal principal) {
        try {
            LineType lt = service.update(lineCode, payload.lineType(),
                payload.priority(), payload.description(), actorOf(principal));
            return ResponseEntity.ok(LineSummary.from(lt));
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{lineCode}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> deactivate(@PathVariable String lineCode, Principal principal) {
        try {
            service.deactivate(lineCode, actorOf(principal));
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PutMapping("/{lineCode}/products")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> replaceProducts(@PathVariable String lineCode,
                                              @RequestBody @Valid ProductMappingPayload payload,
                                              Principal principal) {
        try {
            List<CompatibilitySummary> result = service
                .replaceProducts(lineCode, payload.hoseIds(), actorOf(principal))
                .stream().map(CompatibilitySummary::from).toList();
            return ResponseEntity.ok(result);
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
        pd.setTitle("LINE_TYPE 관리 오류");
        return ResponseEntity.status(status).body(pd);
    }
}
