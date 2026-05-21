package com.scheduling.master.vc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UnschedulableReportCleanupJob 회귀 — TK-04-2-2.
 */
class UnschedulableReportCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-05-21T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));

    private UnschedulableReportCleanupJob jobFor(Path dir) {
        return new UnschedulableReportCleanupJob(dir.toString(), CLOCK);
    }

    @Test
    @DisplayName("24h 이상 된 파일만 삭제 — 최신 파일 유지")
    void deletes_only_files_older_than_24h(@TempDir Path tmp) throws Exception {
        Path old = tmp.resolve("unschedulable_20260520_080000.xlsx");
        Path fresh = tmp.resolve("unschedulable_20260521_080000.xlsx");
        Files.writeString(old, "old");
        Files.writeString(fresh, "fresh");
        // 25h 전 생성 + 1h 전 생성
        Files.setAttribute(old, "basic:creationTime",
            FileTime.from(NOW.minusSeconds(25 * 3600)));
        Files.setAttribute(fresh, "basic:creationTime",
            FileTime.from(NOW.minusSeconds(3600)));

        jobFor(tmp).cleanup();

        assertThat(Files.exists(old)).isFalse();
        assertThat(Files.exists(fresh)).isTrue();
    }

    @Test
    @DisplayName("패턴 불일치 파일 (other.xlsx) — 삭제 안 됨")
    void non_matching_pattern_preserved(@TempDir Path tmp) throws Exception {
        Path matching = tmp.resolve("unschedulable_20260520_080000.xlsx");
        Path other = tmp.resolve("other_report.xlsx");
        Files.writeString(matching, "match");
        Files.writeString(other, "other");
        Files.setAttribute(matching, "basic:creationTime",
            FileTime.from(NOW.minusSeconds(48 * 3600)));
        Files.setAttribute(other, "basic:creationTime",
            FileTime.from(NOW.minusSeconds(48 * 3600)));

        jobFor(tmp).cleanup();

        assertThat(Files.exists(matching)).isFalse();
        assertThat(Files.exists(other)).isTrue();   // 패턴 외 보존
    }

    @Test
    @DisplayName("디렉터리 미존재 → 예외 없이 noop")
    void missing_directory_noops() {
        Path missing = Path.of(System.getProperty("java.io.tmpdir"), "vc-cleanup-missing-" + System.nanoTime());
        jobFor(missing).cleanup();
        // no exception
    }

    @Test
    @DisplayName("빈 디렉터리 → noop")
    void empty_directory_noops(@TempDir Path tmp) {
        jobFor(tmp).cleanup();   // no files = no action
    }
}
