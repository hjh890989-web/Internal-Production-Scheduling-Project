package com.scheduling.vc.allocator;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.master.api.SlotCompatibilityQuery;
import com.scheduling.vc.capacity.SlotAvailability;
import com.scheduling.vc.deadline.BackwardDeadlineCalculator;
import com.scheduling.vc.deadline.DeadlineMap;
import com.scheduling.vc.domain.RotationSlot;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleStatus;
import com.scheduling.vc.required.OrderInput;
import com.scheduling.vc.rule.LeftRightRule;
import com.scheduling.vc.routing.DecisionType;
import com.scheduling.vc.routing.MachineType;
import com.scheduling.vc.routing.MachineTypeRoutingPolicy;
import com.scheduling.vc.routing.RoutingAuditLogger;
import com.scheduling.vc.routing.RoutingContext;
import com.scheduling.vc.routing.RoutingPolicyResolver;
import com.scheduling.vc.yield.AngleCapacityValidator;
import com.scheduling.vc.yield.AngleCapacityViolation;
import com.scheduling.vc.yield.VcYieldCalculator;
import com.scheduling.vc.yield.YieldMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 회전 배치 알고리즘 v1 (단순 greedy) — TK-05-3-2 ⭐⭐ Sprint 2 핵심.
 *
 * <p>알고리즘:
 * <ol>
 *   <li>Q_required 큰 품번부터 정렬 (greedy heuristic)</li>
 *   <li>각 품번:
 *     <ol>
 *       <li>Unschedulable matrix 포함 → AllocationConflict.UNSCHEDULABLE</li>
 *       <li>호라이즌 영업일 순회</li>
 *       <li>저압 (LP) 우선 → IC (ST-05-4 라우팅 외부화 baseline)</li>
 *       <li>회전 1~18 + 슬롯 시도 — AVAILABLE + matrix.isEligible + 앵글 capa 통과 시 배치</li>
 *       <li>누적 yield ≥ Q_required 시 다음 품번</li>
 *     </ol>
 *   </li>
 *   <li>최종 사후 검증 — AngleCapacityValidator (방어적)</li>
 * </ol>
 *
 * <p>결정성 — 같은 입력 → 같은 출력 (회귀 안정성). SchedulingMetrics emit (duration + conflicts).
 *
 * <p>{@code @Profile("with-infra")} — VcYieldCalculator + SlotCompatibilityQuery 의존.
 */
@Component
@Profile("with-infra")
public class GreedyRotationAllocator {

    private static final Logger log = LoggerFactory.getLogger(GreedyRotationAllocator.class);

    private final SlotCompatibilityQuery compatQuery;
    private final VcYieldCalculator yieldCalc;
    private final AngleCapacityValidator angleValidator;
    private final RoutingPolicyResolver policyResolver;
    private final RoutingAuditLogger auditLogger;
    private final BackwardDeadlineCalculator deadlineCalc;
    private final LeftRightRule leftRightRule;
    private final SchedulingMetrics metrics;
    private final Clock clock;

    public GreedyRotationAllocator(
        SlotCompatibilityQuery compatQuery,
        VcYieldCalculator yieldCalc,
        AngleCapacityValidator angleValidator,
        RoutingPolicyResolver policyResolver,
        RoutingAuditLogger auditLogger,
        BackwardDeadlineCalculator deadlineCalc,
        LeftRightRule leftRightRule,
        SchedulingMetrics metrics,
        Clock clock
    ) {
        this.compatQuery = compatQuery;
        this.yieldCalc = yieldCalc;
        this.angleValidator = angleValidator;
        this.policyResolver = policyResolver;
        this.auditLogger = auditLogger;
        this.deadlineCalc = deadlineCalc;
        this.leftRightRule = leftRightRule;
        this.metrics = metrics;
        this.clock = clock;
    }

    public AllocationResult allocate(AllocationContext ctx) {
        long startNanos = System.nanoTime();
        Set<String> unschedulable = compatQuery.unschedulableHoseIds();
        YieldMatrix yieldMatrix = yieldCalc.currentMatrix();
        MachineTypeRoutingPolicy policy = policyResolver.resolve();
        String policyId = policy.policyId();

        // BR-X07 — D-2 deadline 계산 (TK-06-1-2). 같은 hose 다중 수주 = 가장 이른 납기 기준.
        DeadlineMap deadlines = deadlineCalc.compute(ctx.ordersByHose());

        // 슬롯 점유 추적 (in-memory mutable copy)
        Map<RotationSlot, SlotAvailability> mutableCells = new HashMap<>(ctx.ledger().cells());

        List<VcSchedule> schedules = new ArrayList<>();
        List<AllocationConflict> conflicts = new ArrayList<>();

        // 1. Q_required 큰 품번부터 (결정성 위해 동률 시 hose_id asc)
        List<Map.Entry<String, Integer>> sorted = ctx.qRequired().entrySet().stream()
            .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey))
            .toList();

        Instant now = Instant.now(clock);

        for (Map.Entry<String, Integer> hoseEntry : sorted) {
            String hose = hoseEntry.getKey();
            int target = hoseEntry.getValue();

            if (unschedulable.contains(hose)) {
                conflicts.add(AllocationConflict.unschedulable(hose, target));
                continue;
            }

            int cumulativeYield = 0;
            int slotsUsed = 0;
            // 라우팅 정책 — TK-05-4-1: BR-V08 LP 우선 default, ConfigProperties 로 IC_FIRST 등 외부화
            List<MachineType> machineTypeOrder = policy.prioritize(hose, RoutingContext.initial(ctx.workingDays().isEmpty() ? null : ctx.workingDays().get(0)));

            // BR-X07 — 본 hose 의 D-2 deadline (없으면 무제한)
            LocalDate deadline = deadlines.get(hose).orElse(null);

            // 호라이즌 영업일 → 머신 유형 → 회전·슬롯 순회
            for (LocalDate date : ctx.workingDays()) {
                if (cumulativeYield >= target) break;
                if (deadline != null && date.isAfter(deadline)) break;  // BR-X07 hard 제약

                int routingIndex = 0;
                for (MachineType mt : machineTypeOrder) {
                    if (cumulativeYield >= target) break;
                    String machineType = mt.name();

                    int yieldPerRot = yieldMatrix.lookup(hose, machineType).orElse(0);
                    if (yieldPerRot <= 0) {
                        routingIndex++;
                        continue;
                    }

                    // 해당 머신 유형의 AVAILABLE 슬롯 정렬
                    List<RotationSlot> candidates = mutableCells.entrySet().stream()
                        .filter(e -> e.getValue() == SlotAvailability.AVAILABLE)
                        .filter(e -> e.getKey().date().equals(date))
                        .filter(e -> isMachineTypeOf(e.getKey().machineId(), machineType))
                        .map(Map.Entry::getKey)
                        .sorted(Comparator.<RotationSlot>comparingInt(RotationSlot::rotationNo)
                            .thenComparing(RotationSlot::machineId)
                            .thenComparingInt(RotationSlot::slotPosition))
                        .toList();

                    for (RotationSlot slot : candidates) {
                        if (cumulativeYield >= target) break;

                        // a) Slot O/X 검증
                        if (!compatQuery.isEligible(hose, toSlotPositionName(slot, machineType))) continue;

                        // a.5) LP 좌/우 셋팅 (BR-V15·V16) — TK-21-1-2
                        if (!leftRightRule.validate(hose, slot.machineId())) continue;

                        // b) 앵글 capa 사전 — 동일 (machine, date, rotation) 의 같은 hose 점유 슬롯 수 + 1
                        if (!fitsAngleCapacity(hose, machineType, slot, schedules)) continue;

                        // 배치
                        VcSchedule vc = new VcSchedule(
                            UUID.randomUUID(),
                            hose,
                            slot.machineId(),
                            (short) slot.slotPosition(),
                            slot.date(),
                            (short) slot.rotationNo(),
                            "ANGLE-" + hose + "-" + slot.rotationNo(),     // angle_id placeholder (Phase 2 정합)
                            yieldPerRot,
                            VcScheduleStatus.CANDIDATE,
                            linkedOrderIdsCsv(ctx.ordersByHose().get(hose)),
                            now, now
                        );
                        schedules.add(vc);
                        mutableCells.put(slot, SlotAvailability.RESERVED);
                        cumulativeYield += yieldPerRot;
                        slotsUsed++;

                        // BR-X02 — 라우팅 결정 audit
                        DecisionType decisionType = classifyDecision(machineType, routingIndex);
                        auditLogger.logDecision(
                            hose, slot.date(), slot.machineId(), machineType,
                            decisionType, policyId,
                            "Greedy 배치 (cumulative=" + cumulativeYield + "/" + target + ")");
                    }
                    routingIndex++;
                }
            }

            if (cumulativeYield < target) {
                // deadline 내 capa 부족 → BR-X07 별도 카테고리, 그 외 일반 capa
                if (deadline != null) {
                    conflicts.add(AllocationConflict.deadlineExceeded(
                        hose, target, cumulativeYield, deadline));
                } else {
                    conflicts.add(AllocationConflict.insufficientCapacity(
                        hose, target, cumulativeYield, slotsUsed));
                }
            }
        }

        // 사후 검증 — AngleCapacityValidator (방어적)
        Map<String, List<RotationSlot>> assignments = schedules.stream()
            .collect(Collectors.groupingBy(
                VcSchedule::getHoseId,
                Collectors.mapping(VcSchedule::asSlot, Collectors.toList())));
        List<AngleCapacityViolation> angleViols = angleValidator.validate(assignments);
        for (AngleCapacityViolation v : angleViols) {
            conflicts.add(AllocationConflict.fromAngleViolation(v));
        }

        long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        log.info("Greedy allocate — hoses={} target_total={} schedules={} conflicts={} elapsed={}ms",
            sorted.size(),
            sorted.stream().mapToInt(Map.Entry::getValue).sum(),
            schedules.size(), conflicts.size(), elapsedMs);

        if (metrics != null) {
            metrics.recordDuration("vc_allocator", "duration", Duration.ofMillis(elapsedMs));
            if (!conflicts.isEmpty()) {
                metrics.increment("vc_allocator", "conflicts");
            }
        }
        return new AllocationResult(schedules, conflicts);
    }

    /** 같은 (machine, date, rotation) 의 같은 hose 점유 슬롯 + 1 ≤ angle_qty. */
    private boolean fitsAngleCapacity(String hose, String machineType, RotationSlot newSlot,
                                       Collection<VcSchedule> alreadyPlaced) {
        long sameRotation = alreadyPlaced.stream()
            .filter(s -> s.getHoseId().equals(hose))
            .filter(s -> s.getMachineId().equals(newSlot.machineId()))
            .filter(s -> s.getProductionDate().equals(newSlot.date()))
            .filter(s -> s.getRotationNo() == newSlot.rotationNo())
            .count();
        int requested = (int) sameRotation + 1;
        return angleValidator.isWithinCapacity(hose, machineType, requested);
    }

    /**
     * 라우팅 결정 분류 — TK-05-4-2.
     *
     * <p>{@code routingIndex} = policy.prioritize() 결과 내의 시도 순서 (0 = primary, 1+ = fallback).
     */
    private DecisionType classifyDecision(String machineType, int routingIndex) {
        if ("LP".equals(machineType)) {
            return routingIndex == 0 ? DecisionType.LP_PRIMARY : DecisionType.LP_FALLBACK;
        }
        return routingIndex == 0 ? DecisionType.IC_PRIMARY : DecisionType.IC_FALLBACK;
    }

    private boolean isMachineTypeOf(String machineId, String machineType) {
        if ("LP".equals(machineType)) return machineId.startsWith("LP-");
        if ("IC".equals(machineType)) return machineId.startsWith("IC-");
        return false;
    }

    /** RotationSlot 의 slotPosition (int) → SlotPosition enum name (예: LP_TOP, IC_BOT). */
    private String toSlotPositionName(RotationSlot slot, String machineType) {
        // LP 4 슬롯: 1=TOP, 2=UPMID, 3=LOWMID, 4=BOT
        // IC 3 슬롯: 1=TOP, 2=MID, 3=BOT
        if ("LP".equals(machineType)) {
            return switch (slot.slotPosition()) {
                case 1 -> "LP_TOP";
                case 2 -> "LP_UPMID";
                case 3 -> "LP_LOWMID";
                case 4 -> "LP_BOT";
                default -> "LP_TOP"; // 5~8 슬롯은 정의 외 — SlotCompatibilityMatrix 가 false 반환
            };
        }
        return switch (slot.slotPosition()) {
            case 1 -> "IC_TOP";
            case 2 -> "IC_MID";
            case 3 -> "IC_BOT";
            default -> "IC_TOP";
        };
    }

    private String linkedOrderIdsCsv(List<OrderInput> orders) {
        if (orders == null || orders.isEmpty()) return "";
        return orders.stream().map(o -> o.orderId().toString()).collect(Collectors.joining(","));
    }
}
