package com.scheduling.vc.internal;

import com.scheduling.vc.events.VcChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * VC 변경 이벤트 발행 — TK-EX13-1-1 (EP-EX13 ST-EX13-1).
 *
 * <p>VC override (TK-13-4-1 Sprint 4) 또는 UPDATE 시 호출. ex 모듈 listener 가
 * 영향 EX candidate 자동 재계산 (수동 호출 금지 — BR-X03).
 *
 * <p>Sprint 3 단계: publisher 기반 구조. Sprint 4 EP-10 (Confirmed 상태) 완료 후
 * VcScheduleService.override 등에서 본격 호출.
 */
@Component
@Profile("with-infra")
public class VcChangedPublisher {

    private static final Logger log = LoggerFactory.getLogger(VcChangedPublisher.class);

    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public VcChangedPublisher(ApplicationEventPublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    /**
     * VC 변경 이벤트 발행. 변경된 row 들을 단일 batch event 묶음.
     */
    public void publishChanges(UUID scheduleId, List<VcChangedEvent.VcChangedRow> changedRows) {
        if (changedRows == null || changedRows.isEmpty()) {
            log.debug("VcChangedPublisher — 변경 row 0건, 이벤트 미발행");
            return;
        }
        VcChangedEvent event = new VcChangedEvent(scheduleId, Instant.now(clock), changedRows);
        publisher.publishEvent(event);
        log.info("VcChangedEvent published — scheduleId={}, changedRows={}",
            scheduleId, changedRows.size());
    }
}
