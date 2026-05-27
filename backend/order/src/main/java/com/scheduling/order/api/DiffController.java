package com.scheduling.order.api;

import com.scheduling.order.diff.DiffType;
import com.scheduling.order.diff.OrderChangeEntity;
import com.scheduling.order.diff.OrderChangeRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Sprint 13 EP-OC-FULL ST-OC-1 — OrderChange diff read endpoint (TK-OC-1-1).
 *
 * <p>RBAC: PLANNER + IT_OPS + READ_ONLY read (STK_USER 는 수주 diff 미관여 — 403).
 * Sprint 1~3 누적 자산 (DiffEngineService + DiffPersistenceService) 의 결과를 노출.
 *
 * <p>severity 분류 (BR-O02) — Sprint 1~3 SeverityClassifier 가 채움. NULL 인 경우 미분류
 * (legacy data 또는 ST-03-2 미진행 sample).
 */
@RestController
@RequestMapping("/api/v1/orders")
@Profile("with-infra")
public class DiffController {

    private final OrderChangeRepository repository;

    public DiffController(OrderChangeRepository repository) {
        this.repository = repository;
    }

    public record RowDiffSummary(
        UUID changeId, DiffType diffType, String hoseId, LocalDate deliveryDate,
        UUID newOrderId, UUID oldOrderId, String fieldDiffs,
        int previousVersion, int newVersion, String severity, Instant changedAt
    ) {
        public static RowDiffSummary from(OrderChangeEntity e) {
            return new RowDiffSummary(e.getChangeId(), e.getDiffType(), e.getHoseId(),
                e.getDeliveryDate(), e.getNewOrderId(), e.getOldOrderId(),
                e.getFieldDiffsJson(), e.getPreviousVersion(), e.getNewVersion(),
                e.getSeverity(), e.getChangedAt());
        }
    }

    public record DiffSummaryResponse(
        UUID trackingId, int totalRows, int criticalCount, int importantCount,
        int standardCount, int unclassifiedCount, List<RowDiffSummary> rows
    ) {}

    @GetMapping("/{trackingId}/diff")
    @PreAuthorize("hasAnyRole('PLANNER','IT_OPS','READ_ONLY')")
    public ResponseEntity<DiffSummaryResponse> get(@PathVariable UUID trackingId) {
        List<OrderChangeEntity> changes = repository.findByTrackingIdOrderByChangedAtAsc(trackingId);
        if (changes.isEmpty()) {
            // 빈 결과 도 200 — trackingId 의 import 가 PARSED 상태 (diff 단계 미진입) 일 수도
            return ResponseEntity.ok(new DiffSummaryResponse(trackingId, 0, 0, 0, 0, 0, List.of()));
        }

        int critical = 0, important = 0, standard = 0, unclassified = 0;
        for (OrderChangeEntity c : changes) {
            String s = c.getSeverity();
            if (s == null) unclassified++;
            else switch (s) {
                case "CRITICAL" -> critical++;
                case "IMPORTANT" -> important++;
                case "STANDARD" -> standard++;
                default -> unclassified++;
            }
        }

        List<RowDiffSummary> rows = changes.stream().map(RowDiffSummary::from).toList();
        return ResponseEntity.ok(new DiffSummaryResponse(trackingId, changes.size(),
            critical, important, standard, unclassified, rows));
    }
}
