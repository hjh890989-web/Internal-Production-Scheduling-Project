package com.scheduling.order.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 60s 폴링 fallback — TK-01-3-1.
 *
 * <p>{@link FolderWatcherService} 이벤트 누락 (NFS/SMB 환경) 대비.
 * 동일 파일 중복 발행 방지를 위해 in-memory dedup 캐시 — 5분 주기 cleanup.
 * Sprint 1 baseline 은 단일 인스턴스 가정 (Shedlock 도입은 Phase 2).
 *
 * <p>{@link WatcherConfig#isEnabled()}=false 시 NoOp.
 */
@Component
public class ScheduledFolderPoller {

    private static final Logger log = LoggerFactory.getLogger(ScheduledFolderPoller.class);

    private final WatcherConfig config;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;
    private final Set<String> recentlyDiscovered = ConcurrentHashMap.newKeySet();

    public ScheduledFolderPoller(WatcherConfig config, ApplicationEventPublisher publisher, Clock clock) {
        this.config = config;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${scheduling.watcher.poll-interval:PT60S}")
    public void poll() {
        if (!config.isEnabled()) return;

        Path watch = config.getWatchFolder();
        if (!Files.isDirectory(watch)) {
            log.warn("Watch folder does not exist: {}", watch);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(watch, config.getFilePattern())) {
            for (Path file : stream) {
                String key = file.toString();
                if (recentlyDiscovered.contains(key)) continue;

                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    if (!attrs.isRegularFile()) continue;

                    publisher.publishEvent(new RawFileDiscoveredEvent(
                        file, attrs.size(), Instant.now(clock),
                        FileDiscoveredEvent.Source.SCHEDULED_POLL));
                    recentlyDiscovered.add(key);
                    log.debug("ScheduledPoll detected: {} ({} bytes)", file, attrs.size());
                } catch (IOException e) {
                    log.warn("Could not read attributes of {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Polling failed for {}", watch, e);
        }
    }

    /** 5분 주기 dedup 캐시 정리 — 파일 삭제·재추가 사이클 허용. */
    @Scheduled(fixedDelay = 5L * 60L * 1000L)
    public void cleanupCache() {
        if (!recentlyDiscovered.isEmpty()) {
            int size = recentlyDiscovered.size();
            recentlyDiscovered.clear();
            log.debug("Poller dedup cache cleared ({} entries)", size);
        }
    }

    /** 테스트 hook — dedup 캐시 강제 정리. */
    void clearDedupCacheForTest() {
        recentlyDiscovered.clear();
    }
}
