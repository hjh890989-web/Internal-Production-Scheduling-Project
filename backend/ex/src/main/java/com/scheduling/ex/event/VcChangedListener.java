package com.scheduling.ex.event;

import com.scheduling.vc.events.VcChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * VC 변경 이벤트 구독 — TK-EX13-1-1 (EP-EX13 ST-EX13-1, BR-X03 / BR-E11).
 *
 * <p>VC override / UPDATE → 영향 EX candidate 자동 재계산. 수동 호출 없음 (BR-X03).
 *
 * <p>흐름: VcChangedEvent → ImpactedRowFinder → PartialReplanService.
 *
 * <p>{@code @ApplicationModuleListener} = TransactionalEventListener(AFTER_COMMIT) +
 * Async — VC 트랜잭션 commit 후 비동기.
 */
@Component
@Profile("with-infra")
public class VcChangedListener {

    private static final Logger log = LoggerFactory.getLogger(VcChangedListener.class);

    private final ImpactedRowFinder finder;
    private final PartialReplanService replanService;

    public VcChangedListener(ImpactedRowFinder finder, PartialReplanService replanService) {
        this.finder = finder;
        this.replanService = replanService;
    }

    @ApplicationModuleListener
    public void onVcChanged(VcChangedEvent event) {
        log.info("VC changed event received: scheduleId={}, changedRows={}",
            event.scheduleId(), event.changedRows().size());

        List<UUID> impacted = finder.findImpacted(event);
        if (impacted.isEmpty()) {
            log.info("VC change has no EX impact: scheduleId={}", event.scheduleId());
            return;
        }
        int triggered = replanService.triggerReplan(impacted);
        log.info("Partial replan triggered: scheduleId={}, impactedCandidates={}, triggered={}",
            event.scheduleId(), impacted.size(), triggered);
    }
}
