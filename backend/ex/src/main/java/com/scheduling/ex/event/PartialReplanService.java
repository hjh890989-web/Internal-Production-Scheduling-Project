package com.scheduling.ex.event;

import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
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
    private final Clock clock;

    public PartialReplanService(ExScheduleCandidateRepository candidateRepo, Clock clock) {
        this.candidateRepo = candidateRepo;
        this.clock = clock;
    }

    /**
     * 영향 candidate 들을 PENDING 으로 재전환 (Sprint 3 stub).
     *
     * <p>Sprint 4+ 에서 PartialReplan 알고리즘 정식 구현 — yield 재계산 + grouping
     * 재배치 + gate 재실행 + 충돌 리포트.
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
