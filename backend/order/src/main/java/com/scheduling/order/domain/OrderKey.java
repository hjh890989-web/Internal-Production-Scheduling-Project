package com.scheduling.order.domain;

import java.time.LocalDate;

/**
 * Order 복합 키 — TK-02-1-3 (REQ-FUNC-OC-005).
 *
 * <p>{@code (hose_id, delivery_date)} 쌍이 같으면 중복으로 판정. master_version 은 별도 관리
 * (버전 진화 시 같은 키로 새 row 허용 — DB UNIQUE 가 3-tuple 로 강제, 본 record 는 2-tuple).
 *
 * <p>record 의 자동 hashCode/equals — Map key 로 안전 사용.
 */
public record OrderKey(String hoseId, LocalDate deliveryDate) {

    public static OrderKey of(OrderDraft draft) {
        return new OrderKey(draft.hoseId(), draft.deliveryDate());
    }

    public static OrderKey of(Order order) {
        return new OrderKey(order.getHoseId(), order.getDeliveryDate());
    }
}
