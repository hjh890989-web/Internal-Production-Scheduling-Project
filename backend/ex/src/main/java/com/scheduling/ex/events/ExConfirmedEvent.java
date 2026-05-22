package com.scheduling.ex.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * EX 확정 도메인 이벤트 — TK-10-2-1 (EP-10 ST-10-2, BR-X01).
 *
 * <p>EX candidate Confirmed 전이 시 발행. EP-EX14 WebSocket PUSH (notify 모듈) +
 * EP-11 audit 입력.
 *
 * @param batchId       확정 배치 PK
 * @param confirmedAt   확정 시각 (BR-X04 Clock)
 * @param confirmedBy   Planner 사번
 * @param candidateIds  확정된 candidate IDs
 */
public record ExConfirmedEvent(
    UUID batchId,
    Instant confirmedAt,
    String confirmedBy,
    List<UUID> candidateIds
) {
    public ExConfirmedEvent {
        candidateIds = candidateIds == null ? List.of() : List.copyOf(candidateIds);
    }
}
