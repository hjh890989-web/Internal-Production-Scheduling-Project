package com.scheduling.ex.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Partial replan 완료 도메인 이벤트 — TK-EX14-1-1 (EP-EX14, REQ-FUNC-EX-014).
 *
 * <p>{@code PartialReplanService.replanWithContext} 후 발행. notify 모듈
 * {@code ExReplanPushListener} 가 STOMP {@code /topic/extrusion-updates} 로 전파.
 *
 * <p>BR-X03 chain — vc.changed → partial replan → ExReplanCompletedEvent → 압출 패드 PUSH.
 *
 * @param vcScheduleId    유발 batch (VcChangedEvent.scheduleId)
 * @param completedAt     replan 완료 시각 (Clock 주입, BR-X04)
 * @param triggeredCount  cascade 적용된 candidate 수
 * @param candidateIds    영향 candidate IDs
 */
public record ExReplanCompletedEvent(
    UUID vcScheduleId,
    Instant completedAt,
    int triggeredCount,
    List<UUID> candidateIds
) {
    public ExReplanCompletedEvent {
        candidateIds = candidateIds == null ? List.of() : List.copyOf(candidateIds);
    }
}
