package com.scheduling.notify;

import com.scheduling.vc.events.VcChangedEvent;
import com.scheduling.vc.events.VcConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Sprint 14 EP-VC-FULL ST-VC-4 — VC schedule 변경 → STOMP {@code /topic/vc-schedule-updates} push.
 *
 * <p>PLANNER 가 시뮬뷰 (/vc/simview) 에서 hose 드래그 또는 confirm 시 발행되는
 * {@link VcChangedEvent} / {@link VcConfirmedEvent} 를 구독 → STK 화면 1초 내 자동 갱신
 * (REQ-NF-PER-004 p95 ≤ 2초).
 *
 * <p>Sprint 6 ExReplanPushListener 패턴 재사용 — in-memory broker + AFTER_COMMIT async.
 */
@Component
@Profile("with-infra")
public class VcSchedulePushListener {

    private static final Logger log = LoggerFactory.getLogger(VcSchedulePushListener.class);

    /** Sprint 14 EP-VC-FULL — frontend stompClient 구독 토픽. */
    public static final String VC_SCHEDULE_UPDATES_TOPIC = "/topic/vc-schedule-updates";

    private final SimpMessagingTemplate stomp;

    public VcSchedulePushListener(SimpMessagingTemplate stomp) {
        this.stomp = stomp;
    }

    @ApplicationModuleListener
    public void onVcChanged(VcChangedEvent event) {
        stomp.convertAndSend(VC_SCHEDULE_UPDATES_TOPIC, event);
        log.info("STOMP PUSH {} (changed) — schedule={} rows={}",
            VC_SCHEDULE_UPDATES_TOPIC, event.scheduleId(), event.changedRows().size());
    }

    @ApplicationModuleListener
    public void onVcConfirmed(VcConfirmedEvent event) {
        stomp.convertAndSend(VC_SCHEDULE_UPDATES_TOPIC, event);
        log.info("STOMP PUSH {} (confirmed) — event={}",
            VC_SCHEDULE_UPDATES_TOPIC, event.getClass().getSimpleName());
    }
}
