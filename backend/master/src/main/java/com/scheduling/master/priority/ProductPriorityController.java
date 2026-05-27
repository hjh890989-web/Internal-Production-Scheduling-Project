package com.scheduling.master.priority;

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
import java.time.LocalDate;
import java.util.List;

/**
 * Sprint 12 EP-MASTER-UI BR-V12 PRODUCT_PRIORITY REST endpoint (TK-MASTER-3-1).
 *
 * <p>RBAC — read 는 4 role (PLANNER capacity-queue 사용 + READ_ONLY 조회),
 * write 는 IT_OPS only (BR-X02 audit 강제, BR-X05 dual-review).
 */
@RestController
@RequestMapping("/api/v1/master/product-priority")
public class ProductPriorityController {

    private final ProductPriorityAdminService service;

    public ProductPriorityController(ProductPriorityAdminService service) {
        this.service = service;
    }

    public record PrioritySummary(
        String hoseId, short priorityRank, String rationale,
        LocalDate effectiveFrom, LocalDate effectiveTo,
        Instant updatedAt, String updatedBy
    ) {
        public static PrioritySummary from(ProductPriority p) {
            return new PrioritySummary(p.getHoseId(), p.getPriorityRank(), p.getRationale(),
                p.getEffectiveFrom(), p.getEffectiveTo(), p.getUpdatedAt(), p.getUpdatedBy());
        }
    }

    public record PriorityPayload(
        @NotBlank String hoseId,
        @NotNull @Min(1) @Max(99) Short priorityRank,
        String rationale,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo
    ) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','IT_OPS','READ_ONLY')")
    public ResponseEntity<List<PrioritySummary>> list() {
        return ResponseEntity.ok(service.list().stream().map(PrioritySummary::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> create(@RequestBody @Valid PriorityPayload payload, Principal principal) {
        try {
            ProductPriority p = service.create(payload.hoseId(), payload.priorityRank(),
                payload.rationale(), payload.effectiveFrom(), payload.effectiveTo(),
                actorOf(principal));
            return ResponseEntity.status(HttpStatus.CREATED).body(PrioritySummary.from(p));
        } catch (EntityExistsException e) {
            return problem(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PutMapping("/{hoseId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> update(@PathVariable String hoseId,
                                     @RequestBody @Valid PriorityPayload payload,
                                     Principal principal) {
        try {
            ProductPriority p = service.update(hoseId, payload.priorityRank(),
                payload.rationale(), payload.effectiveFrom(), payload.effectiveTo(),
                actorOf(principal));
            return ResponseEntity.ok(PrioritySummary.from(p));
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{hoseId}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> delete(@PathVariable String hoseId) {
        try {
            service.delete(hoseId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private static String actorOf(Principal principal) {
        return principal != null ? principal.getName() : "anonymousUser";
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("PRODUCT_PRIORITY 관리 오류");
        return ResponseEntity.status(status).body(pd);
    }
}
