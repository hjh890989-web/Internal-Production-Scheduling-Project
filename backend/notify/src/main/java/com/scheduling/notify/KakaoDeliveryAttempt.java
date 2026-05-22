package com.scheduling.notify;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Kakao BizMessage 시도 기록 — TK-16-1-1 (EP-16 ST-16-1, REQ-FUNC-CO-008).
 *
 * <p>retry 시퀀스 (attempt_no 1~3) + status (SUCCESS/FAILED/SKIPPED). 도달률 100% 추적.
 */
@Entity
@Table(name = "kakao_delivery_log", schema = "app")
public class KakaoDeliveryAttempt {

    public enum Status { SUCCESS, FAILED, SKIPPED }

    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false)
    private UUID attemptId;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "target_role", nullable = false, length = 40)
    private String targetRole;

    @Column(name = "message_preview", columnDefinition = "text")
    private String messagePreview;

    @Column(name = "attempt_no", nullable = false)
    private short attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected KakaoDeliveryAttempt() {}

    public KakaoDeliveryAttempt(UUID attemptId, UUID notificationId, String targetRole,
                                 String messagePreview, short attemptNo, Status status,
                                 String errorMessage, Instant attemptedAt) {
        if (attemptNo < 1 || attemptNo > 3) {
            throw new IllegalArgumentException("attemptNo 1..3: " + attemptNo);
        }
        this.attemptId = attemptId;
        this.notificationId = notificationId;
        this.targetRole = targetRole;
        this.messagePreview = messagePreview;
        this.attemptNo = attemptNo;
        this.status = status;
        this.errorMessage = errorMessage;
        this.attemptedAt = attemptedAt;
    }

    public UUID getAttemptId() { return attemptId; }
    public UUID getNotificationId() { return notificationId; }
    public String getTargetRole() { return targetRole; }
    public String getMessagePreview() { return messagePreview; }
    public short getAttemptNo() { return attemptNo; }
    public Status getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
