package com.scheduling.order.diff;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Critical 분류 임계치 — TK-03-2-3 (BR-O02 외부화).
 *
 * <p>application.yml:
 * <pre>
 * scheduling:
 *   severity:
 *     qty-change-threshold-pct: 0.20     # BR-O02 ±20%
 *     delivery-date-always-critical: true
 *     hose-id-always-critical: true
 *     new-always-critical: true
 *     deleted-always-critical: true
 * </pre>
 *
 * <p>운영 중 정책 변경 — application.yml 갱신 후 재시작 (Sprint 1 baseline).
 * Sprint 1+ {@code @RefreshScope} + Spring Cloud Config 도입 검토.
 */
@ConfigurationProperties(prefix = "scheduling.severity")
public class SeverityConfig {

    /** qty 변경 임계 비율 — BR-O02 ±20% 기본 */
    private double qtyChangeThresholdPct = 0.20;

    /** delivery_date 변경 → 항상 Critical (기본 true) */
    private boolean deliveryDateAlwaysCritical = true;

    /** hose_id 변경 → 항상 Critical (기본 true) */
    private boolean hoseIdAlwaysCritical = true;

    /** NEW row → 항상 Critical (기본 true — 수주 출현) */
    private boolean newAlwaysCritical = true;

    /** DELETED row → 항상 Critical (기본 true — 수주 소실) */
    private boolean deletedAlwaysCritical = true;

    public double getQtyChangeThresholdPct() { return qtyChangeThresholdPct; }
    public void setQtyChangeThresholdPct(double value) { this.qtyChangeThresholdPct = value; }

    public boolean isDeliveryDateAlwaysCritical() { return deliveryDateAlwaysCritical; }
    public void setDeliveryDateAlwaysCritical(boolean value) { this.deliveryDateAlwaysCritical = value; }

    public boolean isHoseIdAlwaysCritical() { return hoseIdAlwaysCritical; }
    public void setHoseIdAlwaysCritical(boolean value) { this.hoseIdAlwaysCritical = value; }

    public boolean isNewAlwaysCritical() { return newAlwaysCritical; }
    public void setNewAlwaysCritical(boolean value) { this.newAlwaysCritical = value; }

    public boolean isDeletedAlwaysCritical() { return deletedAlwaysCritical; }
    public void setDeletedAlwaysCritical(boolean value) { this.deletedAlwaysCritical = value; }
}
