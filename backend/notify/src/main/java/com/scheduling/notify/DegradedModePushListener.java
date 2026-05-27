package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.vc.events.MesDegradedModeChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sprint 18 EP-NOTIFY ST-NOTIFY-4 — MES degraded mode 전이 → Slack + STOMP push (TK-NOTIFY-4-3).
 *
 * <p>{@link MesDegradedModeChangedEvent} 구독 (DegradedModeService 가 1분 polling 후 변화 시 publish)
 * → Slack alert (CRITICAL 채널) + STOMP {@code /topic/mes-degraded-updates} broadcast.
 *
 * <p>4 role 모두 frontend 가 본 토픽 구독 → DegradedBanner 즉시 갱신 (30s polling 대기 단축).
 */
@Component
@Profile("with-infra")
public class DegradedModePushListener {

    private static final Logger log = LoggerFactory.getLogger(DegradedModePushListener.class);

    /** Sprint 18 EP-NOTIFY — frontend stompClient 구독 토픽 (DegradedBanner 즉시 갱신). */
    public static final String MES_DEGRADED_TOPIC = "/topic/mes-degraded-updates";

    private final SimpMessagingTemplate stomp;
    private final SlackNotifier slackNotifier;

    public DegradedModePushListener(SimpMessagingTemplate stomp, SlackNotifier slackNotifier) {
        this.stomp = stomp;
        this.slackNotifier = slackNotifier;
    }

    @ApplicationModuleListener
    public void on(MesDegradedModeChangedEvent event) {
        // STOMP push (모든 role broadcast — frontend 가 topic 구독 시 자동 수신)
        stomp.convertAndSend(MES_DEGRADED_TOPIC, Map.of(
            "machineId", event.machineId(),
            "wasDegraded", event.wasDegraded(),
            "isDegraded", event.isDegraded(),
            "changedAt", event.changedAt().toString(),
            "transition", event.isEntering() ? "ENTERING" : (event.isRecovered() ? "RECOVERED" : "NONE")
        ));

        // Slack alert — 진입/해제 동시 push (운영자 회복 인지)
        String title;
        String body;
        if (event.isEntering()) {
            title = String.format("MES degraded 진입 — %s", event.machineId());
            body = String.format(
                "*machineId*: %s%n*상태*: NORMAL → DEGRADED%n*감지 시각*: %s%n"
                    + "1 shift (6h) 미수신 — 직전 계획값 임시 사용 (REQ-FUNC-CO-004). PLANNER/IT_OPS 가 Excel 폴백 입력 권고.",
                event.machineId(), event.changedAt());
        } else if (event.isRecovered()) {
            title = String.format("MES 정상 복구 — %s", event.machineId());
            body = String.format(
                "*machineId*: %s%n*상태*: DEGRADED → NORMAL%n*복구 시각*: %s%n자동 재조정 진행.",
                event.machineId(), event.changedAt());
        } else {
            // 동일 상태 유지 — Slack push skip (노이즈 차단)
            log.debug("MES degraded event no-op — machine={} state={}", event.machineId(), event.isDegraded());
            return;
        }
        slackNotifier.alert(ChangeSeverity.CRITICAL, title, body);

        log.info("STOMP PUSH {} + Slack — machine={} {} → {}",
            MES_DEGRADED_TOPIC, event.machineId(),
            event.wasDegraded() ? "DEGRADED" : "NORMAL",
            event.isDegraded() ? "DEGRADED" : "NORMAL");
    }
}
