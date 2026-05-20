package com.scheduling.order.watcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * 폴더 watcher 설정 — TK-01-3-1 (REQ-FUNC-OC-015).
 *
 * <p>{@code scheduling.watcher.*} prefix. <b>기본 OFF</b> (Could 등급) — 운영 진입 시
 * 별도 PR 로 {@code enabled=true} + watch-folder 경로 설정.
 *
 * <pre>
 * scheduling:
 *   watcher:
 *     enabled: false
 *     watch-folder: /data/inbox
 *     poll-interval: PT60S
 *     file-pattern: "*.xlsx"
 *     duplicate-window-hours: 24
 *     stability:
 *       max-samples: 3
 *       sample-interval: PT1S
 * </pre>
 */
@ConfigurationProperties(prefix = "scheduling.watcher")
public class WatcherConfig {

    private boolean enabled = false;
    private Path watchFolder = Paths.get("/data/inbox");
    private Duration pollInterval = Duration.ofSeconds(60);
    private String filePattern = "*.xlsx";
    private int duplicateWindowHours = 24;
    private Stability stability = new Stability();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Path getWatchFolder() { return watchFolder; }
    public void setWatchFolder(Path watchFolder) { this.watchFolder = watchFolder; }

    public Duration getPollInterval() { return pollInterval; }
    public void setPollInterval(Duration pollInterval) { this.pollInterval = pollInterval; }

    public String getFilePattern() { return filePattern; }
    public void setFilePattern(String filePattern) { this.filePattern = filePattern; }

    public int getDuplicateWindowHours() { return duplicateWindowHours; }
    public void setDuplicateWindowHours(int duplicateWindowHours) {
        this.duplicateWindowHours = duplicateWindowHours;
    }

    public Stability getStability() { return stability; }
    public void setStability(Stability stability) { this.stability = stability; }

    /** TK-01-3-3 — fs close 안정성 sampling 파라미터. */
    public static class Stability {
        private int maxSamples = 3;
        private Duration sampleInterval = Duration.ofSeconds(1);

        public int getMaxSamples() { return maxSamples; }
        public void setMaxSamples(int maxSamples) { this.maxSamples = maxSamples; }

        public Duration getSampleInterval() { return sampleInterval; }
        public void setSampleInterval(Duration sampleInterval) {
            this.sampleInterval = sampleInterval;
        }
    }
}
