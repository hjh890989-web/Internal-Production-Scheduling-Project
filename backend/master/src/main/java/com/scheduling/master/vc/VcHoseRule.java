package com.scheduling.master.vc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 품번 단위 운영 룰 — TK-21-2-1 (EP-21 ST-21-2, BR-V14 / REQ-FUNC-VC-024).
 *
 * <p>{@code master.vc_hose_rule}. 47품번 중 룰 보유 품번만 row (sparse).
 *
 * <ul>
 *   <li>{@code machinePin}: 고정 가류기 (28422-08HA0 → LP-01). NULL = 자유 배치</li>
 *   <li>{@code maxConcurrentSlots}: 동시 다중 슬롯 상한 (BR-V14, default 99)</li>
 *   <li>{@code sideLock}: 좌/우 강제 (28422-2M800 → RIGHT, NULL = K/L 만 적용)</li>
 *   <li>{@code lpOnly}: IC 사용 금지 (LP-first 강화)</li>
 * </ul>
 */
@Entity
@Table(name = "vc_hose_rule", schema = "master")
public class VcHoseRule {

    @Id
    @Column(name = "hose_id", nullable = false, length = 40, updatable = false)
    private String hoseId;

    @Column(name = "machine_pin", length = 10)
    private String machinePin;

    @Column(name = "max_concurrent_slots", nullable = false)
    private int maxConcurrentSlots = 99;

    @Column(name = "side_lock", length = 5)
    private String sideLock;

    @Column(name = "lp_only", nullable = false)
    private boolean lpOnly;

    @Column(name = "notes")
    private String notes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected VcHoseRule() {}

    public VcHoseRule(String hoseId, String machinePin, int maxConcurrentSlots,
                      String sideLock, boolean lpOnly, String notes,
                      Instant updatedAt, String updatedBy) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt 필수 (BR-X04 Clock 주입)");
        }
        if (maxConcurrentSlots < 1 || maxConcurrentSlots > 99) {
            throw new IllegalArgumentException("maxConcurrentSlots 는 1~99: " + maxConcurrentSlots);
        }
        if (sideLock != null && !"LEFT".equals(sideLock) && !"RIGHT".equals(sideLock)) {
            throw new IllegalArgumentException("sideLock 는 LEFT/RIGHT 또는 NULL: " + sideLock);
        }
        this.hoseId = hoseId;
        this.machinePin = machinePin;
        this.maxConcurrentSlots = maxConcurrentSlots;
        this.sideLock = sideLock;
        this.lpOnly = lpOnly;
        this.notes = notes;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy == null ? "system:seed" : updatedBy;
    }

    public boolean hasMachinePin() { return machinePin != null; }
    public boolean hasSideLock() { return sideLock != null; }

    public String getHoseId() { return hoseId; }
    public String getMachinePin() { return machinePin; }
    public int getMaxConcurrentSlots() { return maxConcurrentSlots; }
    public String getSideLock() { return sideLock; }
    public boolean isLpOnly() { return lpOnly; }
    public String getNotes() { return notes; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
