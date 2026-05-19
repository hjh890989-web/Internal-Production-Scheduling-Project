package com.scheduling.order.import_;

import com.scheduling.order.parser.ClassificationResult;
import com.scheduling.order.parser.ExcelParseException;
import com.scheduling.order.parser.ExcelParserService;
import com.scheduling.order.parser.ParsedWorkbook;
import com.scheduling.order.parser.SourceClassifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Excel import 비동기 오케스트레이터 — TK-01-1-3.
 *
 * <p>OrderImportController 의 호출:
 * <pre>
 *   controller → trackingService.markStarted → orchestrator.processAsync (background)
 *   background:
 *     - parse 워크북 (ExcelParserService)
 *     - 분류 (SourceClassifierService)
 *     - 추적 상태 update
 *     - ST-01-2 매핑 단계 위임 (Sprint 1+ 이벤트 발행)
 * </pre>
 *
 * <p>Sprint 0 baseline 은 PARSED 단계까지 — ST-01-2 활성 후 MAPPED/COMMITTED 추가.
 */
@Service
public class ImportOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ImportOrchestratorService.class);

    private final ExcelParserService parser;
    private final SourceClassifierService classifier;
    private final ImportTrackingService tracking;

    public ImportOrchestratorService(
        ExcelParserService parser,
        SourceClassifierService classifier,
        ImportTrackingService tracking
    ) {
        this.parser = parser;
        this.classifier = classifier;
        this.tracking = tracking;
    }

    @Async(AsyncConfig.EXCEL_IMPORT_EXECUTOR)
    public void processAsync(UUID trackingId, List<MultipartFile> files) {
        log.info("Async import started: {} ({} files)", trackingId, files.size());
        tracking.update(trackingId, ImportStatus.PARSING, null);

        try {
            for (MultipartFile file : files) {
                processSingleFile(trackingId, file);
            }
            tracking.update(trackingId, ImportStatus.PARSED, null);
            log.info("Async import PARSED: {}", trackingId);
            // ST-01-2 (스키마 매핑) 위임 — 이벤트 발행 (Sprint 1+ TK-01-2-* 에서 활성)
        } catch (Exception e) {
            log.error("Import failed for tracking {}", trackingId, e);
            tracking.update(trackingId, ImportStatus.FAILED, e.getMessage());
        }
    }

    private void processSingleFile(UUID trackingId, MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.xlsx";
        try {
            ParsedWorkbook wb = parser.parse(filename, file.getInputStream(), file.getSize());
            ClassificationResult result = classifier.classify(wb);
            tracking.recordClassification(trackingId, filename, result);
            log.info("Parsed {} → {} (confidence={})", filename, result.sourceType(), result.confidence());
        } catch (IOException | ExcelParseException e) {
            log.warn("Parse failed for {}: {}", filename, e.getMessage());
            tracking.recordClassification(trackingId, filename,
                new com.scheduling.order.parser.ClassificationResult(
                    com.scheduling.order.parser.SourceType.UNRECOGNIZED, 0.0, java.util.Map.of()));
        }
    }
}
