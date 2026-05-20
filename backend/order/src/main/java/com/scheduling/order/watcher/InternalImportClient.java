package com.scheduling.order.watcher;

import com.scheduling.order.import_.ImportOrchestratorService;
import com.scheduling.order.import_.ImportTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * watcher → 기존 import 파이프 시스템 호출 — TK-01-3-2.
 *
 * <p>OrderImportController 의 HTTP 경유 없이 동일한 {@code ImportOrchestratorService} 를 직접 호출.
 * actor = "system:watcher" (사용자 직접 업로드와 audit 구분).
 *
 * <p>{@code @Profile("with-infra")} — ImportTrackingService (Redis) 의존.
 */
@Component
@Profile("with-infra")
public class InternalImportClient {

    private static final Logger log = LoggerFactory.getLogger(InternalImportClient.class);

    private final ImportOrchestratorService orchestrator;
    private final ImportTrackingService tracking;

    public InternalImportClient(ImportOrchestratorService orchestrator, ImportTrackingService tracking) {
        this.orchestrator = orchestrator;
        this.tracking = tracking;
    }

    /**
     * 단일 파일 ingest — 새 trackingId 발급 + processAsync 호출.
     *
     * @return 새 trackingId (PickedFile.trackingId 컬럼에 저장)
     */
    public UUID importFile(Path file) throws IOException {
        UUID trackingId = UUID.randomUUID();
        PathMultipartFile mf = new PathMultipartFile(file);

        tracking.markStarted(trackingId, List.of(file.getFileName().toString()));
        orchestrator.processAsync(trackingId, List.of(mf));

        log.info("Internal import triggered: trackingId={} file={}", trackingId, file);
        return trackingId;
    }
}
