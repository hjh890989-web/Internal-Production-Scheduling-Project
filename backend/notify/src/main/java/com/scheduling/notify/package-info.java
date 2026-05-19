/**
 * 알림 모듈 — WebSocket / STOMP 실시간 push.
 *
 * REQ-FUNC-CO-008 (도달 추적), REQ-FUNC-EX-014 (WebSocket PUSH).
 * BR-X06 MES 폴백 (1 shift 미수신 시 degraded mode).
 *
 * 의존: common, order::events, vc::events, ex::events.
 * 발행 이벤트: (없음 — terminal sink)
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Notify",
    allowedDependencies = { "common", "order::events", "vc::events", "ex::events" }
)
package com.scheduling.notify;
