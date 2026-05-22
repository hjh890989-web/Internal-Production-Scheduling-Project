package com.scheduling.ex.event;

import com.scheduling.vc.events.VcConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * VC 확정 이벤트 구독 — TK-07-1-1 (EP-07 ST-07-1).
 *
 * <p>Spring Modulith {@code @ApplicationModuleListener} = {@code @Async} +
 * {@code @TransactionalEventListener(AFTER_COMMIT)}. VC 트랜잭션 commit 후 비동기 호출 —
 * 압출 처리 실패해도 VC commit 유지 (Modulith retry/DLQ 처리).
 *
 * <p>모듈 경계 — ex 는 vc 도메인 모델 (VcSchedule) 직접 의존 금지. {@link VcConfirmedEvent}
 * record 만 의존 (vc::events namedinterface).
 */
@Component
@Profile("with-infra")
public class VcConfirmedListener {

    private static final Logger log = LoggerFactory.getLogger(VcConfirmedListener.class);

    private final ExtrusionScheduleService extrusionService;

    public VcConfirmedListener(ExtrusionScheduleService extrusionService) {
        this.extrusionService = extrusionService;
    }

    @ApplicationModuleListener
    public void onVcConfirmed(VcConfirmedEvent event) {
        log.info("VC confirmed event received: scheduleId={}, rows={}",
            event.scheduleId(), event.rows().size());
        int created = extrusionService.generateCandidates(event);
        log.info("ExScheduleCandidate generated: scheduleId={}, created={}",
            event.scheduleId(), created);
    }
}
