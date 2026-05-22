package com.scheduling.ex.event;

import com.scheduling.ex.deadline.BackwardExtrusionCalculator;
import com.scheduling.ex.deadline.ExDeadlineMap;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.vc.events.VcConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * VC 확정 → 압출 후보 1:1 생성 — TK-07-1-2 (EP-07 ST-07-1).
 *
 * <p>BR-E01: extrusionDeadline = vcProductionDate − 1 working day. 호스별 가장 이른
 * vc_date 기준 (hard 제약). 멱등 — 같은 vc_row_id 재발행 시 UPSERT (기존 row 보존).
 *
 * <p>EP-08 (yield 계산) 진입 전 candidate 단계 (status=PENDING).
 */
@Service
@Profile("with-infra")
public class ExtrusionScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ExtrusionScheduleService.class);

    private final BackwardExtrusionCalculator deadlineCalc;
    private final ExScheduleCandidateRepository candidateRepo;
    private final Clock clock;

    public ExtrusionScheduleService(
        BackwardExtrusionCalculator deadlineCalc,
        ExScheduleCandidateRepository candidateRepo,
        Clock clock
    ) {
        this.deadlineCalc = deadlineCalc;
        this.candidateRepo = candidateRepo;
        this.clock = clock;
    }

    /**
     * VC 확정 이벤트 → 압출 후보 row 생성.
     *
     * @return 생성된 후보 수 (재발행 시 기존 row 는 보존, 신규만 카운트)
     */
    @Transactional
    public int generateCandidates(VcConfirmedEvent event) {
        ExDeadlineMap deadlines = deadlineCalc.compute(event);
        Instant now = Instant.now(clock);
        int created = 0;

        for (VcConfirmedEvent.VcConfirmedRow row : event.rows()) {
            // 멱등 — 동일 vc_row_id 존재 시 skip (UPSERT 유사)
            Optional<ExScheduleCandidate> existing = candidateRepo.findByVcRowId(row.rowId());
            if (existing.isPresent()) continue;

            LocalDate deadline = deadlines.get(row.hoseId())
                .orElseThrow(() -> new IllegalStateException(
                    "deadline 계산 누락: " + row.hoseId()));

            ExScheduleCandidate candidate = new ExScheduleCandidate(
                UUID.randomUUID(),
                event.scheduleId(),
                row.hoseId(),
                row.rowId(),
                row.productionDate(),
                deadline,
                row.vcYield(),
                CandidateStatus.PENDING,
                now, now);
            candidateRepo.save(candidate);
            created++;
        }

        log.info("ExtrusionScheduleService — scheduleId={}, vc_rows={}, created={}",
            event.scheduleId(), event.rows().size(), created);
        return created;
    }
}
