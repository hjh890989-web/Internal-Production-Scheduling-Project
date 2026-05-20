package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Notification 영속 — TK-03-3-2.
 *
 * <p>{@link #findCriticalOverdue} = DeliveryEscalator hot path.
 * 부분 인덱스 {@code idx_notification_undelivered_critical} 와 매칭되어 1분 polling 도 부담 없음.
 */
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    /**
     * SLA 위반 Critical 알림 — {@code dispatched_at < threshold AND ack/fail 모두 null}.
     */
    @Query("""
        SELECT n FROM NotificationEntity n
        WHERE n.severity = :severity
          AND n.acknowledgedAt IS NULL
          AND n.failedAt IS NULL
          AND n.dispatchedAt < :threshold
        ORDER BY n.dispatchedAt ASC
        """)
    List<NotificationEntity> findCriticalOverdue(
        @Param("severity") ChangeSeverity severity,
        @Param("threshold") Instant threshold);

    List<NotificationEntity> findByOrderChangeIdOrderByDispatchedAtAsc(UUID orderChangeId);
}
