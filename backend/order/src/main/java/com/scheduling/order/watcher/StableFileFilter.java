package com.scheduling.order.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Raw → Stable 이벤트 변환 필터 — TK-01-3-3.
 *
 * <p>{@link FolderWatcherService} / {@link ScheduledFolderPoller} 가 발행한
 * {@link RawFileDiscoveredEvent} 를 받아 {@link FileStabilityChecker} 로 검증 후
 * 안전 파일만 {@link FileDiscoveredEvent} 로 재발행 → {@link FileIngestQueueService} 가 수신.
 *
 * <p>stability 미통과 파일은 raw event 만 발행되고 정식 이벤트는 발행 안 됨 →
 * 다음 폴링 사이클에서 재평가.
 */
@Component
public class StableFileFilter {

    private static final Logger log = LoggerFactory.getLogger(StableFileFilter.class);

    private final FileStabilityChecker checker;
    private final ApplicationEventPublisher publisher;

    public StableFileFilter(FileStabilityChecker checker, ApplicationEventPublisher publisher) {
        this.checker = checker;
        this.publisher = publisher;
    }

    @EventListener
    public void onRawDiscovered(RawFileDiscoveredEvent event) {
        if (!checker.isStable(event.filePath())) {
            log.info("File not yet stable, skip: {} — will retry next poll", event.filePath());
            return;
        }
        publisher.publishEvent(new FileDiscoveredEvent(
            event.filePath(), event.sizeBytes(), event.discoveredAt(), event.source()));
        log.debug("Stability check PASSED, FileDiscoveredEvent re-published: {}", event.filePath());
    }
}
