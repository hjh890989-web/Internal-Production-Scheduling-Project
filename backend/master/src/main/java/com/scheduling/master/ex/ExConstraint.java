package com.scheduling.master.ex;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 압출 마스터 — TK-21-5-1 (Sprint 2 최소 spec/angle) + TK-08-2-1 (Sprint 3 풀 확장 speed/length).
 *
 * <p>{@code master.ex_constraint}. V016 (Sprint 2) + V019 (Sprint 3).
 */
@Entity
@Table(name = "ex_constraint", schema = "master")
public class ExConstraint {

    @Id
    @Column(name = "hose_id", nullable = false, length = 40, updatable = false)
    private String hoseId;

    @Column(name = "spec_value")
    private Integer specValue;

    @Column(name = "angle_count", nullable = false)
    private int angleCount = 1;

    @Column(name = "speed_m_per_min", precision = 7, scale = 3)
    private BigDecimal speedMPerMin;

    @Column(name = "length_mm")
    private Integer lengthMm;

    @Column(name = "die_code", length = 20)
    private String dieCode;

    @Column(name = "line_code", length = 10)
    private String lineCode;

    @Column(name = "notes")
    private String notes;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected ExConstraint() {}

    public String getHoseId() { return hoseId; }
    public Integer getSpecValue() { return specValue; }
    public int getAngleCount() { return angleCount; }
    public BigDecimal getSpeedMPerMin() { return speedMPerMin; }
    public Integer getLengthMm() { return lengthMm; }
    public String getDieCode() { return dieCode; }
    public String getLineCode() { return lineCode; }
    public String getNotes() { return notes; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
