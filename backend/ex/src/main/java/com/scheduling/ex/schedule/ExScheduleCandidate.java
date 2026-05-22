package com.scheduling.ex.schedule;

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
 * 압출 후보 스케줄 — TK-07-1-2 (EP-07 ST-07-1).
 *
 * <p>{@code app.ex_schedule_candidate} 1:1 ↔ VcSchedule. VC 확정 이벤트 수신 시 생성,
 * EP-08 (yield) / EP-09 (그룹핑) / EP-EX11 (검증) 단계에서 status 전환.
 *
 * <p>BR-E01 — {@code extrusionDeadline = vcProductionDate − 1 working day}.
 */
@Entity
@Table(name = "ex_schedule_candidate", schema = "app")
public class ExScheduleCandidate {

    @Id
    @Column(name = "ex_candidate_id", nullable = false, updatable = false)
    private UUID exCandidateId;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "hose_id", nullable = false, length = 40)
    private String hoseId;

    @Column(name = "vc_row_id", nullable = false)
    private UUID vcRowId;

    @Column(name = "vc_production_date", nullable = false)
    private LocalDate vcProductionDate;

    @Column(name = "extrusion_deadline", nullable = false)
    private LocalDate extrusionDeadline;

    @Column(name = "vc_yield", nullable = false)
    private int vcYield;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CandidateStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by", length = 40)
    private String confirmedBy;

    protected ExScheduleCandidate() {}

    public ExScheduleCandidate(UUID exCandidateId, UUID scheduleId, String hoseId,
                                UUID vcRowId, LocalDate vcProductionDate,
                                LocalDate extrusionDeadline, int vcYield,
                                CandidateStatus status,
                                Instant createdAt, Instant updatedAt) {
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("createdAt/updatedAt 필수 (BR-X04 Clock 주입)");
        }
        if (vcYield < 0) {
            throw new IllegalArgumentException("vcYield ≥ 0: " + vcYield);
        }
        if (extrusionDeadline.isAfter(vcProductionDate)) {
            throw new IllegalArgumentException(
                "extrusionDeadline %s 가 vcProductionDate %s 보다 늦음 (BR-E01)"
                    .formatted(extrusionDeadline, vcProductionDate));
        }
        this.exCandidateId = exCandidateId;
        this.scheduleId = scheduleId;
        this.hoseId = hoseId;
        this.vcRowId = vcRowId;
        this.vcProductionDate = vcProductionDate;
        this.extrusionDeadline = extrusionDeadline;
        this.vcYield = vcYield;
        this.status = status == null ? CandidateStatus.PENDING : status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getExCandidateId() { return exCandidateId; }
    public UUID getScheduleId() { return scheduleId; }
    public String getHoseId() { return hoseId; }
    public UUID getVcRowId() { return vcRowId; }
    public LocalDate getVcProductionDate() { return vcProductionDate; }
    public LocalDate getExtrusionDeadline() { return extrusionDeadline; }
    public int getVcYield() { return vcYield; }
    public CandidateStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void transitionTo(CandidateStatus next, Instant now) {
        this.status = next;
        this.updatedAt = now;
    }

    /**
     * BR-E11 partial replan — VC 변경 반영 + status PENDING 재전환.
     *
     * <p>TK-EX13-1-3 (EP-EX13) Sprint 4 정식 활성. CONFIRMED 상태는 호출자가 사전 차단.
     */
    public void applyVcChange(int newYield, LocalDate newDeadline,
                              LocalDate newVcDate, Instant now) {
        if (newYield < 0) {
            throw new IllegalArgumentException("newYield ≥ 0: " + newYield);
        }
        if (newDeadline == null || newVcDate == null) {
            throw new IllegalArgumentException("newDeadline / newVcDate 필수");
        }
        if (newDeadline.isAfter(newVcDate)) {
            throw new IllegalArgumentException(
                "newDeadline %s 가 newVcDate %s 보다 늦음 (BR-E01)"
                    .formatted(newDeadline, newVcDate));
        }
        this.vcYield = newYield;
        this.extrusionDeadline = newDeadline;
        this.vcProductionDate = newVcDate;
        this.status = CandidateStatus.PENDING;
        this.updatedAt = now;
    }

    public Instant getConfirmedAt() { return confirmedAt; }
    public String getConfirmedBy() { return confirmedBy; }

    /**
     * BR-X01 — Planner 확정 (SCHEDULED → CONFIRMED, TK-10-2-1).
     */
    public void confirm(String plannerId, Instant now) {
        if (status != CandidateStatus.SCHEDULED) {
            throw new IllegalStateException(
                "BR-X01 confirm 전이는 SCHEDULED 에서만: 현재 " + status);
        }
        if (plannerId == null || plannerId.isBlank()) {
            throw new IllegalArgumentException("plannerId 필수 (BR-X01 RBAC)");
        }
        this.status = CandidateStatus.CONFIRMED;
        this.confirmedAt = now;
        this.confirmedBy = plannerId;
        this.updatedAt = now;
    }
}
