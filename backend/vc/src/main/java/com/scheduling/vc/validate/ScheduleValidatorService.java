package com.scheduling.vc.validate;

import com.scheduling.master.api.HoseRuleLookup;
import com.scheduling.master.api.SlotCompatibilityQuery;
import com.scheduling.master.api.VcHoseRuleSummary;
import com.scheduling.vc.allocator.AllocationConflict;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.rule.LeftRightRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * On-Demand 전체 스케줄 검증 — TK-VC16-1-1 (REQ-FUNC-VC-016).
 *
 * <p>기 확정 {@link VcSchedule} row 들을 현재 마스터·룰 기반으로 일괄 재검증.
 * EP-04 ST-04-1 슬롯 O/X + EP-21 ST-21-1 좌/우 + EP-21 ST-21-2 호기 핀·LP-only +
 * UNIQUE 슬롯 검증 통합.
 *
 * <p>설계:
 * <ul>
 *   <li>모든 위반 수집 (early-exit 금지) — 50건 위반 시 50건 모두 반환</li>
 *   <li>row 단위 독립 검증 — parallelStream 활용 (CPU 코어 활용)</li>
 *   <li>summary — 카테고리별 카운트 (UI 1차 그루핑용)</li>
 * </ul>
 *
 * <p>{@code @Profile("with-infra")} — VcScheduleRepository / 외부 lookup 의존.
 *
 * <p>마스터 변경 retroactive 검증: 본 서비스는 호출 시점의 최신 마스터·룰 캐시로 평가 —
 * 신규 휴일 추가 / VC_HOSE_RULE 변경 후 즉시 발견 (TC-VC-016 시나리오).
 */
@Service
@Profile("with-infra")
public class ScheduleValidatorService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleValidatorService.class);

    private final VcScheduleRepository scheduleRepo;
    private final SlotCompatibilityQuery compatQuery;
    private final HoseRuleLookup hoseRuleLookup;
    private final LeftRightRule leftRightRule;
    private final Clock clock;

    public ScheduleValidatorService(
        VcScheduleRepository scheduleRepo,
        SlotCompatibilityQuery compatQuery,
        HoseRuleLookup hoseRuleLookup,
        LeftRightRule leftRightRule,
        Clock clock
    ) {
        this.scheduleRepo = scheduleRepo;
        this.compatQuery = compatQuery;
        this.hoseRuleLookup = hoseRuleLookup;
        this.leftRightRule = leftRightRule;
        this.clock = clock;
    }

    /**
     * 호라이즌 내 모든 VcSchedule row 검증.
     *
     * @param from horizon 시작 (포함)
     * @param to   horizon 끝 (포함)
     * @return 위반 목록 + 카테고리별 summary
     */
    public ValidationResult validateRange(LocalDate from, LocalDate to) {
        long startNanos = System.nanoTime();
        List<VcSchedule> rows = scheduleRepo.findByDateRange(from, to);
        Set<String> unschedulable = compatQuery.unschedulableHoseIds();

        // 중복 슬롯 사전 집계 (UNIQUE 위반 — 동일 machine+date+rotation+slot 2건 이상)
        Map<String, List<UUID>> slotKey = new HashMap<>();
        for (VcSchedule s : rows) {
            String key = s.getMachineId() + "/" + s.getProductionDate()
                + "/" + s.getRotationNo() + "/" + s.getSlotPosition();
            slotKey.computeIfAbsent(key, k -> new ArrayList<>()).add(s.getVcScheduleId());
        }

        List<ValidationViolation> violations = rows.stream()
            .flatMap(row -> validateRow(row, unschedulable, slotKey).stream())
            .toList();

        Map<AllocationConflict.Category, Long> summary = violations.stream()
            .collect(Collectors.groupingBy(ValidationViolation::category, Collectors.counting()));

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("validateRange [{} ~ {}] — rows={} violations={} elapsed={}ms",
            from, to, rows.size(), violations.size(), elapsedMs);

        return new ValidationResult(from, to, violations, summary, rows.size(), Instant.now(clock));
    }

    private List<ValidationViolation> validateRow(VcSchedule row,
                                                  Set<String> unschedulable,
                                                  Map<String, List<UUID>> slotKey) {
        List<ValidationViolation> out = new ArrayList<>();

        // 1. Unschedulable (BR-V11)
        if (unschedulable.contains(row.getHoseId())) {
            out.add(violation(row, AllocationConflict.Category.UNSCHEDULABLE,
                "UNSCHEDULABLE — 모든 슬롯 X (BR-V11) — 외주·재고 대응 권고"));
        }

        // 2. Slot O/X (BR-V01)
        String slotName = toSlotPositionName(row);
        if (slotName != null && !compatQuery.isEligible(row.getHoseId(), slotName)) {
            out.add(violation(row, AllocationConflict.Category.ANGLE_VIOLATION,
                "슬롯 O/X 위반 — %s × %s 미적합 (BR-V01)".formatted(row.getHoseId(), slotName)));
        }

        // 3. LP 좌/우 셋팅 (BR-V15·V16)
        if (!leftRightRule.validate(row.getHoseId(), row.getMachineId())) {
            out.add(violation(row, AllocationConflict.Category.LEFT_RIGHT_VIOLATION,
                "LP 좌/우 셋팅 위반 — %s × %s (BR-V15·V16)".formatted(row.getHoseId(), row.getMachineId())));
        }

        // 4. 호기 핀·LP only (BR-V14)
        Optional<VcHoseRuleSummary> ruleOpt = hoseRuleLookup.findById(row.getHoseId());
        if (ruleOpt.isPresent()) {
            VcHoseRuleSummary r = ruleOpt.get();
            if (r.hasMachinePin() && !r.machinePin().equals(row.getMachineId())) {
                out.add(violation(row, AllocationConflict.Category.LEFT_RIGHT_VIOLATION,
                    "호기 핀 위반 — 품번 %s 는 %s 만 사용 가능 (현재: %s, BR-V14)"
                        .formatted(row.getHoseId(), r.machinePin(), row.getMachineId())));
            }
            if (r.lpOnly() && row.getMachineId().startsWith("IC-")) {
                out.add(violation(row, AllocationConflict.Category.LEFT_RIGHT_VIOLATION,
                    "lp_only 위반 — 품번 %s 는 IC 사용 금지 (현재: %s, BR-V14)"
                        .formatted(row.getHoseId(), row.getMachineId())));
            }
        }

        // 5. 중복 슬롯 (UNIQUE 위반)
        String key = row.getMachineId() + "/" + row.getProductionDate()
            + "/" + row.getRotationNo() + "/" + row.getSlotPosition();
        List<UUID> sameSlot = slotKey.getOrDefault(key, List.of());
        if (sameSlot.size() > 1) {
            out.add(violation(row, AllocationConflict.Category.INSUFFICIENT_CAPACITY,
                "중복 슬롯 — %s 점유 %d건 (UNIQUE 위반)".formatted(key, sameSlot.size())));
        }

        return out;
    }

    private ValidationViolation violation(VcSchedule row,
                                          AllocationConflict.Category category, String reason) {
        return new ValidationViolation(
            row.getVcScheduleId(), row.getHoseId(), row.getProductionDate(),
            row.getMachineId(), category, reason);
    }

    /** RotationSlot → SlotPosition enum name (예: LP_TOP). */
    private String toSlotPositionName(VcSchedule row) {
        if (row.getMachineId().startsWith("LP-")) {
            return switch (row.getSlotPosition()) {
                case 1 -> "LP_TOP";
                case 2 -> "LP_UPMID";
                case 3 -> "LP_LOWMID";
                case 4 -> "LP_BOT";
                default -> null;
            };
        }
        if (row.getMachineId().startsWith("IC-")) {
            return switch (row.getSlotPosition()) {
                case 1 -> "IC_TOP";
                case 2 -> "IC_MID";
                case 3 -> "IC_BOT";
                default -> null;
            };
        }
        return null;
    }
}
