package com.scheduling.master.vc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 가류기 마스터 — TK-05-1-1 (BR-V05).
 *
 * <p>저압 (LP) 4대 × 8슬롯 + IC 1대 × 6슬롯 = 5 row.
 * 회전수 — 주간 + 야간 = 18 회전/대/일 (BR-V04).
 *
 * <p>schema: master. Sprint 1+ master_admin role 만 변경 가능 (Phase 2 RBAC 강화).
 */
@Entity
@Table(name = "vc_machine", schema = "master")
public class VcMachine {

    @Id
    @Column(name = "machine_id", nullable = false, length = 10, updatable = false)
    private String machineId;          // LP-01 ~ LP-04, IC-01

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_type", nullable = false, length = 2, updatable = false)
    private MachineType machineType;

    @Column(name = "total_slots", nullable = false)
    private short totalSlots;          // LP=8, IC=6 (CHECK 제약)

    @Column(name = "day_rotations", nullable = false)
    private short dayRotations;        // BR-V04 주간 기본 8

    @Column(name = "night_rotations", nullable = false)
    private short nightRotations;      // BR-V04 야간 기본 10

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected VcMachine() {}

    public VcMachine(String machineId, MachineType machineType,
                     short totalSlots, short dayRotations, short nightRotations,
                     boolean active, Instant updatedAt, String updatedBy) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt 필수 (Clock 주입 — BR-X04)");
        }
        if (machineType == MachineType.LP && totalSlots != 8) {
            throw new IllegalArgumentException("LP machine totalSlots 는 8: " + totalSlots);
        }
        if (machineType == MachineType.IC && totalSlots != 6) {
            throw new IllegalArgumentException("IC machine totalSlots 는 6: " + totalSlots);
        }
        this.machineId = machineId;
        this.machineType = machineType;
        this.totalSlots = totalSlots;
        this.dayRotations = dayRotations;
        this.nightRotations = nightRotations;
        this.active = active;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy == null ? "system:seed" : updatedBy;
    }

    /** BR-V04: 주간 + 야간 = 18 회전/일. */
    public int totalRotationsPerDay() {
        return dayRotations + nightRotations;
    }

    /** 일일 capa = 18 회전 × totalSlots. LP=144, IC=108. */
    public int dailyCapacity() {
        return totalRotationsPerDay() * totalSlots;
    }

    public String getMachineId() { return machineId; }
    public MachineType getMachineType() { return machineType; }
    public short getTotalSlots() { return totalSlots; }
    public short getDayRotations() { return dayRotations; }
    public short getNightRotations() { return nightRotations; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
