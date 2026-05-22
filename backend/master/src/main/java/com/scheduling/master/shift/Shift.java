package com.scheduling.master.shift;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * 압출 4-shift 마스터 — TK-08-1-1 (EP-08 ST-08-1, BR-E03·E04).
 *
 * <p>{@code master.shift} 테이블. {@code effective_min} 은 PG GENERATED ALWAYS AS
 * {@code FLOOR(nominal_min * efficiency)} STORED — 효율 변경 시 자동 재계산.
 *
 * <p>4 row seed:
 * <ul>
 *   <li>DAY_EARLY (주간전반) 08:00~12:00 — 240 × 0.75 = 180</li>
 *   <li>DAY_LATE (주간후반) 13:00~17:00 — 240 × 0.75 = 180</li>
 *   <li>NIGHT_EARLY (야간전반) 20:00~00:00 — 240 × 0.75 = 180</li>
 *   <li>NIGHT_LATE (야간후반) 01:00~05:00 — 240 × 0.75 = 180</li>
 * </ul>
 */
@Entity
@Table(name = "shift", schema = "master")
public class Shift {

    @Id
    @Column(name = "shift_code", nullable = false, length = 20, updatable = false)
    private String shiftCode;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "nominal_min", nullable = false)
    private int nominalMin;

    @Column(name = "efficiency", nullable = false, precision = 4, scale = 3)
    private BigDecimal efficiency;

    @Column(name = "effective_min", nullable = false, insertable = false, updatable = false)
    private int effectiveMin;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected Shift() {}

    public String getShiftCode() { return shiftCode; }
    public String getName() { return name; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getNominalMin() { return nominalMin; }
    public BigDecimal getEfficiency() { return efficiency; }
    public int getEffectiveMin() { return effectiveMin; }
    public short getSortOrder() { return sortOrder; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
