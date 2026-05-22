package com.scheduling.order.export;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Excel 역-export 엔드포인트 — TK-12-1-3 + TK-12-2-3 (EP-12).
 *
 * <p>{@code GET /api/v1/export/master} → 통합 마스터 XLSX
 *
 * <p>RBAC PLANNER + IT_OPS — Sprint 4 baseline.
 */
@RestController
@RequestMapping("/api/v1/export")
@Profile("with-infra")
public class ExportController {

    private final MasterExcelExporter masterExporter;

    public ExportController(MasterExcelExporter masterExporter) {
        this.masterExporter = masterExporter;
    }

    @GetMapping("/master")
    @PreAuthorize("hasAnyRole('PLANNER','IT_OPS')")
    public ResponseEntity<byte[]> exportMaster() {
        byte[] body = masterExporter.exportMaster();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"MASTER.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }
}
