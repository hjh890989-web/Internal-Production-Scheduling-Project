package com.scheduling.ex.confirm;

import com.scheduling.ex.events.ExConfirmedEvent;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EX candidate Planner 확정 — TK-10-2-1 (EP-10 ST-10-2, BR-X01).
 *
 * <p>SCHEDULED → CONFIRMED 전이 + audit 필드 + {@link ExConfirmedEvent} 발행.
 * RBAC ROLE_PLANNER 강제 (Controller 레벨 @PreAuthorize).
 */
@Service
@Profile("with-infra")
public class ExCandidateConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(ExCandidateConfirmationService.class);

    private final ExScheduleCandidateRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ExCandidateConfirmationService(
        ExScheduleCandidateRepository repository,
        ApplicationEventPublisher eventPublisher,
        Clock clock
    ) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ExScheduleCandidate confirm(UUID candidateId, String plannerId) {
        ExScheduleCandidate c = repository.findById(candidateId)
            .orElseThrow(() -> new IllegalArgumentException(
                "ex_candidate_id 미존재: " + candidateId));
        Instant now = Instant.now(clock);
        c.confirm(plannerId, now);
        repository.save(c);
        log.info("EX candidate confirmed — id={}, planner={}", candidateId, plannerId);
        return c;
    }

    @Transactional
    public int confirmBatch(List<UUID> candidateIds, String plannerId, UUID batchId) {
        Instant now = Instant.now(clock);
        List<UUID> confirmed = new ArrayList<>();

        for (UUID id : candidateIds) {
            ExScheduleCandidate c = repository.findById(id).orElse(null);
            if (c == null) continue;
            c.confirm(plannerId, now);
            repository.save(c);
            confirmed.add(c.getExCandidateId());
        }

        if (!confirmed.isEmpty()) {
            eventPublisher.publishEvent(new ExConfirmedEvent(batchId, now, plannerId, confirmed));
            log.info("EX batch confirmed — batchId={}, candidates={}, planner={}",
                batchId, confirmed.size(), plannerId);
        }
        return confirmed.size();
    }
}
