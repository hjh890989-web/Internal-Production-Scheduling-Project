package com.scheduling.ex.export;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 압출 매트릭스 XLSX export — TK-12-2-3 (EP-12 ST-12-2, REQ-FUNC-EX-018).
 *
 * <p>{@code GET /api/v1/export/extrusion-matrix?from=2026-05-25&to=2026-05-31}
 */
@RestController
@RequestMapping("/api/v1/export")
@Profile("with-infra")
public class ExMatrixExportController {

    private final ExtrusionMatrixExporter exporter;

    public ExMatrixExportController(ExtrusionMatrixExporter exporter) {
        this.exporter = exporter;
    }

    @GetMapping("/extrusion-matrix")
    @PreAuthorize("hasAnyRole('PLANNER','IT_OPS')")
    public ResponseEntity<byte[]> exportMatrix(
        @RequestParam("from") LocalDate from,
        @RequestParam("to") LocalDate to
    ) {
        byte[] body = exporter.exportMatrix(from, to);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"EX_MATRIX.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(body);
    }
}
