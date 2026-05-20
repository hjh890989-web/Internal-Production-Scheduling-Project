package com.scheduling.master.vc;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 성형 슬롯 적합성 마스터 — TK-04-1-1 (SAD §6.2.2 / REF-09 47품번).
 *
 * <p>schema: master. 1 품번 = 1 row. 7 BOOLEAN 컬럼이 LP 4 슬롯 + IC 3 슬롯 적합성.
 *
 * <p>{@link #isEligibleFor(SlotPosition)} — 슬롯 위치별 O/X 조회 (TK-04-1-2 Matrix 빌드 hot path).
 * {@link #isUnschedulable()} — 모든 슬롯 X (BR-V11 unschedulable, 부분 인덱스 적중).
 * {@link #yieldPerRotation(MachineType)} — 회전당 수율 (TK-05-2-1 수율 계산).
 */
@Entity
@Table(name = "vc_constraint", schema = "master")
public class VcConstraint {

    @Id
    @Column(name = "hose_id", nullable = false, length = 40, updatable = false)
    private String hoseId;

    @Column(name = "mold_qty", nullable = false)
    private int moldQty;

    @Column(name = "composite_count", nullable = false)
    private short compositeCount;     // CHECK IN (1, 2, 3, 6) — REF-11

    // 저압 가류기 (LP) — REF-09 G·H·I·J
    @Column(name = "lp_molds_per_angle")
    private Short lpMoldsPerAngle;

    @Column(name = "lp_angle_qty")
    private Short lpAngleQty;

    @Column(name = "lp_slot_top", nullable = false)
    private boolean lpSlotTop;

    @Column(name = "lp_slot_upmid", nullable = false)
    private boolean lpSlotUpmid;

    @Column(name = "lp_slot_lowmid", nullable = false)
    private boolean lpSlotLowmid;

    @Column(name = "lp_slot_bot", nullable = false)
    private boolean lpSlotBot;

    // IC 가류기 — REF-09 M·N·O
    @Column(name = "ic_molds_per_angle")
    private Short icMoldsPerAngle;

    @Column(name = "ic_angle_qty")
    private Short icAngleQty;

    @Column(name = "ic_slot_top", nullable = false)
    private boolean icSlotTop;

    @Column(name = "ic_slot_mid", nullable = false)
    private boolean icSlotMid;

    @Column(name = "ic_slot_bot", nullable = false)
    private boolean icSlotBot;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected VcConstraint() {}

    public VcConstraint(String hoseId, int moldQty, short compositeCount,
                        Short lpMoldsPerAngle, Short lpAngleQty,
                        boolean lpSlotTop, boolean lpSlotUpmid, boolean lpSlotLowmid, boolean lpSlotBot,
                        Short icMoldsPerAngle, Short icAngleQty,
                        boolean icSlotTop, boolean icSlotMid, boolean icSlotBot,
                        Instant updatedAt, String updatedBy) {
        if (updatedAt == null) {
            // BR-X04 — Clock 주입 강제
            throw new IllegalArgumentException("updatedAt 필수 (Clock 주입 — BR-X04)");
        }
        if (compositeCount != 1 && compositeCount != 2 && compositeCount != 3 && compositeCount != 6) {
            // CHECK 제약과 동일 — application 레벨 빠른 거부 (REF-11 도메인)
            throw new IllegalArgumentException("compositeCount 는 1·2·3·6 중 하나: " + compositeCount);
        }
        this.hoseId = hoseId;
        this.moldQty = moldQty;
        this.compositeCount = compositeCount;
        this.lpMoldsPerAngle = lpMoldsPerAngle;
        this.lpAngleQty = lpAngleQty;
        this.lpSlotTop = lpSlotTop;
        this.lpSlotUpmid = lpSlotUpmid;
        this.lpSlotLowmid = lpSlotLowmid;
        this.lpSlotBot = lpSlotBot;
        this.icMoldsPerAngle = icMoldsPerAngle;
        this.icAngleQty = icAngleQty;
        this.icSlotTop = icSlotTop;
        this.icSlotMid = icSlotMid;
        this.icSlotBot = icSlotBot;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy == null ? "system:seed" : updatedBy;
    }

    /** 슬롯 위치별 O/X 조회. */
    public boolean isEligibleFor(SlotPosition pos) {
        return switch (pos) {
            case LP_TOP -> lpSlotTop;
            case LP_UPMID -> lpSlotUpmid;
            case LP_LOWMID -> lpSlotLowmid;
            case LP_BOT -> lpSlotBot;
            case IC_TOP -> icSlotTop;
            case IC_MID -> icSlotMid;
            case IC_BOT -> icSlotBot;
        };
    }

    /** 모든 슬롯이 X → BR-V11 Unschedulable. */
    public boolean isUnschedulable() {
        return !lpSlotTop && !lpSlotUpmid && !lpSlotLowmid && !lpSlotBot
            && !icSlotTop && !icSlotMid && !icSlotBot;
    }

    /** 회전당 수율 — TK-05-2-1 (compositeCount × moldsPerAngle). */
    public int yieldPerRotation(MachineType type) {
        return switch (type) {
            case LP -> compositeCount * (lpMoldsPerAngle != null ? lpMoldsPerAngle : 0);
            case IC -> compositeCount * (icMoldsPerAngle != null ? icMoldsPerAngle : 0);
        };
    }

    public String getHoseId() { return hoseId; }
    public int getMoldQty() { return moldQty; }
    public short getCompositeCount() { return compositeCount; }
    public Short getLpMoldsPerAngle() { return lpMoldsPerAngle; }
    public Short getLpAngleQty() { return lpAngleQty; }
    public boolean isLpSlotTop() { return lpSlotTop; }
    public boolean isLpSlotUpmid() { return lpSlotUpmid; }
    public boolean isLpSlotLowmid() { return lpSlotLowmid; }
    public boolean isLpSlotBot() { return lpSlotBot; }
    public Short getIcMoldsPerAngle() { return icMoldsPerAngle; }
    public Short getIcAngleQty() { return icAngleQty; }
    public boolean isIcSlotTop() { return icSlotTop; }
    public boolean isIcSlotMid() { return icSlotMid; }
    public boolean isIcSlotBot() { return icSlotBot; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
