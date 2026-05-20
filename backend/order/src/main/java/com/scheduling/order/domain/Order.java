package com.scheduling.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 표준 ORDER 마스터 row — TK-02-1-1 (SRS §6.2.4).
 *
 * <p>JPA Entity. {@link OrderDraft} (commit 전 DTO) 가 {@link com.scheduling.order.mapping.SchemaMappingService}
 * 결과로 생성되어 DB persist 시 본 Entity 로 변환.
 *
 * <p>UNIQUE 제약: {@code (hose_id, delivery_date, master_version)} — TK-02-1-1 핵심 (REQ-FUNC-OC-005).
 * Flyway 마이그레이션 {@code V002__add_order_unique_constraint.sql} 가 동일 제약 명시 (JPA + DB dual).
 *
 * <p>schema: {@code app} (PDD v1.7 ADR-010 정본).
 */
@Entity
@Table(
    name = "\"order\"",
    schema = "app",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_order_hose_delivery_version",
        columnNames = {"hose_id", "delivery_date", "master_version"}
    )
)
public class Order {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "hose_id", nullable = false, length = 40)
    private String hoseId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Column(name = "customer", nullable = false, length = 100)
    private String customer;

    @Column(name = "master_version", nullable = false)
    private int masterVersion;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** JPA 기본 생성자. */
    protected Order() {}

    public Order(UUID orderId, String hoseId, LocalDate deliveryDate, int qty,
                 OrderType orderType, String customer, int masterVersion,
                 String status, Instant createdAt) {
        if (createdAt == null) {
            // BR-X04 — Clock 주입 강제 (Instant.now() 정적 호출 금지, ArchUnit KstTimezoneArchTest)
            // caller (OrderCommitService) 가 Clock 으로 createdAt 명시 필요.
            throw new IllegalArgumentException("createdAt 필수 — Clock 주입으로 명시 (BR-X04)");
        }
        this.orderId = orderId;
        this.hoseId = hoseId;
        this.deliveryDate = deliveryDate;
        this.qty = qty;
        this.orderType = orderType;
        this.customer = customer;
        this.masterVersion = masterVersion;
        this.status = status == null ? "ACTIVE" : status;
        this.createdAt = createdAt;
    }

    /** OrderDraft + master_version → Order entity 변환. */
    public static Order fromDraft(OrderDraft draft, int masterVersion, Instant createdAt) {
        return new Order(
            draft.orderId(),
            draft.hoseId(),
            draft.deliveryDate(),
            draft.qty(),
            draft.orderType(),
            draft.customer(),
            masterVersion,
            "ACTIVE",
            createdAt
        );
    }

    public UUID getOrderId() { return orderId; }
    public String getHoseId() { return hoseId; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public int getQty() { return qty; }
    public OrderType getOrderType() { return orderType; }
    public String getCustomer() { return customer; }
    public int getMasterVersion() { return masterVersion; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
