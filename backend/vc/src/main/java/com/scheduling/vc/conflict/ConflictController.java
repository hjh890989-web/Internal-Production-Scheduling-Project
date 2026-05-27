package com.scheduling.vc.conflict;

import com.scheduling.vc.allocator.AllocationConflict;
import com.scheduling.vc.allocator.AllocationContext;
import com.scheduling.vc.allocator.AllocationResult;
import com.scheduling.vc.allocator.GreedyRotationAllocator;
import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.CapacityLedgerBuilder;
import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.required.OrderInput;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 충돌 리포트 REST — TK-VC15-1-3 (REQ-FUNC-VC-015).
 *
 * <p>{@code GET /api/v1/schedule/conflicts?from=&to=} — 인증된 모든 role.
 * 호라이즌 내 GreedyAllocator 시뮬레이션 → 충돌 분류 + ≥ 3 distinct 대안 enrich.
 *
 * <p>p95 ≤ 1초 목표 (REQ-FUNC-VC-015). 본 endpoint 는 stateless — 매 호출 fresh
 * allocation. 향후 캐싱 검토 (Phase 2+).
 */
@RestController
@RequestMapping("/api/v1/schedule")
@Profile("with-infra")
public class ConflictController {

    private final ConflictReportService reportService;
    private final GreedyRotationAllocator allocator;
    private final CapacityLedgerBuilder ledgerBuilder;
    private final WorkingCalendar calendar;

    public ConflictController(ConflictReportService reportService,
                               GreedyRotationAllocator allocator,
                               CapacityLedgerBuilder ledgerBuilder,
                               WorkingCalendar calendar) {
        this.reportService = reportService;
        this.allocator = allocator;
        this.ledgerBuilder = ledgerBuilder;
        this.calendar = calendar;
    }

    /**
     * 호라이즌 내 충돌 dry-run + 리포트.
     *
     * <p>Q_required 는 외부 입력 (UI) 또는 향후 confirmed schedule 기반 — 본 endpoint
     * 는 sandbox 시뮬레이션 (empty Q_required → 0 conflict). Phase 2+ 에서 실제
     * Q_required 입력 통합.
     */
    @GetMapping("/conflicts")
    @PreAuthorize("hasAnyRole('PLANNER','STK_USER','IT_OPS','READ_ONLY')")
    public ResponseEntity<ConflictReport> getConflicts(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().build();
        }
        List<LocalDate> workingDays = calendar.workingDaysInRange(from, to);
        if (workingDays.isEmpty()) {
            return ResponseEntity.ok(reportService.buildReport(List.of()));
        }
        CapacityLedger ledger = ledgerBuilder.build(from, to);
        Map<String, Integer> qRequired = Map.of();
        Map<String, List<OrderInput>> orders = new HashMap<>();
        AllocationContext ctx = new AllocationContext(qRequired, orders, ledger, workingDays);
        AllocationResult result = allocator.allocate(ctx);

        List<AllocationConflict> conflicts = result.conflicts();
        return ResponseEntity.ok(reportService.buildReport(conflicts));
    }

    /**
     * 직접 conflict list 를 받아 리포트 빌드 — POST 시뮬레이션용. Phase 2+.
     */
    @SuppressWarnings("unused")
    private ConflictReport sample(UUID scheduleId) {
        return reportService.buildReport(List.of());
    }
}
