package com.scheduling.notify.api;

/**
 * 알림 채널 분류 — TK-03-3-1 (REQ-FUNC-OC-009).
 *
 * <p>{@link #IN_APP} — WebSocket STOMP /topic/notifications/{role} 채널 — 모든 변경 발송.
 * {@link #KAKAOTALK} — Workplace Bot Webhook — Critical 만 백업 발송 (SAD §3.1 EXT-SYS-05).
 * {@link #SLACK} — IT_OPS 에스컬레이션 (1분 미 ack Critical, TK-03-3-2 DeliveryEscalator).
 */
public enum NotificationChannel {
    IN_APP,
    KAKAOTALK,
    SLACK
}
