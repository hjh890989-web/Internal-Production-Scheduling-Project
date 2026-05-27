package com.scheduling.order.api;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.audit.api.AuditLogService;
import com.scheduling.order.diff.OrderChangeRepository;
import com.scheduling.order.events.OrderCommittedEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 13 EP-OC-FULL ST-OC-2 — PLANNER 가 diff 검토 후 trackingId 확정/거절 (TK-OC-2-1).
 *
 * <p>RBAC: PLANNER only (BR-X05 dual-review 작성자). reason 필수 (BR-X02 audit).
 *
 * <p>commit 시 {@link OrderCommittedEvent} 발행 — Sprint 14 EP-VC-FULL listener 가 성형
 * 스케줄 입력 단계로 진입. Sprint 13 baseline 은 publisher 만 (실 Order 자동 INSERT 는 Sprint 14).
 *
 * <p>reject 는 단순 audit_log 기록 + event 미발행 (PLANNER 가 입력 파일 재요청 의도).
 */
@RestController
@RequestMapping("/api/v1/orders")
@Profile("with-infra")
public class OrderCommitController {

    private static final Logger log = LoggerFactory.getLogger(OrderCommitController.class);

    private final OrderChangeRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLog;
    private final Clock clock;

    public OrderCommitController(OrderChangeRepository repository,
                                  ApplicationEventPublisher eventPublisher,
                                  AuditLogService auditLog,
                                  Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public record DecisionPayload(@NotBlank String reason) {}

    public record CommitResponse(UUID trackingId, String decidedBy, Instant decidedAt,
                                  int affectedRows, String reason) {}

    @PostMapping("/{trackingId}/commit")
    @PreAuthorize("hasRole('PLANNER')")
    @Auditable("EP-OC-FULL 수주 import 확정 (PLANNER, BR-X05 작성자)")
    @Transactional
    public ResponseEntity<?> commit(@PathVariable UUID trackingId,
                                     @RequestBody @Valid DecisionPayload payload,
                                     Principal principal) {
        int rowCount = repository.findByTrackingIdOrderByChangedAtAsc(trackingId).size();
        if (rowCount == 0) {
            return problem(HttpStatus.NOT_FOUND,
                "trackingId 의 OrderChange row 0 — diff 미진행 또는 미존재");
        }

        String actor = actorOf(principal);
        Instant now = clock.instant();
        // BR-X02 — mutation 없는 의사결정 audit (event 만 발행하면 trigger 미발화)
        auditLog.record("order_change", trackingId.toString(), AuditLogService.Action.UPDATE,
            actor, "EP-OC-FULL 수주 import 확정 (PLANNER, BR-X05): " + payload.reason());
        eventPublisher.publishEvent(new OrderCommittedEvent(trackingId, actor, now, payload.reason()));
        log.info("EP-OC-FULL commit — tracking={} by {} rows={}", trackingId, actor, rowCount);

        return ResponseEntity.ok(new CommitResponse(trackingId, actor, now, rowCount, payload.reason()));
    }

    @PostMapping("/{trackingId}/reject")
    @PreAuthorize("hasRole('PLANNER')")
    @Auditable("EP-OC-FULL 수주 import 거절 (PLANNER)")
    @Transactional
    public ResponseEntity<?> reject(@PathVariable UUID trackingId,
                                     @RequestBody @Valid DecisionPayload payload,
                                     Principal principal) {
        int rowCount = repository.findByTrackingIdOrderByChangedAtAsc(trackingId).size();
        if (rowCount == 0) {
            return problem(HttpStatus.NOT_FOUND, "trackingId 미존재");
        }

        String actor = actorOf(principal);
        // BR-X02 — reject 도 의사결정 audit (event 미발행, audit_log 만)
        auditLog.record("order_change", trackingId.toString(), AuditLogService.Action.UPDATE,
            actor, "EP-OC-FULL 수주 import 거절 (PLANNER): " + payload.reason());
        log.warn("EP-OC-FULL reject — tracking={} by {} reason={}", trackingId, actor, payload.reason());
        return ResponseEntity.ok(new CommitResponse(trackingId, actor, clock.instant(),
            rowCount, payload.reason()));
    }

    private static String actorOf(Principal principal) {
        return principal != null ? principal.getName() : "anonymousUser";
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle("수주 import 확정 오류");
        return ResponseEntity.status(status).body(pd);
    }
}
