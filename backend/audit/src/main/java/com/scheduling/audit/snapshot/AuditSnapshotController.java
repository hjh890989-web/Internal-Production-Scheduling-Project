package com.scheduling.audit.snapshot;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * EP-19 임의 시점 마스터 복원 REST — TK-19-1-1 (REQ-FUNC-OC-014).
 *
 * <p>{@code GET /api/v1/audit/snapshot?table=&rowPk=&at=2026-05-22T00:00:00Z}
 * → 해당 시점 row 상태 (JSON payload + 마지막 action + capturedAt)
 *
 * <p>{@code GET /api/v1/audit/timeline?table=&rowPk=} → 전체 audit 시간순.
 *
 * <p>RBAC IT_OPS + READ_ONLY (forensic 조회 — Planner 별도 confirm 흐름).
 */
@RestController
@RequestMapping("/api/v1/audit")
@Profile("with-infra")
public class AuditSnapshotController {

    private final AuditSnapshotService service;

    public AuditSnapshotController(AuditSnapshotService service) {
        this.service = service;
    }

    @GetMapping("/snapshot")
    @PreAuthorize("hasAnyRole('IT_OPS','READ_ONLY','PLANNER')")
    public AuditSnapshotService.SnapshotResult snapshot(
        @RequestParam("table") String table,
        @RequestParam("rowPk") String rowPk,
        @RequestParam("at") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant at
    ) {
        return service.reconstructAt(table, rowPk, at);
    }

    @GetMapping("/timeline")
    @PreAuthorize("hasAnyRole('IT_OPS','READ_ONLY','PLANNER')")
    public List<Map<String, Object>> timeline(
        @RequestParam("table") String table,
        @RequestParam("rowPk") String rowPk
    ) {
        return service.timeline(table, rowPk);
    }
}
