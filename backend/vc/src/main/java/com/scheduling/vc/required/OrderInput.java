package com.scheduling.vc.required;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Q_required 계산용 수주 입력 — TK-05-3-1.
 *
 * <p>order 도메인 (Order entity) 의 vc-local DTO. caller (REST endpoint 또는 orchestrator) 가
 * Order → OrderInput 변환. Modulith 경계 회피 — vc 는 order.domain.* import 금지.
 *
 * @param orderId        Order PK (linkedOrderIds 추적용)
 * @param hoseId         품번
 * @param deliveryDate   납기일 (호라이즌 필터 기준)
 * @param qty            수주 수량
 */
public record OrderInput(
    UUID orderId,
    String hoseId,
    LocalDate deliveryDate,
    int qty
) {
}
