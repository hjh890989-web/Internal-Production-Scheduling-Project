package com.scheduling.order.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 파일 ingest 큐 + audit — TK-01-3-2 (BR-X02 / REQ-FUNC-OC-015).
 *
 * <p>흐름:
 * <ol>
 *   <li>{@link FileDiscoveredEvent} 수신</li>
 *   <li>SHA-256 해시 계산 → {@code app.picked_file} INSERT (QUEUED)</li>
 *   <li>24h 윈도우 내 동일 해시 INGESTED/PROCESSING 존재 → SKIPPED_DUPLICATE 마킹</li>
 *   <li>중복 X → {@link InternalImportClient#importFile} 호출 → INGESTED + trackingId 기록</li>
 *   <li>예외 → FAILED + errorMessage 기록</li>
 * </ol>
 *
 * <p>{@code @Profile("with-infra")} — JPA + Redis 의존 (PickedFileRepository + InternalImportClient).
 */
@Service
@Profile("with-infra")
public class FileIngestQueueService {

    private static final Logger log = LoggerFactory.getLogger(FileIngestQueueService.class);
    private static final String ACTOR = "system:watcher";

    private final PickedFileRepository repository;
    private final FileHashCalculator hasher;
    private final ObjectProvider<InternalImportClient> importClientProvider;
    private final WatcherConfig config;
    private final Clock clock;

    public FileIngestQueueService(
        PickedFileRepository repository,
        FileHashCalculator hasher,
        ObjectProvider<InternalImportClient> importClientProvider,
        WatcherConfig config,
        Clock clock
    ) {
        this.repository = repository;
        this.hasher = hasher;
        this.importClientProvider = importClientProvider;
        this.config = config;
        this.clock = clock;
    }

    /**
     * stability 통과한 정식 {@link FileDiscoveredEvent} 수신.
     * raw 이벤트는 {@link StableFileFilter} 가 검증 후 변환.
     */
    @EventListener
    @Transactional
    public void onFileDiscovered(FileDiscoveredEvent event) {
        Path path = event.filePath();
        Instant now = Instant.now(clock);
        log.info("Picked file: {} (size={}, via={})", path, event.sizeBytes(), event.source());

        String hash;
        try {
            hash = hasher.sha256(path);
        } catch (IllegalStateException e) {
            persistFailed(event, "error:" + UUID.randomUUID(), e.getMessage(), now);
            return;
        }

        Instant threshold = now.minus(Duration.ofHours(config.getDuplicateWindowHours()));
        boolean duplicate = repository.existsRecentSuccessByHash(hash, threshold);

        PickedFile picked = new PickedFile(
            UUID.randomUUID(),
            path.toString(),
            path.getFileName().toString(),
            hash,
            event.sizeBytes(),
            event.discoveredAt(),
            event.source(),
            duplicate ? PickedFileStatus.SKIPPED_DUPLICATE : PickedFileStatus.QUEUED,
            ACTOR
        );
        repository.save(picked);

        if (duplicate) {
            log.info("Skip duplicate file: {} (hash={})", path, hash);
            return;
        }

        // INGESTED 전이 — 실 import 호출
        picked.markProcessing();
        repository.save(picked);

        InternalImportClient client = importClientProvider.getIfAvailable();
        if (client == null) {
            picked.markFailed("InternalImportClient bean 부재 (DEV context)", now);
            repository.save(picked);
            log.warn("InternalImportClient unavailable — picked_file marked FAILED: {}", path);
            return;
        }

        try {
            UUID trackingId = client.importFile(path);
            picked.markIngested(trackingId, Instant.now(clock));
            repository.save(picked);
            log.info("Ingested {} → trackingId={}", path, trackingId);
        } catch (IOException | RuntimeException e) {
            picked.markFailed(e.getMessage(), Instant.now(clock));
            repository.save(picked);
            log.error("Ingest failed for {}: {}", path, e.getMessage(), e);
        }
    }

    private void persistFailed(FileDiscoveredEvent event, String hashFallback, String error, Instant at) {
        PickedFile picked = new PickedFile(
            UUID.randomUUID(),
            event.filePath().toString(),
            event.filePath().getFileName().toString(),
            hashFallback,
            event.sizeBytes(),
            event.discoveredAt(),
            event.source(),
            PickedFileStatus.FAILED,
            ACTOR
        );
        picked.markFailed(error, at);
        repository.save(picked);
        log.error("Hash failed — picked_file FAILED: {} ({})", event.filePath(), error);
    }
}
