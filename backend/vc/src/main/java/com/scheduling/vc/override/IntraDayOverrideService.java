package com.scheduling.vc.override;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * BR-V07 일중 앵글 교체 override — TK-13-4-1 (EP-13 ST-13-4).
 *
 * <p>사용자가 명시적 사유 입력 시 DB trigger 가 통과 — audit row 자동 발행
 * (V025 trg_audit_vc_schedule + @Auditable reason 주입).
 *
 * <p>RBAC ROLE_PLANNER 강제 — Controller @PreAuthorize.
 */
@Service
@Profile("with-infra")
public class IntraDayOverrideService {

    private static final Logger log = LoggerFactory.getLogger(IntraDayOverrideService.class);

    private final VcScheduleRepository repository;
    private final Clock clock;

    public IntraDayOverrideService(VcScheduleRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Auditable("BR-V07 일중 앵글 교체 override")
    @Transactional
    public VcSchedule applyOverride(UUID vcScheduleId, String reason, String plannerId) {
        VcSchedule s = repository.findById(vcScheduleId)
            .orElseThrow(() -> new IllegalArgumentException(
                "vc_schedule_id 미존재: " + vcScheduleId));
        Instant now = Instant.now(clock);
        s.applyOverride(reason, plannerId, now);
        repository.save(s);     // DB trigger 가 일중 락 통과 (reason 비-NULL)
        log.info("BR-V07 override applied — id={}, planner={}, boundary={}",
            vcScheduleId, plannerId,
            BusinessDayBoundaryFormatter.formatBoundaryKey(s.getProductionDate()));
        return s;
    }
}
