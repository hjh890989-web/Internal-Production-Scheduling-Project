package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * SLA 위반 Critical 알림 에스컬레이션 — TK-03-3-2 (REQ-NF-KPI-015 K-O04).
 *
 * <p>1분 주기 polling — {@code dispatched_at + 1분 < now AND ack=null AND fail=null} Critical 알림 조회.
 * 부분 인덱스 {@code idx_notification_undelivered_critical} hit (V005 마이그레이션).
 *
 * <p>Sprint 1 baseline — Slack 에스컬레이션은 log 만 (실제 Slack webhook 호출은 Sprint 2 EP-31).
 * {@code retry_count} 증가 — Resilience4j Retry 정책 (Sprint 2) 가 소비.
 *
 * <p>{@code @Profile("with-infra")} — Repository 의존. DEV 컨텍스트 미활성화.
 */
@Component
@Profile("with-infra")
public class DeliveryEscalator {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEscalator.class);
    private static final Duration CRITICAL_SLA = Duration.ofMinutes(1);  // REQ-FUNC-OC-009

    private final NotificationRepository repository;
    private final Clock clock;

    public DeliveryEscalator(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${scheduling.notification.escalation.interval-ms:60000}")
    @Transactional
    public void escalate() {
        Instant threshold = Instant.now(clock).minus(CRITICAL_SLA);
        List<NotificationEntity> overdue = repository.findCriticalOverdue(ChangeSeverity.CRITICAL, threshold);

        if (overdue.isEmpty()) {
            log.debug("DeliveryEscalator — overdue Critical 알림 0건");
            return;
        }

        for (NotificationEntity n : overdue) {
            n.incrementRetry();
            // Sprint 2 — SlackClient.send(SlackChannel.OPS, buildEscalationMessage(n))
            log.warn("[SLA-BREACH] Critical Notification overdue notificationId={} hose={} dispatched={} retryCount={}",
                n.getNotificationId(), n.getHoseId(), n.getDispatchedAt(), n.getRetryCount());
        }
        repository.saveAll(overdue);

        log.info("DeliveryEscalator — escalated {} Critical 알림 (threshold={})", overdue.size(), threshold);
    }
}
