package com.scheduling.master.priority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * BR-V12 PRODUCT_PRIORITY — Sprint 7 (REQ-FUNC-VC-022, 수주통합 후 활성).
 *
 * <p>capa 초과 시 {@code CapacityOverflowQueueService} 가 본 entity 의 priority_rank ASC
 * 정렬로 자동 채택 + 추가 요청 큐 분리.
 */
@Entity
@Table(name = "product_priority", schema = "master")
public class ProductPriority {

    @Id
    @Column(name = "hose_id", nullable = false, length = 40)
    private String hoseId;

    @Column(name = "priority_rank", nullable = false)
    private short priorityRank;

    @Column(name = "rationale", columnDefinition = "text")
    private String rationale;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected ProductPriority() {}

    public ProductPriority(String hoseId, short priorityRank, String rationale,
                           LocalDate effectiveFrom, LocalDate effectiveTo,
                           Instant updatedAt, String updatedBy) {
        if (priorityRank < 1 || priorityRank > 99) {
            throw new IllegalArgumentException("priorityRank 1..99: " + priorityRank);
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException(
                "effectiveTo %s < effectiveFrom %s".formatted(effectiveTo, effectiveFrom));
        }
        this.hoseId = hoseId;
        this.priorityRank = priorityRank;
        this.rationale = rationale;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public String getHoseId() { return hoseId; }
    public short getPriorityRank() { return priorityRank; }
    public String getRationale() { return rationale; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }

    public boolean isEffective(LocalDate at) {
        return !at.isBefore(effectiveFrom) && (effectiveTo == null || !at.isAfter(effectiveTo));
    }
}
