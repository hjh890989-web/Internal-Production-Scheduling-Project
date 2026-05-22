package com.scheduling.master.line;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 압출 라인 타입 마스터 — TK-14-1-1 (EP-14 ST-14-1, BR-E08).
 *
 * <p>NEW (신규 라인) priority 1순위, FORD (포드 노후) fallback. 신규 라인 사용률 ≥ 90%.
 */
@Entity
@Table(name = "line_type", schema = "master")
public class LineType {

    @Id
    @Column(name = "line_id", nullable = false, length = 10, updatable = false)
    private String lineId;

    @Column(name = "line_type", nullable = false, length = 10)
    private String lineType;

    @Column(name = "priority", nullable = false)
    private short priority;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "description")
    private String description;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected LineType() {}

    public String getLineId() { return lineId; }
    public String getLineType() { return lineType; }
    public short getPriority() { return priority; }
    public boolean isActive() { return active; }
    public String getDescription() { return description; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }

    public boolean isNew() { return "NEW".equals(lineType); }
    public boolean isFord() { return "FORD".equals(lineType); }
}
