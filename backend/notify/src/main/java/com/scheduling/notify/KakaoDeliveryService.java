package com.scheduling.notify;

import com.scheduling.notify.api.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Kakao 도달 추적 + 3회 retry — TK-16-1-1 (EP-16 ST-16-1, REQ-FUNC-CO-008).
 *
 * <p>{@link KakaoTalkClient} 호출 결과 영속 ({@link KakaoDeliveryAttempt}) + max 3회 inline retry.
 * Sprint 6+ Resilience4j @Retry 도입 시 본 서비스의 inline retry 제거.
 *
 * <p>도달률 KPI — {@link KakaoDeliveryRepository#countByStatus} 로 NS-04 monitor.
 */
@Service
@Profile("with-infra")
public class KakaoDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(KakaoDeliveryService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final KakaoTalkClient client;
    private final KakaoDeliveryRepository repository;
    private final Clock clock;

    public KakaoDeliveryService(KakaoTalkClient client,
                                 KakaoDeliveryRepository repository,
                                 Clock clock) {
        this.client = client;
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * @return true = SUCCESS (1~3 회 시도 중 하나라도 성공), false = 모두 실패 또는 SKIPPED
     */
    @Transactional
    public boolean sendWithRetry(Notification notification) {
        for (short attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Instant now = Instant.now(clock);
            boolean ok;
            String error = null;
            try {
                ok = client.send(notification);
            } catch (RuntimeException ex) {
                ok = false;
                error = ex.getMessage();
                log.warn("Kakao send attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, error);
            }
            KakaoDeliveryAttempt.Status status = ok
                ? KakaoDeliveryAttempt.Status.SUCCESS
                : KakaoDeliveryAttempt.Status.FAILED;
            repository.save(new KakaoDeliveryAttempt(
                UUID.randomUUID(), notification.notificationId(),
                notification.targetRole(),
                truncate(buildPreview(notification)),
                attempt, status, error, now));
            if (ok) {
                log.info("Kakao SUCCESS notificationId={} attempt={}/{}",
                    notification.notificationId(), attempt, MAX_ATTEMPTS);
                return true;
            }
        }
        log.warn("Kakao FAILED after {} attempts — notificationId={}",
            MAX_ATTEMPTS, notification.notificationId());
        return false;
    }

    /** SKIPPED 기록 (config disabled 시 호출). */
    @Transactional
    public void recordSkipped(Notification notification, String reason) {
        repository.save(new KakaoDeliveryAttempt(
            UUID.randomUUID(), notification.notificationId(),
            notification.targetRole(),
            truncate(buildPreview(notification)),
            (short) 1, KakaoDeliveryAttempt.Status.SKIPPED, reason,
            Instant.now(clock)));
    }

    private String buildPreview(Notification n) {
        return String.format("hose=%s due=%s change=%s",
            n.hoseId(), n.deliveryDate(), n.changeSummary());
    }

    private String truncate(String s) {
        return s == null ? null : (s.length() > 200 ? s.substring(0, 200) : s);
    }
}
