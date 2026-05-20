package com.scheduling.notify.api;

import com.scheduling.common.enums.ChangeSeverity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 알림 dispatch payload — TK-03-3-1.
 *
 * <p>cross-module DTO — {@link com.scheduling.order.events.OrderDiffPersistedEvent} 로부터
 * NotificationService 가 생성. KakaoTalkClient · WebSocketNotificationPublisher 의 공통 입력.
 *
 * <p>{@code notificationId} 는 알림 1건당 unique — TK-03-3-2 NOTIFICATION 테이블 PK 와 매칭.
 * {@code targetRole} = Keycloak role name (PLANNER · STK_USER · IT_OPS · READ_ONLY).
 *
 * @param notificationId  알림 unique ID (UUID)
 * @param orderChangeId   원본 OrderChangeEntity ID (추적용)
 * @param channel         발송 채널 (IN_APP / KAKAOTALK / SLACK)
 * @param severity        CRITICAL / NORMAL
 * @param targetRole      수신 대상 role
 * @param hoseId          품번
 * @param deliveryDate    납기일
 * @param changeSummary   사용자용 한국어 요약
 * @param dispatchedAt    이벤트 생성 시각 (Clock 주입 — BR-X04)
 */
public record Notification(
    UUID notificationId,
    UUID orderChangeId,
    NotificationChannel channel,
    ChangeSeverity severity,
    String targetRole,
    String hoseId,
    LocalDate deliveryDate,
    String changeSummary,
    Instant dispatchedAt
) {
}
