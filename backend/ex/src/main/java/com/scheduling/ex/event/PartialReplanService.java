package com.scheduling.ex.event;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.events.VcChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Partial replan stub — TK-EX13-1-3 (EP-EX13 ST-EX13-1).
 *
 * <p><b>Sprint 3 단계</b>: 영향 candidate 들을 PENDING 으로 재전환 (재계산 트리거 표시).
 * 실제 yield / grouping / gate 재실행은 Sprint 4 EP-10 (Confirmed 상태) + EP-EX11
 * (검증 게이트) 완료 후 본격 활성.
 *
 * <p>BR-X03 — 수동 호출 금지. 본 service 는 {@link VcChangedListener} 가 자동 호출.
 */
@Component
@Profile("with-infra")
public class PartialReplanService {

    private static final Logger log = LoggerFactory.getLogger(PartialReplanService.class);

    private final ExScheduleCandidateRepository candidateRepo;
    private final ImpactedRowFinder finder;
    private final WorkingCalendar calendar;
    private final Clock clock;

    public PartialReplanService(ExScheduleCandidateRepository candidateRepo,
                                ImpactedRowFinder finder,
                                WorkingCalendar calendar,
                                Clock clock) {
        this.candidateRepo = candidateRepo;
        this.finder = finder;
        this.calendar = calendar;
        this.clock = clock;
    }

    /**
     * VcChangedEvent → 영향 candidate 자동 재계산 — TK-EX13-1-3 정식 활성 (Sprint 4).
     *
     * <p>알고리즘:
     * <ol>
     *   <li>{@link ImpactedRowFinder} 로 영향 candidate IDs 수집</li>
     *   <li>각 candidate 에 매칭되는 VcChangedRow 적용 — vcYield + deadline 재산출</li>
     *   <li>DELETED → FAILED, 그 외 → PENDING (yield/deadline 갱신)</li>
     *   <li>CONFIRMED candidate 는 사전 차단 (override 별도 흐름)</li>
     * </ol>
     *
     * <p>BR-X03 — 본 메서드는 {@link VcChangedListener} 가 자동 호출 (수동 호출 0건).
     * BR-E11 — 변경된 hose 만 재계산 (전체 replan 회피).
     *
     * @return 재계산된 candidate 수
     */
    @Auditable("BR-X03 partial replan (vc.changed cascade)")
    @Transactional
    public int replanWithContext(VcChangedEvent event) {
        if (event == null || event.changedRows().isEmpty()) return 0;

        Instant now = Instant.now(clock);
        // VcChangedRow lookup by rowId (vc_row_id 매핑)
        Map<UUID, VcChangedEvent.VcChangedRow> changeByRowId = new HashMap<>();
        for (VcChangedEvent.VcChangedRow row : event.changedRows()) {
            changeByRowId.put(row.rowId(), row);
        }

        List<UUID> impactedIds = finder.findImpacted(event);
        int triggered = 0;
        for (UUID id : impactedIds) {
            ExScheduleCandidate cand = candidateRepo.findById(id).orElse(null);
            if (cand == null) continue;
            if (cand.getStatus() == CandidateStatus.CONFIRMED) {
                log.warn("CONFIRMED candidate {} replan skip (override 필요)", id);
                continue;
            }
            VcChangedEvent.VcChangedRow change = changeByRowId.get(cand.getVcRowId());
            if (change == null) {
                // 인접 horizon 영향 — 안전을 위해 PENDING 재전환만
                cand.transitionTo(CandidateStatus.PENDING, now);
            } else if (change.changeType() == VcChangedEvent.ChangeType.DELETED) {
                cand.transitionTo(CandidateStatus.FAILED, now);
            } else {
                LocalDate newVcDate = change.newDate();
                LocalDate newDeadline = calendar.subtractWorkingDays(newVcDate, 1);
                cand.applyVcChange(change.newQty(), newDeadline, newVcDate, now);
            }
            candidateRepo.save(cand);
            triggered++;
        }

        log.info("PartialReplanService cascade — changedRows={}, impacted={}, triggered={}",
            event.changedRows().size(), impactedIds.size(), triggered);
        return triggered;
    }

    /**
     * 영향 candidate 들을 PENDING 으로 재전환 (Sprint 3 stub — 직접 ID 호출용).
     *
     * <p>Sprint 4 cascade 흐름은 {@link #replanWithContext(VcChangedEvent)} 사용.
     *
     * @return 재전환된 candidate 수
     */
    @Transactional
    public int triggerReplan(List<UUID> impactedCandidateIds) {
        if (impactedCandidateIds == null || impactedCandidateIds.isEmpty()) {
            return 0;
        }
        Instant now = Instant.now(clock);
        int triggered = 0;

        for (UUID id : impactedCandidateIds) {
            ExScheduleCandidate candidate = candidateRepo.findById(id).orElse(null);
            if (candidate == null) continue;
            // CONFIRMED candidate 는 재전환 금지 (Sprint 4 BR-V07 D-Day lock 정합)
            if (candidate.getStatus() == CandidateStatus.CONFIRMED) {
                log.warn("CONFIRMED candidate {} replan skip (Sprint 4 BR-V07 lock)", id);
                continue;
            }
            candidate.transitionTo(CandidateStatus.PENDING, now);
            candidateRepo.save(candidate);
            triggered++;
        }

        log.info("PartialReplanService — impactedCandidates={}, triggered={}",
            impactedCandidateIds.size(), triggered);
        return triggered;
    }
}
