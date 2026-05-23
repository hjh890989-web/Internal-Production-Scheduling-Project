package com.scheduling.master.api;

import java.time.LocalDate;
import java.util.UUID;

/**
 * BR-V13 KD_ORDER 요약 — Sprint 7 (cross-module).
 *
 * <p>vc 모듈 {@code KdSupplementService} 가 본 record + KdOrderLookup.consume(uuid,qty).
 */
public record KdOrderSummary(
    UUID kdOrderId,
    String hoseId,
    int orderQty,
    int remainingQty,
    LocalDate orderDate,
    String status              // OPEN | PARTIAL | FILLED | CANCELLED
) {
    public boolean isAvailable() {
        return ("OPEN".equals(status) || "PARTIAL".equals(status)) && remainingQty > 0;
    }
}
