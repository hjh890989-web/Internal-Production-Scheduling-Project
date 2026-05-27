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

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by", length = 40)
    private String confirmedBy;

    @Column(name = "override_reason", columnDefinition = "text")
    private String overrideReason;

    @Column(name = "override_by", length = 40)
    private String overrideBy;

    @Column(name = "created_by", length = 40)
    private String createdBy;

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

    public Instant getConfirmedAt() { return confirmedAt; }
    public String getConfirmedBy() { return confirmedBy; }
    public String getOverrideReason() { return overrideReason; }
    public String getOverrideBy() { return overrideBy; }
    public String getCreatedBy() { return createdBy; }

    /**
     * Sprint 16 BR-X05 — INSERT actor 식별. Allocator/Listener 가 row 생성 직후 호출.
     * 한 번 set 된 createdBy 는 immutable (재호출 차단).
     */
    public void assignCreatedBy(String createdBy) {
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("createdBy 필수 (BR-X05 dual-review)");
        }
        if (this.createdBy != null && !this.createdBy.isBlank()) {
            throw new IllegalStateException(
                "BR-X05 createdBy immutable — 이미 " + this.createdBy + " 로 set");
        }
        this.createdBy = createdBy;
    }

    /**
     * BR-V07 일중 앵글 교체 override — TK-13-4-1 (EP-13 ST-13-4).
     *
     * <p>DB trigger {@code trg_vc_intra_day_lock} 가 reason/by 누락 시 reject.
     * 본 메서드는 도메인 invariant 만 강제 (reason blank 차단).
     */
    public void applyOverride(String reason, String overrideActor, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("BR-V07 override_reason 강제 필수");
        }
        if (overrideActor == null || overrideActor.isBlank()) {
            throw new IllegalArgumentException("BR-V07 override_by 강제 필수");
        }
        this.overrideReason = reason;
        this.overrideBy = overrideActor;
        this.updatedAt = now;
    }

    /**
     * BR-X01 — Planner 확정 (CANDIDATE → CONFIRMED, TK-10-1-2).
     *
     * <p>DB trigger 가 audit 필드 누락 시 reject — 본 메서드가 set 후 update.
     */
    public void confirm(String plannerId, Instant now) {
        if (status != VcScheduleStatus.CANDIDATE) {
            throw new IllegalStateException(
                "BR-X01 confirm 전이는 CANDIDATE 에서만: 현재 " + status);
        }
        if (plannerId == null || plannerId.isBlank()) {
            throw new IllegalArgumentException("plannerId 필수 (BR-X01 RBAC)");
        }
        this.status = VcScheduleStatus.CONFIRMED;
        this.confirmedAt = now;
        this.confirmedBy = plannerId;
        this.updatedAt = now;
    }
}
