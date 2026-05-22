package com.scheduling.master.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 품번별 재고 마스터 — TK-08-3-1 (EP-08 ST-08-3).
 *
 * <p>{@code Q_ext = max(0, Q_vc + target_stock − current_stock)} 입력. 음수 stock 금지
 * (CHECK ≥ 0). MES 동기화 또는 IT_OPS 수동 갱신 (Phase 2+).
 */
@Entity
@Table(name = "product_inventory", schema = "master")
public class ProductInventory {

    @Id
    @Column(name = "hose_id", nullable = false, length = 40, updatable = false)
    private String hoseId;

    @Column(name = "target_stock", nullable = false)
    private int targetStock;

    @Column(name = "current_stock", nullable = false)
    private int currentStock;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false, length = 40)
    private String updatedBy;

    protected ProductInventory() {}

    public ProductInventory(String hoseId, int targetStock, int currentStock,
                             Instant updatedAt, String updatedBy) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt 필수 (BR-X04 Clock 주입)");
        }
        if (targetStock < 0 || currentStock < 0) {
            throw new IllegalArgumentException(
                "stock ≥ 0: target=%d, current=%d".formatted(targetStock, currentStock));
        }
        this.hoseId = hoseId;
        this.targetStock = targetStock;
        this.currentStock = currentStock;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy == null ? "system:seed" : updatedBy;
    }

    public String getHoseId() { return hoseId; }
    public int getTargetStock() { return targetStock; }
    public int getCurrentStock() { return currentStock; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
