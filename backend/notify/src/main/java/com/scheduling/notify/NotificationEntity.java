package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.api.NotificationChannel;
import com.scheduling.notify.api.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 알림 도달 라이프사이클 영속 — TK-03-3-2 (SAD §6.2.12).
 *
 * <p>schema: {@code app}. 1 Notification = 1 row. Critical 변경 1건 → 인앱 row 1 + 카톡 row 1 (총 2건).
 *
 * <p>상태 머신: {@code DISPATCHED → SENT → DELIVERED → ACKNOWLEDGED | FAILED} —
 * setter 메서드는 라이프사이클 전이만 허용 (다른 컬럼은 immutable).
 *
 * <p>{@code dispatchedAt} 은 Clock 주입 (BR-X04). status 전이 시각 컬럼들은 Instant 직접 set —
 * 호출자 ({@link NotificationDispatchService} / {@link AckController}) 가 Clock 사용.
 */
@Entity
@Table(name = "notification", schema = "app")
public class NotificationEntity {

    @Id
    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "order_change_id", nullable = false, updatable = false)
    private UUID orderChangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20, updatable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10, updatable = false)
    private ChangeSeverity severity;

    @Column(name = "target_role", nullable = false, length = 30, updatable = false)
    private String targetRole;

    @Column(name = "hose_id", nullable = false, length = 40, updatable = false)
    private String hoseId;

    @Column(name = "delivery_date", nullable = false, updatable = false)
    private LocalDate deliveryDate;

    @Column(name = "change_summary", columnDefinition = "text", updatable = false)
    private String changeSummary;

    @Column(name = "dispatched_at", nullable = false, updatable = false)
    private Instant dispatchedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    protected NotificationEntity() {}

    public NotificationEntity(UUID notificationId, UUID orderChangeId, NotificationChannel channel,
                              ChangeSeverity severity, String targetRole, String hoseId,
                              LocalDate deliveryDate, String changeSummary, Instant dispatchedAt) {
        if (dispatchedAt == null) {
            throw new IllegalArgumentException("dispatchedAt 필수 (Clock 주입 — BR-X04)");
        }
        this.notificationId = notificationId;
        this.orderChangeId = orderChangeId;
        this.channel = channel;
        this.severity = severity;
        this.targetRole = targetRole;
        this.hoseId = hoseId;
        this.deliveryDate = deliveryDate;
        this.changeSummary = changeSummary;
        this.dispatchedAt = dispatchedAt;
        this.retryCount = 0;
        this.status = NotificationStatus.DISPATCHED;
    }

    public void markSent(Instant at) {
        this.sentAt = at;
        this.status = NotificationStatus.SENT;
    }

    public void markDelivered(Instant at) {
        this.deliveredAt = at;
        this.status = NotificationStatus.DELIVERED;
    }

    /** idempotent — 이미 ack 된 경우 변경 없음 (AC: 재 ack noop). */
    public boolean acknowledge(Instant at) {
        if (this.acknowledgedAt != null) {
            return false;
        }
        this.acknowledgedAt = at;
        this.status = NotificationStatus.ACKNOWLEDGED;
        return true;
    }

    public void markFailed(Instant at, String errorMessage) {
        this.failedAt = at;
        this.errorMessage = errorMessage;
        this.status = NotificationStatus.FAILED;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public UUID getNotificationId() { return notificationId; }
    public UUID getOrderChangeId() { return orderChangeId; }
    public NotificationChannel getChannel() { return channel; }
    public ChangeSeverity getSeverity() { return severity; }
    public String getTargetRole() { return targetRole; }
    public String getHoseId() { return hoseId; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public String getChangeSummary() { return changeSummary; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getFailedAt() { return failedAt; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public NotificationStatus getStatus() { return status; }
}
