package com.scheduling.notify.api;

/**
 * 알림 도달 라이프사이클 — TK-03-3-2 (SAD §6.2.12 NOTIFICATION).
 *
 * <pre>
 *   DISPATCHED → SENT → DELIVERED → ACKNOWLEDGED
 *                                  └─→ FAILED (에러 / timeout)
 * </pre>
 *
 * <ul>
 *   <li>{@link #DISPATCHED} — Notification row 생성 (channel routing 완료)</li>
 *   <li>{@link #SENT} — 외부 채널 (WebSocket / KakaoTalk) 송신 시도 완료</li>
 *   <li>{@link #DELIVERED} — 채널 응답 (HTTP 2xx / WebSocket ack)</li>
 *   <li>{@link #ACKNOWLEDGED} — 사용자 확인 (UI 클릭 or 카카오 응답)</li>
 *   <li>{@link #FAILED} — 채널 송신 실패 (Resilience4j fallback 후)</li>
 * </ul>
 */
public enum NotificationStatus {
    DISPATCHED,
    SENT,
    DELIVERED,
    ACKNOWLEDGED,
    FAILED
}
