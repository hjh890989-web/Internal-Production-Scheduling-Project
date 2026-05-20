package com.scheduling.order.events;

import com.scheduling.common.enums.ChangeSeverity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 마스터 버전 간 diff 영속 직후 발행 — TK-03-3-1.
 *
 * <p>{@link com.scheduling.order.diff.DiffTaggingService} 가 한 OrderChangeEntity 마다 발행.
 * notify 모듈의 {@link org.springframework.modulith.events.ApplicationModuleListener} 가 구독 →
 * Critical 은 인앱 + 카카오톡, Normal 은 인앱만 발송 (REQ-FUNC-OC-009).
 *
 * <p>{@link OrderChangedEvent} 와 분리한 이유 — 본 이벤트는 import / commit 사이클의
 * diff 컨텍스트 (changeId, changeSummary) 를 포함, vc partial replan 트리거 OrderChangedEvent
 * 와는 의미가 다름.
 *
 * @param changeId        OrderChangeEntity change_id (PK)
 * @param trackingId      import 추적 ID
 * @param hoseId          품번
 * @param deliveryDate    납기일
 * @param diffType        NEW / MODIFIED / DELETED
 * @param severity        CRITICAL / NORMAL
 * @param changeSummary   사용자용 한국어 요약 (예: "qty 100 → 130 (+30%)")
 * @param occurredAt      이벤트 발생 시각 (KST, Clock 주입 — BR-X04)
 */
public record OrderDiffPersistedEvent(
    UUID changeId,
    UUID trackingId,
    String hoseId,
    LocalDate deliveryDate,
    String diffType,
    ChangeSeverity severity,
    String changeSummary,
    Instant occurredAt
) {
}
