package com.scheduling.order.api;

import com.scheduling.order.import_.ImportOrchestratorService;
import com.scheduling.order.import_.ImportStatus;
import com.scheduling.order.import_.ImportTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * 수주 엑셀 import 엔드포인트 — TK-01-1-3.
 *
 * <p>REQ-FUNC-OC-001: 최대 3 워크북 multipart 동시 업로드, 추적 ID 발급 ≤2초.
 *
 * <p>RBAC (@PreAuthorize) 는 Sprint 1 ST-30-2 활성 후 추가. 현재는 Sprint 0 baseline —
 * 모든 인증된 요청 허용 ({@link com.scheduling.SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderImportController {

    private static final Logger log = LoggerFactory.getLogger(OrderImportController.class);
    private static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final int MAX_FILES = 3;

    private final ImportOrchestratorService orchestrator;
    private final ImportTrackingService tracking;
    private final Clock clock;

    public OrderImportController(
        ImportOrchestratorService orchestrator,
        ImportTrackingService tracking,
        Clock clock
    ) {
        this.orchestrator = orchestrator;
        this.tracking = tracking;
        this.clock = clock;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ImportResponse importWorkbooks(@RequestParam("files") List<MultipartFile> files) {
        validateFiles(files);

        UUID trackingId = UUID.randomUUID();
        List<String> filenames = files.stream()
            .map(f -> f.getOriginalFilename() != null ? f.getOriginalFilename() : "unknown.xlsx")
            .toList();
        tracking.markStarted(trackingId, filenames);
        orchestrator.processAsync(trackingId, files);

        log.info("Accepted import {} ({} files)", trackingId, files.size());
        return new ImportResponse(
            trackingId,
            ImportStatus.QUEUED,
            "/api/v1/orders/import/" + trackingId,
            filenames,
            Instant.now(clock)
        );
    }

    @GetMapping("/import/{trackingId}")
    public ImportStatusResponse status(@PathVariable UUID trackingId) {
        return tracking.get(trackingId);
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least 1 file required");
        }
        if (files.size() > MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Maximum %d files per request".formatted(MAX_FILES));
        }
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file rejected");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File %s exceeds 20MB".formatted(file.getOriginalFilename()));
            }
            String name = file.getOriginalFilename();
            if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only .xlsx supported: " + name);
            }
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NoSuchElementException e) {
        return e.getMessage();
    }
}
