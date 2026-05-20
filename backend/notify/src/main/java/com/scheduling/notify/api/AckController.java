package com.scheduling.notify.api;

import com.scheduling.notify.NotificationEntity;
import com.scheduling.notify.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 알림 acknowledge 엔드포인트 — TK-03-3-2 (REQ-FUNC-CO-008).
 *
 * <p>{@code POST /api/v1/notifications/{id}/ack} — UI 클릭 또는 카카오 webhook 콜백.
 *
 * <p>idempotent — 이미 ack 된 알림 재호출 시 변경 없음, 200 으로 동일 응답.
 * 미존재 → 404 (ResponseStatusException).
 *
 * <p>{@code @Profile("with-infra")} — DB / Repository 의존.
 *
 * <p>RBAC: 인증된 모든 role 허용 (Critical 알림 수신자는 PLANNER · STK_USER · IT_OPS 광범위).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Profile("with-infra")
public class AckController {

    private static final Logger log = LoggerFactory.getLogger(AckController.class);

    private final NotificationRepository repository;
    private final Clock clock;

    public AckController(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @PostMapping("/{id}/ack")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public AckResponse acknowledge(@PathVariable UUID id) {
        NotificationEntity entity = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Notification 미존재: " + id));

        Instant at = Instant.now(clock);
        boolean changed = entity.acknowledge(at);
        if (changed) {
            repository.save(entity);
            log.info("Notification {} acknowledged at {}", id, at);
        } else {
            log.debug("Notification {} 이미 ack됨 — idempotent noop", id);
        }
        return new AckResponse(entity.getNotificationId(), entity.getAcknowledgedAt(), changed);
    }

    /** ack 응답 — 새로 ack 됐는지 (changed) + ack 시각. */
    public record AckResponse(UUID notificationId, Instant acknowledgedAt, boolean changed) {}
}
