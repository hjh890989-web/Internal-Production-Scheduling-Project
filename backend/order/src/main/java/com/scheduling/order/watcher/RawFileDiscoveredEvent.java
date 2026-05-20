package com.scheduling.order.watcher;

import java.nio.file.Path;
import java.time.Instant;

/**
 * stability 미검증 raw 이벤트 — TK-01-3-3.
 *
 * <p>{@link FolderWatcherService} / {@link ScheduledFolderPoller} 가 발행 →
 * {@link StableFileFilter} 가 {@link FileStabilityChecker} 로 검증 후
 * {@link FileDiscoveredEvent} 로 변환·재발행.
 *
 * <p>쓰는 중 파일·락 점유 파일은 raw 이벤트만 발행되고 정식 이벤트는 발행 안 됨 →
 * 다음 폴링 사이클에서 재평가.
 */
public record RawFileDiscoveredEvent(
    Path filePath,
    long sizeBytes,
    Instant discoveredAt,
    FileDiscoveredEvent.Source source
) {
}
