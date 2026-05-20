package com.scheduling.order.watcher;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * folder watcher ingest audit + 중복 처리 — TK-01-3-2.
 *
 * <p>1 파일 발견 = 1 row. {@link PickedFileStatus} 가 라이프사이클 제어.
 * actor = "system:watcher" (사용자 직접 업로드와 구분).
 *
 * <p>schema: app (operational queue). audit 강화 (event sourcing) 는 Phase 2.
 */
@Entity
@Table(name = "picked_file", schema = "app")
public class PickedFile {

    @Id
    @Column(name = "picked_file_id", nullable = false, updatable = false)
    private UUID pickedFileId;

    @Column(name = "file_path", nullable = false, updatable = false, columnDefinition = "text")
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 255, updatable = false)
    private String fileName;

    @Column(name = "file_hash", nullable = false, length = 64, updatable = false)
    private String fileHash;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "discovered_at", nullable = false, updatable = false)
    private Instant discoveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "discovered_via", nullable = false, length = 20, updatable = false)
    private FileDiscoveredEvent.Source discoveredVia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PickedFileStatus status;

    @Column(name = "tracking_id")
    private UUID trackingId;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "actor", nullable = false, length = 40, updatable = false)
    private String actor;

    protected PickedFile() {}

    public PickedFile(UUID pickedFileId, String filePath, String fileName, String fileHash,
                      long sizeBytes, Instant discoveredAt,
                      FileDiscoveredEvent.Source discoveredVia, PickedFileStatus status,
                      String actor) {
        if (discoveredAt == null) {
            throw new IllegalArgumentException("discoveredAt 필수 (Clock 주입 — BR-X04)");
        }
        this.pickedFileId = pickedFileId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.sizeBytes = sizeBytes;
        this.discoveredAt = discoveredAt;
        this.discoveredVia = discoveredVia;
        this.status = status;
        this.actor = actor == null ? "system:watcher" : actor;
    }

    public void markProcessing() {
        this.status = PickedFileStatus.PROCESSING;
    }

    public void markIngested(UUID trackingId, Instant at) {
        this.trackingId = trackingId;
        this.processedAt = at;
        this.status = PickedFileStatus.INGESTED;
    }

    public void markFailed(String errorMessage, Instant at) {
        this.errorMessage = errorMessage;
        this.processedAt = at;
        this.status = PickedFileStatus.FAILED;
    }

    public UUID getPickedFileId() { return pickedFileId; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public String getFileHash() { return fileHash; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getDiscoveredAt() { return discoveredAt; }
    public FileDiscoveredEvent.Source getDiscoveredVia() { return discoveredVia; }
    public PickedFileStatus getStatus() { return status; }
    public UUID getTrackingId() { return trackingId; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getProcessedAt() { return processedAt; }
    public String getActor() { return actor; }
}
