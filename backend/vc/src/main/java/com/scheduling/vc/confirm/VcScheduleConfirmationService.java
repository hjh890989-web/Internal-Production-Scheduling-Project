package com.scheduling.vc.confirm;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.events.VcConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * VC 스케줄 Planner 확정 — TK-10-1-2 (EP-10 ST-10-1, BR-X01).
 *
 * <p>Candidate → Confirmed 상태 전이 + audit 필드 + {@link VcConfirmedEvent} 발행.
 * RBAC ROLE_PLANNER 강제 (Controller 레벨 @PreAuthorize).
 *
 * <p>DB trigger {@code trg_vc_schedule_transition} 이 잘못된 전이를 추가 차단 (이중 안전망).
 */
@Service
@Profile("with-infra")
public class VcScheduleConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(VcScheduleConfirmationService.class);

    private final VcScheduleRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public VcScheduleConfirmationService(
        VcScheduleRepository repository,
        ApplicationEventPublisher eventPublisher,
        Clock clock
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * 단일 row 확정. Planner UI 단건 클릭 시.
     *
     * <p>Sprint 16 BR-X05 dual-review — createdBy 와 plannerId 가 동일 사번이면 reject.
     * createdBy 가 NULL (legacy row, Sprint 15 이전 데이터) 인 경우는 통과 — 이후 row 만 강제.
     */
    @Auditable("VC schedule Planner 단건 확정 (BR-X01·X05)")
    @Transactional
    public VcSchedule confirm(UUID vcScheduleId, String plannerId) {
        VcSchedule schedule = repository.findById(vcScheduleId)
            .orElseThrow(() -> new IllegalArgumentException(
                "vc_schedule_id 미존재: " + vcScheduleId));
        enforceDualReview(schedule, plannerId);
        Instant now = Instant.now(clock);
        schedule.confirm(plannerId, now);
        repository.save(schedule);
        log.info("VC schedule confirmed — id={}, planner={}, createdBy={}",
            vcScheduleId, plannerId, schedule.getCreatedBy());
        return schedule;
    }

    /**
     * BR-X05 dual-review — 작성자(createdBy) ≠ 승인자(plannerId) 강제.
     * createdBy NULL 은 legacy row (Sprint 16 이전) — 통과.
     */
    private void enforceDualReview(VcSchedule schedule, String plannerId) {
        String createdBy = schedule.getCreatedBy();
        if (createdBy != null && !createdBy.isBlank() && createdBy.equals(plannerId)) {
            throw new DualReviewConflictException(schedule.getVcScheduleId(), createdBy, plannerId);
        }
    }

    /**
     * Batch 확정 + 이벤트 발행. Planner UI 다건 선택 시.
     *
     * @return 확정된 row 수
     */
    @Auditable("VC schedule Planner 배치 확정 (BR-X01·X05)")
    @Transactional
    public int confirmBatch(List<UUID> scheduleIds, String plannerId, UUID batchId) {
        Instant now = Instant.now(clock);
        List<VcConfirmedEvent.VcConfirmedRow> rows = new java.util.ArrayList<>();

        for (UUID id : scheduleIds) {
            VcSchedule s = repository.findById(id).orElse(null);
            if (s == null) continue;
            enforceDualReview(s, plannerId);
            s.confirm(plannerId, now);
            repository.save(s);
            rows.add(new VcConfirmedEvent.VcConfirmedRow(
                s.getVcScheduleId(), s.getHoseId(), s.getProductionDate(),
                s.getMachineId(), s.getRotationNo(), s.getSlotPosition(),
                s.getPlannedQty()));
        }

        if (!rows.isEmpty()) {
            eventPublisher.publishEvent(new VcConfirmedEvent(batchId, now, rows));
            log.info("VC batch confirmed — batchId={}, rows={}, planner={}",
                batchId, rows.size(), plannerId);
        }
        return rows.size();
    }
}
