package com.scheduling.master.vc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Unschedulable Excel 리포트 24h 자동 정리 — TK-04-2-2.
 *
 * <p>{@code scheduling.vc.unschedulable.report-dir} (기본 /tmp/scheduling/vc-reports) 안의
 * {@code unschedulable_*.xlsx} 파일을 매시간 스캔 → 생성 시각 ≥ 24h 인 파일 삭제.
 *
 * <p>{@code @Profile("with-infra")} — 운영 환경에서만 정리 작업 활성. DEV 컨텍스트는 보존.
 */
@Component
@Profile("with-infra")
public class UnschedulableReportCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(UnschedulableReportCleanupJob.class);
    private static final Duration RETENTION = Duration.ofHours(24);
    private static final String FILE_PATTERN = "unschedulable_*.xlsx";

    private final Path reportDir;
    private final Clock clock;

    public UnschedulableReportCleanupJob(
        @Value("${scheduling.vc.unschedulable.report-dir:/tmp/scheduling/vc-reports}") String reportDir,
        Clock clock
    ) {
        this.reportDir = Paths.get(reportDir);
        this.clock = clock;
    }

    /** 매 1시간 — 24h 이상 된 unschedulable Excel 파일 삭제. */
    @Scheduled(fixedDelayString = "${scheduling.vc.unschedulable.cleanup-interval-ms:3600000}",
               initialDelayString = "${scheduling.vc.unschedulable.cleanup-initial-delay-ms:3600000}")
    public void cleanup() {
        if (!Files.isDirectory(reportDir)) {
            log.debug("Unschedulable report dir 미존재: {}", reportDir);
            return;
        }
        Instant cutoff = Instant.now(clock).minus(RETENTION);
        int deleted = 0;
        int kept = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(reportDir, FILE_PATTERN)) {
            for (Path file : stream) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attrs.creationTime().toInstant().isBefore(cutoff)) {
                        Files.delete(file);
                        deleted++;
                    } else {
                        kept++;
                    }
                } catch (IOException e) {
                    log.warn("정리 실패: {} ({})", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Cleanup 디렉터리 스캔 실패: {}", reportDir, e);
        }
        if (deleted > 0 || kept > 0) {
            log.info("Unschedulable report cleanup — deleted={}, kept={}, cutoff={}",
                deleted, kept, cutoff);
        }
    }
}
