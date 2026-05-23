package com.scheduling.master.kd;

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
 * BR-V13 KD_ORDER — Sprint 7 (REQ-FUNC-VC-023, 수주통합 후 활성).
 *
 * <p>capa 부족 시 {@code KdSupplementService} 가 동일 hose → 동일 셋팅 그룹 순으로
 * remaining_qty 참조 + 보충.
 */
@Entity
@Table(name = "kd_order", schema = "master")
public class KdOrder {

    public enum Status { OPEN, PARTIAL, FILLED, CANCELLED }

    @Id
    @Column(name = "kd_order_id", nullable = false, updatable = false)
    private UUID kdOrderId;

    @Column(name = "hose_id", nullable = false, length = 40)
    private String hoseId;

    @Column(name = "order_qty", nullable = false)
    private int orderQty;

    @Column(name = "remaining_qty", nullable = false)
    private int remainingQty;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "customer_code", length = 40)
    private String customerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected KdOrder() {}

    public KdOrder(UUID kdOrderId, String hoseId, int orderQty, int remainingQty,
                   LocalDate orderDate, String customerCode, Status status,
                   Instant updatedAt, String updatedBy) {
        if (orderQty <= 0) {
            throw new IllegalArgumentException("orderQty > 0: " + orderQty);
        }
        if (remainingQty < 0 || remainingQty > orderQty) {
            throw new IllegalArgumentException(
                "remainingQty 0..orderQty (%d): %d".formatted(orderQty, remainingQty));
        }
        this.kdOrderId = kdOrderId;
        this.hoseId = hoseId;
        this.orderQty = orderQty;
        this.remainingQty = remainingQty;
        this.orderDate = orderDate;
        this.customerCode = customerCode;
        this.status = status == null ? Status.OPEN : status;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public UUID getKdOrderId() { return kdOrderId; }
    public String getHoseId() { return hoseId; }
    public int getOrderQty() { return orderQty; }
    public int getRemainingQty() { return remainingQty; }
    public LocalDate getOrderDate() { return orderDate; }
    public String getCustomerCode() { return customerCode; }
    public Status getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }

    /**
     * remaining_qty 차감 + status 자동 갱신 — KdSupplementService 호출용.
     *
     * @param qty 차감량 (≤ remaining_qty)
     * @return 실 차감량 (remaining 부족 시 가능량만)
     */
    public int consume(int qty, Instant now, String actor) {
        if (qty <= 0) throw new IllegalArgumentException("qty > 0: " + qty);
        int actualConsumed = Math.min(qty, remainingQty);
        this.remainingQty -= actualConsumed;
        this.status = (remainingQty == 0) ? Status.FILLED
                    : (remainingQty < orderQty) ? Status.PARTIAL
                    : Status.OPEN;
        this.updatedAt = now;
        this.updatedBy = actor;
        return actualConsumed;
    }
}
