package com.scheduling.vc.events;

import java.time.Instant;

/**
 * Sprint 18 EP-NOTIFY ST-NOTIFY-4 — MES degraded mode 전이 이벤트 (BR-X06).
 *
 * <p>{@code DegradedModeService} 가 1분 주기 polling 으로 머신별 직전 상태와 비교 → 변화 시
 * 본 이벤트 발행. notify 모듈 {@code DegradedModePushListener} 가 구독 → Slack alert +
 * STOMP {@code /topic/notifications/{role}} push.
 *
 * @param machineId    가류기 (LP-01~04, IC-01)
 * @param wasDegraded  직전 상태 (true = degraded)
 * @param isDegraded   현재 상태 (true = degraded)
 * @param changedAt    전이 감지 시각 (Clock 주입, BR-X04 KST)
 */
public record MesDegradedModeChangedEvent(
    String machineId,
    boolean wasDegraded,
    boolean isDegraded,
    Instant changedAt
) {
    /** 진입 전이 — NORMAL → DEGRADED (true 반환). */
    public boolean isEntering() {
        return !wasDegraded && isDegraded;
    }

    /** 해제 전이 — DEGRADED → NORMAL (true 반환). */
    public boolean isRecovered() {
        return wasDegraded && !isDegraded;
    }
}
