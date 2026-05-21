package com.scheduling.vc.routing;

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
 * 라우팅 결정 audit row — TK-05-4-2 (BR-X02, K-V06).
 *
 * <p>schema: audit. UPDATE/DELETE 차단 (REQ-NF-SEC-004 — INSERT only).
 *
 * <p>setter 메서드 없음 (immutable post-INSERT) — Clock 강제 (BR-X04).
 */
@Entity
@Table(name = "machine_decision", schema = "audit")
public class MachineDecisionAudit {

    @Id
    @Column(name = "decision_id", nullable = false, updatable = false)
    private UUID decisionId;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private Instant decidedAt;

    @Column(name = "hose_id", nullable = false, length = 40, updatable = false)
    private String hoseId;

    @Column(name = "production_date", nullable = false, updatable = false)
    private LocalDate productionDate;

    @Column(name = "machine_id", nullable = false, length = 10, updatable = false)
    private String machineId;

    @Column(name = "machine_type", nullable = false, length = 2, updatable = false)
    private String machineType;     // "LP" / "IC"

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 20, updatable = false)
    private DecisionType decisionType;

    @Column(name = "policy_id", nullable = false, length = 20, updatable = false)
    private String policyId;

    @Column(name = "reason", columnDefinition = "text", updatable = false)
    private String reason;

    @Column(name = "actor", nullable = false, length = 40, updatable = false)
    private String actor;

    protected MachineDecisionAudit() {}

    public MachineDecisionAudit(UUID decisionId, Instant decidedAt, String hoseId,
                                 LocalDate productionDate, String machineId, String machineType,
                                 DecisionType decisionType, String policyId,
                                 String reason, String actor) {
        if (decidedAt == null) {
            throw new IllegalArgumentException("decidedAt 필수 (Clock 주입 — BR-X04)");
        }
        if (!"LP".equals(machineType) && !"IC".equals(machineType)) {
            throw new IllegalArgumentException("machineType: LP|IC 만 허용 — " + machineType);
        }
        this.decisionId = decisionId;
        this.decidedAt = decidedAt;
        this.hoseId = hoseId;
        this.productionDate = productionDate;
        this.machineId = machineId;
        this.machineType = machineType;
        this.decisionType = decisionType;
        this.policyId = policyId;
        this.reason = reason;
        this.actor = actor == null ? "system:allocator" : actor;
    }

    public UUID getDecisionId() { return decisionId; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getHoseId() { return hoseId; }
    public LocalDate getProductionDate() { return productionDate; }
    public String getMachineId() { return machineId; }
    public String getMachineType() { return machineType; }
    public DecisionType getDecisionType() { return decisionType; }
    public String getPolicyId() { return policyId; }
    public String getReason() { return reason; }
    public String getActor() { return actor; }
}
