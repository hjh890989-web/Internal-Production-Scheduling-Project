package com.scheduling.order.watcher;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Clock;
import java.time.Instant;

/**
 * NIO WatchService 기반 실시간 folder watcher — TK-01-3-1.
 *
 * <p>{@link WatcherConfig#isEnabled()}=true 일 때만 daemon thread 시작.
 * fs 이벤트 (ENTRY_CREATE / ENTRY_MODIFY) 수신 → {@link RawFileDiscoveredEvent} 발행.
 * TK-01-3-3 {@link StableFileFilter} 가 검증 후 정식 {@link FileDiscoveredEvent} 재발행.
 *
 * <p>장애 격리 — 시작 실패해도 다른 service 영향 없음 (try/catch + 로그).
 * Linux inotify 기반 — Windows 는 일부 지연. NFS/SMB 누락은 {@link ScheduledFolderPoller}
 * fallback 폴링으로 보완.
 */
@Service
@EnableConfigurationProperties(WatcherConfig.class)
public class FolderWatcherService {

    private static final Logger log = LoggerFactory.getLogger(FolderWatcherService.class);

    private final WatcherConfig config;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    private WatchService watchService;
    private Thread watcherThread;
    private volatile boolean running = false;

    public FolderWatcherService(WatcherConfig config, ApplicationEventPublisher publisher, Clock clock) {
        this.config = config;
        this.publisher = publisher;
        this.clock = clock;
    }

    @PostConstruct
    public void start() {
        if (!config.isEnabled()) {
            log.info("Folder watcher disabled (scheduling.watcher.enabled=false)");
            return;
        }
        try {
            Files.createDirectories(config.getWatchFolder());
            watchService = FileSystems.getDefault().newWatchService();
            config.getWatchFolder().register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            running = true;
            watcherThread = new Thread(this::runLoop, "folder-watcher");
            watcherThread.setDaemon(true);
            watcherThread.start();
            log.info("Folder watcher started on {}", config.getWatchFolder());
        } catch (IOException e) {
            log.error("Failed to start folder watcher — fallback Scheduled poller only", e);
            // 장애 격리 — 예외 전파 X
        }
    }

    private void runLoop() {
        while (running) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                Path filename = (Path) event.context();
                if (filename == null) continue;
                Path full = config.getWatchFolder().resolve(filename);
                if (!matchesPattern(filename)) continue;

                long size;
                try {
                    size = Files.size(full);
                } catch (IOException e) {
                    log.warn("Could not stat file {} — will retry via Scheduled poller: {}",
                        full, e.getMessage());
                    continue;
                }

                publisher.publishEvent(new RawFileDiscoveredEvent(
                    full, size, Instant.now(clock),
                    FileDiscoveredEvent.Source.WATCH_SERVICE));
                log.debug("WatchService detected: {} ({} bytes)", full, size);
            }

            if (!key.reset()) {
                log.warn("Watch key invalidated — watcher stopping");
                break;
            }
        }
    }

    private boolean matchesPattern(Path filename) {
        PathMatcher matcher = FileSystems.getDefault()
            .getPathMatcher("glob:" + config.getFilePattern());
        return matcher.matches(filename);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (watchService != null) {
            try { watchService.close(); } catch (IOException ignored) { /* no-op */ }
        }
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        log.info("Folder watcher stopped");
    }
}
