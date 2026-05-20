package com.scheduling.order.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * fs close 안정성 검증 — TK-01-3-3 (REQ-FUNC-OC-015 정밀화).
 *
 * <p>SMB/NFS 환경의 "복사 중 ingest" 버그 차단. 두 단계:
 * <ol>
 *   <li><b>크기 sampling</b> — {@link WatcherConfig.Stability#getMaxSamples()} 회까지
 *       {@link WatcherConfig.Stability#getSampleInterval()} 간격으로 size 측정. 2회 연속 동일 → stable.</li>
 *   <li><b>shared lock 시도</b> — {@code FileChannel.tryLock(shared)} 가 다른 프로세스의
 *       exclusive write 점유를 검출. Linux 는 advisory lock 한계로 가짜 양성 가능.</li>
 * </ol>
 *
 * <p>실패 시 false 반환 (예외 미전파) — 다음 폴링에서 재시도.
 */
@Component
public class FileStabilityChecker {

    private static final Logger log = LoggerFactory.getLogger(FileStabilityChecker.class);

    private final WatcherConfig config;

    public FileStabilityChecker(WatcherConfig config) {
        this.config = config;
    }

    /**
     * @return true = 안전하게 ingest 가능, false = 쓰는 중 / 락 점유 / 삭제 / 오류
     */
    public boolean isStable(Path file) {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return false;
        }
        if (!sizeStabilized(file)) {
            log.debug("File size still changing: {}", file);
            return false;
        }
        if (!canAcquireSharedLock(file)) {
            log.debug("File locked by another process: {}", file);
            return false;
        }
        return true;
    }

    private boolean sizeStabilized(Path file) {
        long previousSize = -1L;
        int stableCount = 0;
        int maxSamples = Math.max(2, config.getStability().getMaxSamples());
        long intervalMs = config.getStability().getSampleInterval().toMillis();

        for (int i = 0; i < maxSamples; i++) {
            try {
                long currentSize = Files.size(file);
                if (currentSize == previousSize) {
                    stableCount++;
                    if (stableCount >= 1) {
                        return true; // 2회 연속 동일 (previous + current) → stable
                    }
                } else {
                    stableCount = 0;
                }
                previousSize = currentSize;
                if (i < maxSamples - 1) Thread.sleep(intervalMs);
            } catch (NoSuchFileException e) {
                return false;
            } catch (IOException e) {
                log.debug("size 측정 실패 {}: {}", file, e.getMessage());
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean canAcquireSharedLock(Path file) {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r");
             FileChannel channel = raf.getChannel()) {
            try (FileLock lock = channel.tryLock(0L, Long.MAX_VALUE, true)) {
                return lock != null;
            } catch (OverlappingFileLockException e) {
                return false;
            }
        } catch (IOException e) {
            log.debug("Could not acquire lock on {}: {}", file, e.getMessage());
            return false;
        }
    }
}
