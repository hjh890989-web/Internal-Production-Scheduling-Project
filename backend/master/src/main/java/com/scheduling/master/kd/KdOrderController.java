package com.scheduling.master.kd;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import java.util.UUID;

/**
 * Sprint 12 EP-MASTER-UI BR-V13 KD_ORDER REST endpoint (TK-MASTER-4-1).
 *
 * <p>RBAC — read 는 4 role (PLANNER capacity supplement 사용 + READ_ONLY 조회),
 * write 는 IT_OPS only.
 */
@RestController
@RequestMapping("/api/v1/master/kd-order")
public class KdOrderController {

    private final KdOrderAdminService service;

    public KdOrderController(KdOrderAdminService service) {
        this.service = service;
    }

    public record KdOrderSummary(
        UUID kdOrderId, String hoseId, int orderQty, int remainingQty,
        LocalDate orderDate, String customerCode, KdOrder.Status status,
        Instant updatedAt, String updatedBy
    ) {
        public static KdOrderSummary from(KdOrder k) {
            return new KdOrderSummary(k.getKdOrderId(), k.getHoseId(), k.getOrderQty(),
                k.getRemainingQty(), k.getOrderDate(), k.getCustomerCode(), k.getStatus(),
                k.getUpdatedAt(), k.getUpdatedBy());
        }
    }

    public record KdOrderPayload(
        @NotBlank String hoseId,
        @NotNull @Min(1) Integer orderQty,
        @NotNull @PositiveOrZero Integer remainingQty,
        @NotNull LocalDate orderDate,
        String customerCode,
        KdOrder.Status status
    ) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('PLANNER','IT_OPS','READ_ONLY')")
    public ResponseEntity<List<KdOrderSummary>> list() {
        return ResponseEntity.ok(service.list().stream().map(KdOrderSummary::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> create(@RequestBody @Valid KdOrderPayload payload, Principal principal) {
        try {
            KdOrder k = service.create(payload.hoseId(), payload.orderQty(), payload.remainingQty(),
                payload.orderDate(), payload.customerCode(), payload.status(), actorOf(principal));
            return ResponseEntity.status(HttpStatus.CREATED).body(KdOrderSummary.from(k));
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> update(@PathVariable UUID id,
                                     @RequestBody @Valid KdOrderPayload payload,
                                     Principal principal) {
        try {
            KdOrder k = service.update(id, payload.hoseId(), payload.orderQty(),
                payload.remainingQty(), payload.orderDate(), payload.customerCode(),
                payload.status(), actorOf(principal));
            return ResponseEntity.ok(KdOrderSummary.from(k));
        } catch (EntityNotFoundException e) {
            return problem(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            return problem(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('IT_OPS')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            service.delete(id);
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
        pd.setTitle("KD_ORDER 관리 오류");
        return ResponseEntity.status(status).body(pd);
    }
}
