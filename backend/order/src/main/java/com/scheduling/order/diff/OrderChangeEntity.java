package com.scheduling.order.diff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 마스터 버전 간 diff 영속 entity — TK-03-1-3 (SAD §6.2.5).
 *
 * <p>{@link RowDiff} 1건당 본 entity 1 row. field_diffs 는 JSONB string (Sprint 1 baseline —
 * Sprint 2+ Hibernate 6 JSON 타입 mapper 도입 검토).
 *
 * <p>schema: {@code app}. UPDATE 는 severity 컬럼만 허용 (ST-03-2 후속).
 * DELETE 는 부서 정책으로 차단 — REQ-NF-SEC-004 정합.
 */
@Entity
@Table(name = "order_change", schema = "app")
public class OrderChangeEntity {

    @Id
    @Column(name = "change_id", nullable = false, updatable = false)
    private UUID changeId;

    @Column(name = "tracking_id", nullable = false)
    private UUID trackingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "diff_type", nullable = false, length = 20)
    private DiffType diffType;

    @Column(name = "hose_id", nullable = false, length = 40)
    private String hoseId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "new_order_id")
    private UUID newOrderId;

    @Column(name = "old_order_id")
    private UUID oldOrderId;

    @Column(name = "field_diffs", columnDefinition = "jsonb")
    private String fieldDiffsJson;

    @Column(name = "previous_version", nullable = false)
    private int previousVersion;

    @Column(name = "new_version", nullable = false)
    private int newVersion;

    @Column(name = "severity", length = 10)
    private String severity;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected OrderChangeEntity() {}

    public OrderChangeEntity(UUID changeId, UUID trackingId, DiffType diffType,
                             String hoseId, LocalDate deliveryDate,
                             UUID newOrderId, UUID oldOrderId, String fieldDiffsJson,
                             int previousVersion, int newVersion,
                             String severity, Instant changedAt) {
        if (changedAt == null) {
            // BR-X04 — Clock 주입 강제, Instant.now() 정적 호출 금지
            throw new IllegalArgumentException("changedAt 필수 (Clock 주입)");
        }
        this.changeId = changeId;
        this.trackingId = trackingId;
        this.diffType = diffType;
        this.hoseId = hoseId;
        this.deliveryDate = deliveryDate;
        this.newOrderId = newOrderId;
        this.oldOrderId = oldOrderId;
        this.fieldDiffsJson = fieldDiffsJson;
        this.previousVersion = previousVersion;
        this.newVersion = newVersion;
        this.severity = severity;
        this.changedAt = changedAt;
    }

    public UUID getChangeId() { return changeId; }
    public UUID getTrackingId() { return trackingId; }
    public DiffType getDiffType() { return diffType; }
    public String getHoseId() { return hoseId; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public UUID getNewOrderId() { return newOrderId; }
    public UUID getOldOrderId() { return oldOrderId; }
    public String getFieldDiffsJson() { return fieldDiffsJson; }
    public int getPreviousVersion() { return previousVersion; }
    public int getNewVersion() { return newVersion; }
    public String getSeverity() { return severity; }
    public Instant getChangedAt() { return changedAt; }

    public void setSeverity(String severity) {
        // ST-03-2 (Critical 태깅) 후속 — severity 만 UPDATE 허용
        this.severity = severity;
    }
}
