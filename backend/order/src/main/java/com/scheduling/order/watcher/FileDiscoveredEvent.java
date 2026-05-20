package com.scheduling.order.watcher;

import java.nio.file.Path;
import java.time.Instant;

/**
 * 파일 발견 도메인 이벤트 — TK-01-3-1.
 *
 * <p>{@link FolderWatcherService} (실시간 NIO) 또는 {@link ScheduledFolderPoller} (60s 폴백) 가
 * 발행 → {@link FileIngestQueueService} 가 수신.
 *
 * <p>TK-01-3-3 도입 후 — 본 이벤트는 {@link StableFileFilter} 가 stability 검증 통과한 파일만
 * 재발행. raw event 는 {@link RawFileDiscoveredEvent} 로 분리.
 *
 * @param filePath       절대 경로
 * @param sizeBytes      파일 크기 (stability 확정 시점)
 * @param discoveredAt   발견 시각 (KST — Clock 주입, BR-X04)
 * @param source         발견 경로
 */
public record FileDiscoveredEvent(
    Path filePath,
    long sizeBytes,
    Instant discoveredAt,
    Source source
) {
    public enum Source { WATCH_SERVICE, SCHEDULED_POLL }
}
