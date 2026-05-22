package com.scheduling.ex.event;

import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.vc.events.VcChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * VC 변경 → 영향 EX row 식별 — TK-EX13-1-2 (EP-EX13 ST-EX13-1).
 *
 * <p>VcChangedRow 별로 영향받는 ExScheduleCandidate 식별. 1:1 매핑된 candidate
 * (vc_row_id) + 같은 (hoseId, 이전 vc_date 근처) candidate 검색.
 *
 * <p>알고리즘:
 * <ul>
 *   <li>DELETED — vc_row_id 직접 매핑 candidate 만</li>
 *   <li>QUANTITY — vc_row_id 직접 매핑 (yield 재계산 필요)</li>
 *   <li>DATE — vc_row_id 직접 + hoseId 같은 horizon 인접 candidate (재배정 필요)</li>
 *   <li>MACHINE — vc_row_id 직접 (machine_pin / 좌우 변경 영향)</li>
 * </ul>
 */
@Component
@Profile("with-infra")
public class ImpactedRowFinder {

    private static final Logger log = LoggerFactory.getLogger(ImpactedRowFinder.class);

    private final ExScheduleCandidateRepository candidateRepo;

    public ImpactedRowFinder(ExScheduleCandidateRepository candidateRepo) {
        this.candidateRepo = candidateRepo;
    }

    /**
     * VcChangedEvent → 영향 ExScheduleCandidate IDs.
     *
     * @return 중복 제거된 candidate IDs (BR-E11 partial replan 입력)
     */
    public List<UUID> findImpacted(VcChangedEvent event) {
        if (event == null || event.changedRows().isEmpty()) return List.of();

        Set<UUID> impacted = new HashSet<>();
        for (VcChangedEvent.VcChangedRow row : event.changedRows()) {
            // 1. vc_row_id 직접 매핑 candidate
            Optional<ExScheduleCandidate> direct = candidateRepo.findByVcRowId(row.rowId());
            direct.ifPresent(c -> impacted.add(c.getExCandidateId()));

            // 2. DATE 변경 시 horizon 인접 candidate 도 포함 (재배정 가능성)
            if (row.changeType() == VcChangedEvent.ChangeType.DATE
                && row.previousDate() != null && row.newDate() != null) {
                // hoseId 같은 candidate 중 이전 또는 신규 일자 근방 (±3 일)
                var earliest = row.previousDate().isBefore(row.newDate())
                    ? row.previousDate() : row.newDate();
                var latest = row.previousDate().isAfter(row.newDate())
                    ? row.previousDate() : row.newDate();
                List<ExScheduleCandidate> nearby = candidateRepo
                    .findByHoseIdAndExtrusionDeadlineBetween(
                        row.hoseId(), earliest.minusDays(3), latest.plusDays(3));
                nearby.forEach(c -> impacted.add(c.getExCandidateId()));
            }
        }

        log.info("ImpactedRowFinder — changedRows={}, impactedCandidates={}",
            event.changedRows().size(), impacted.size());
        return new ArrayList<>(impacted);
    }
}
