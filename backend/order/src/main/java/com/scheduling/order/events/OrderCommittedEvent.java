package com.scheduling.order.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Sprint 13 EP-OC-FULL ST-OC-2 — PLANNER 가 trackingId 의 수주 diff 확정 시 발행.
 *
 * <p>Sprint 14 EP-VC-FULL 의 VC 모듈 listener (@ApplicationModuleListener) 가 구독 →
 * 성형 스케줄 입력 단계로 진입. Sprint 13 baseline 은 publisher 만 (listener 는 Sprint 14).
 *
 * @param trackingId import 추적 ID
 * @param committedBy 확정자 사번 (BR-X05 dual-review 작성자)
 * @param committedAt 확정 시각 (KST, BR-X04)
 * @param reason 확정 사유 (BR-X02 audit, 빈 문자열 허용)
 */
public record OrderCommittedEvent(
    UUID trackingId,
    String committedBy,
    Instant committedAt,
    String reason
) {}
