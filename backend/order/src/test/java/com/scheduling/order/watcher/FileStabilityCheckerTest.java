package com.scheduling.order.watcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FileStabilityChecker 회귀 — TK-01-3-3.
 *
 * <p>sample 간격은 200ms (테스트 빠른 회귀용) — 운영 시 1s.
 */
class FileStabilityCheckerTest {

    private WatcherConfig config;
    private FileStabilityChecker checker;

    @BeforeEach
    void setUp() {
        config = new WatcherConfig();
        config.getStability().setMaxSamples(3);
        config.getStability().setSampleInterval(Duration.ofMillis(100));   // 빠른 테스트
        checker = new FileStabilityChecker(config);
    }

    @Test
    @DisplayName("쓰기 완료 파일 → stable (2회 연속 동일 size)")
    void completed_file_is_stable(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("done.xlsx");
        Files.writeString(f, "complete content");
        assertThat(checker.isStable(f)).isTrue();
    }

    @Test
    @DisplayName("쓰는 중 파일 (size 변하는) → unstable")
    void growing_file_is_unstable(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("growing.xlsx");
        Files.createFile(f);

        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    Files.writeString(f, "chunk-" + i + "-", java.nio.file.StandardOpenOption.APPEND);
                    Thread.sleep(120);   // sample interval 100ms 보다 살짝 김 → 매 sampling 시 size 변함
                }
            } catch (Exception ignored) { /* no-op */ }
        });
        writer.setDaemon(true);
        writer.start();

        try {
            assertThat(checker.isStable(f)).isFalse();
        } finally {
            writer.interrupt();
            writer.join(1000);
        }
    }

    @Test
    @DisplayName("존재하지 않는 파일 → false (예외 없음)")
    void missing_file_returns_false(@TempDir Path tmp) {
        assertThat(checker.isStable(tmp.resolve("ghost.xlsx"))).isFalse();
    }

    @Test
    @DisplayName("디렉토리 → false (regular file 아님)")
    void directory_returns_false(@TempDir Path tmp) {
        assertThat(checker.isStable(tmp)).isFalse();
    }

    @Test
    @DisplayName("빈 파일 (0 bytes) → stable (후속 단계가 거부)")
    void empty_file_is_stable(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("empty.xlsx");
        Files.createFile(f);
        assertThat(checker.isStable(f)).isTrue();
    }
}
