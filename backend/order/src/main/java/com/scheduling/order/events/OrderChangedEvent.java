package com.scheduling.order.events;

import com.scheduling.common.enums.ChangeSeverity;
import java.time.Instant;
import java.util.UUID;

/**
 * 수주 변경 도메인 이벤트.
 *
 * BR-O02 변경 분류: D-Day 임박 / cascade 영향 / 수량 ±10% 초과 시 CRITICAL.
 * vc · notify 모듈이 @ApplicationModuleListener 로 구독.
 *
 * @param orderId    수주 식별자
 * @param hoseId     영향 받은 호스 품번 (예: "29673-2R060")
 * @param occurredAt 이벤트 발생 시각 (KST — BR-X04)
 * @param severity   심각도 (NORMAL / CRITICAL)
 */
public record OrderChangedEvent(
    UUID orderId,
    String hoseId,
    Instant occurredAt,
    ChangeSeverity severity
) {
}
