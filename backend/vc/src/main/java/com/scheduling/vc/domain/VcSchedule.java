package com.scheduling.vc.domain;

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
 * 회전 단위 성형 스케줄 — TK-05-1-1 (REQ-FUNC-VC-005).
 *
 * <p>schema: app. 1 회전 슬롯 = 1 row. UNIQUE (machine_id, slot_position, production_date, rotation_no).
 *
 * <p>{@link #asSlot()} → {@link RotationSlot} 변환 (CapacityLedger 키).
 */
@Entity
@Table(name = "vc_schedule", schema = "app")
public class VcSchedule {

    @Id
    @Column(name = "vc_schedule_id", nullable = false, updatable = false)
    private UUID vcScheduleId;

    @Column(name = "hose_id", nullable = false, length = 40)
    private String hoseId;

    @Column(name = "machine_id", nullable = false, length = 10)
    private String machineId;

    @Column(name = "slot_position", nullable = false)
    private short slotPosition;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @Column(name = "rotation_no", nullable = false)
    private short rotationNo;

    @Column(name = "angle_id", nullable = false, length = 40)
    private String angleId;

    @Column(name = "planned_qty", nullable = false)
    private int plannedQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VcScheduleStatus status;

    @Column(name = "linked_order_ids", nullable = false, columnDefinition = "text")
    private String linkedOrderIds;     // CSV — Phase 2 UUID[] 로 ALTER

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VcSchedule() {}

    public VcSchedule(UUID vcScheduleId, String hoseId, String machineId,
                      short slotPosition, LocalDate productionDate, short rotationNo,
                      String angleId, int plannedQty, VcScheduleStatus status,
                      String linkedOrderIds, Instant createdAt, Instant updatedAt) {
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("createdAt/updatedAt 필수 (Clock 주입 — BR-X04)");
        }
        if (rotationNo < 1 || rotationNo > 18) {
            throw new IllegalArgumentException("rotationNo 1..18: " + rotationNo);
        }
        this.vcScheduleId = vcScheduleId;
        this.hoseId = hoseId;
        this.machineId = machineId;
        this.slotPosition = slotPosition;
        this.productionDate = productionDate;
        this.rotationNo = rotationNo;
        this.angleId = angleId;
        this.plannedQty = plannedQty;
        this.status = status;
        this.linkedOrderIds = linkedOrderIds == null ? "" : linkedOrderIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** RotationSlot 키 변환 — CapacityLedger 통합. */
    public RotationSlot asSlot() {
        return new RotationSlot(productionDate, machineId, rotationNo, slotPosition);
    }

    public UUID getVcScheduleId() { return vcScheduleId; }
    public String getHoseId() { return hoseId; }
    public String getMachineId() { return machineId; }
    public short getSlotPosition() { return slotPosition; }
    public LocalDate getProductionDate() { return productionDate; }
    public short getRotationNo() { return rotationNo; }
    public String getAngleId() { return angleId; }
    public int getPlannedQty() { return plannedQty; }
    public VcScheduleStatus getStatus() { return status; }
    public String getLinkedOrderIds() { return linkedOrderIds; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(VcScheduleStatus status) {
        this.status = status;
    }
}
