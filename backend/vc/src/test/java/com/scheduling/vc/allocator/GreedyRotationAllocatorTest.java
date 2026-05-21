package com.scheduling.vc.allocator;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.master.api.SlotCompatibilityQuery;
import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.SlotAvailability;
import com.scheduling.vc.domain.RotationSlot;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.required.OrderInput;
import com.scheduling.vc.routing.LpFirstThenIcRoutingPolicy;
import com.scheduling.vc.routing.RoutingAuditLogger;
import com.scheduling.vc.routing.RoutingPolicyResolver;
import com.scheduling.vc.yield.AngleCapacityValidator;
import com.scheduling.vc.yield.VcYieldCalculator;
import com.scheduling.vc.yield.YieldMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GreedyRotationAllocator 단위 회귀 — TK-05-3-2 (REQ-FUNC-VC-010).
 *
 * <p>Mockito 로 dependencies 격리 (SlotCompatibilityQuery / VcYieldCalculator / AngleCapacityValidator).
 * CapacityLedger 는 in-memory 합성.
 */
class GreedyRotationAllocatorTest {

    private static final LocalDate D = LocalDate.of(2026, 2, 16);
    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-05-21T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private SlotCompatibilityQuery compatQuery;
    private VcYieldCalculator yieldCalc;
    private AngleCapacityValidator angleValidator;
    private RoutingPolicyResolver policyResolver;
    private RoutingAuditLogger auditLogger;
    private SchedulingMetrics metrics;
    private GreedyRotationAllocator allocator;

    @BeforeEach
    void setUp() {
        compatQuery = mock(SlotCompatibilityQuery.class);
        yieldCalc = mock(VcYieldCalculator.class);
        angleValidator = mock(AngleCapacityValidator.class);
        policyResolver = mock(RoutingPolicyResolver.class);
        auditLogger = mock(RoutingAuditLogger.class);
        metrics = mock(SchedulingMetrics.class);
        allocator = new GreedyRotationAllocator(
            compatQuery, yieldCalc, angleValidator,
            policyResolver, auditLogger, metrics, CLOCK);

        // 기본: 모든 슬롯 eligible, 앵글 capa 무한, validator 후 위반 0
        lenient().when(compatQuery.isEligible(anyString(), anyString())).thenReturn(true);
        lenient().when(compatQuery.unschedulableHoseIds()).thenReturn(Set.of());
        lenient().when(angleValidator.isWithinCapacity(anyString(), anyString(), anyInt())).thenReturn(true);
        lenient().when(angleValidator.validate(org.mockito.ArgumentMatchers.anyMap())).thenReturn(List.of());
        // 기본 정책 — LP_FIRST (BR-V08)
        lenient().when(policyResolver.resolve()).thenReturn(new LpFirstThenIcRoutingPolicy());
    }

    /** LP 4대 × 18 회전 × 4 슬롯 (TOP/UPMID/LOWMID/BOT) AVAILABLE 격자 — 단순 합성. */
    private CapacityLedger fullLedger() {
        Map<RotationSlot, SlotAvailability> cells = new HashMap<>();
        for (int rot = 1; rot <= 18; rot++) {
            for (int slot = 1; slot <= 4; slot++) {
                for (String m : List.of("LP-01", "LP-02", "LP-03", "LP-04")) {
                    cells.put(new RotationSlot(D, m, rot, slot), SlotAvailability.AVAILABLE);
                }
            }
            for (int slot = 1; slot <= 3; slot++) {
                cells.put(new RotationSlot(D, "IC-01", rot, slot), SlotAvailability.AVAILABLE);
            }
        }
        return new CapacityLedger(D, D, cells);
    }

    private AllocationContext ctx(Map<String, Integer> qRequired, CapacityLedger ledger) {
        Map<String, List<OrderInput>> orders = new HashMap<>();
        qRequired.forEach((hose, q) ->
            orders.put(hose, List.of(new OrderInput(UUID.randomUUID(), hose, D, q))));
        return new AllocationContext(qRequired, orders, ledger, List.of(D));
    }

    private void yields(String hose, int lpY, int icY) {
        Map<String, Integer> lp = lpY > 0 ? Map.of(hose, lpY) : Map.of();
        Map<String, Integer> ic = icY > 0 ? Map.of(hose, icY) : Map.of();
        YieldMatrix m = new YieldMatrix(1, Instant.now(CLOCK), lp, ic, Set.of());
        when(yieldCalc.currentMatrix()).thenReturn(m);
    }

    // ---------- 정상 흐름 ----------

    @Test
    @DisplayName("단일 품번 Q_required=10, LP yield=1 → 10 schedules 정확 생성")
    void single_hose_exact_yield() {
        yields("A", 1, 0);
        AllocationResult r = allocator.allocate(ctx(Map.of("A", 10), fullLedger()));

        assertThat(r.scheduleCount()).isEqualTo(10);
        assertThat(r.hasConflicts()).isFalse();
        assertThat(r.schedules()).allSatisfy(s ->
            assertThat(s.getMachineId()).startsWith("LP-"));
    }

    @Test
    @DisplayName("yield > 1 — 회전 1회로 yield 충족 시 1 schedule 만 생성")
    void single_hose_yield_covers_target_one_rotation() {
        yields("A", 15, 0);                  // composite 3 × molds 5 = 15
        AllocationResult r = allocator.allocate(ctx(Map.of("A", 10), fullLedger()));

        // yield=15 > target=10 → 1 schedule 로 누적 yield 15 ≥ 10
        assertThat(r.scheduleCount()).isEqualTo(1);
        assertThat(r.schedules().get(0).getPlannedQty()).isEqualTo(15);
    }

    @Test
    @DisplayName("여러 품번 — Q_required 큰 것 우선 배치")
    void multiple_hoses_sorted_by_qrequired_desc() {
        // LP yield = 모두 1 — slot 점유 순서로 정렬 검증
        YieldMatrix m = new YieldMatrix(1, Instant.now(CLOCK),
            Map.of("BIG", 1, "SMALL", 1), Map.of(), Set.of());
        when(yieldCalc.currentMatrix()).thenReturn(m);

        AllocationResult r = allocator.allocate(ctx(Map.of("BIG", 5, "SMALL", 2), fullLedger()));

        // BIG 먼저 5 → SMALL 2 = 7 schedules
        assertThat(r.scheduleCount()).isEqualTo(7);
        List<String> hoseOrder = r.schedules().stream().map(VcSchedule::getHoseId).toList();
        // 앞 5개 = BIG, 뒤 2개 = SMALL
        assertThat(hoseOrder.subList(0, 5)).containsOnly("BIG");
        assertThat(hoseOrder.subList(5, 7)).containsOnly("SMALL");
    }

    // ---------- Unschedulable (BR-V11) ----------

    @Test
    @DisplayName("Unschedulable 품번 → AllocationConflict.UNSCHEDULABLE + schedule 0")
    void unschedulable_hose_creates_conflict() {
        when(compatQuery.unschedulableHoseIds()).thenReturn(Set.of("UNSCHED"));
        yields("UNSCHED", 1, 1);

        AllocationResult r = allocator.allocate(ctx(Map.of("UNSCHED", 5), fullLedger()));

        assertThat(r.schedules()).isEmpty();
        assertThat(r.conflicts()).hasSize(1);
        AllocationConflict c = r.conflicts().get(0);
        assertThat(c.category()).isEqualTo(AllocationConflict.Category.UNSCHEDULABLE);
        assertThat(c.hoseId()).isEqualTo("UNSCHED");
        assertThat(c.reason()).contains("BR-V11");
    }

    // ---------- 용량 부족 ----------

    @Test
    @DisplayName("용량 부족 — 호라이즌 가용 < Q_required → INSUFFICIENT_CAPACITY conflict")
    void insufficient_capacity_creates_conflict() {
        yields("A", 1, 0);
        // 격자 4 슬롯만 (LP-01 × rotation 1~4 × slot 1) 가용
        Map<RotationSlot, SlotAvailability> cells = new HashMap<>();
        for (int rot = 1; rot <= 4; rot++) {
            cells.put(new RotationSlot(D, "LP-01", rot, 1), SlotAvailability.AVAILABLE);
        }
        CapacityLedger small = new CapacityLedger(D, D, cells);

        AllocationResult r = allocator.allocate(ctx(Map.of("A", 100), small));

        assertThat(r.scheduleCount()).isEqualTo(4);   // 4 × yield 1 = 4
        assertThat(r.conflicts()).hasSize(1);
        AllocationConflict c = r.conflicts().get(0);
        assertThat(c.category()).isEqualTo(AllocationConflict.Category.INSUFFICIENT_CAPACITY);
        assertThat(c.targetQty()).isEqualTo(100);
        assertThat(c.placedQty()).isEqualTo(4);
    }

    // ---------- 슬롯 O/X 강제 ----------

    @Test
    @DisplayName("isEligible=false 슬롯 → 건너뜀")
    void ineligible_slot_skipped() {
        yields("A", 1, 0);
        // LP_TOP 만 false — UPMID/LOWMID/BOT 만 사용
        when(compatQuery.isEligible("A", "LP_TOP")).thenReturn(false);

        AllocationResult r = allocator.allocate(ctx(Map.of("A", 4), fullLedger()));

        assertThat(r.scheduleCount()).isEqualTo(4);
        assertThat(r.schedules()).allSatisfy(s ->
            assertThat(s.getSlotPosition()).isNotEqualTo((short) 1));   // TOP=1 회피
    }

    // ---------- 저압 우선 라우팅 (ST-05-4 baseline) ----------

    @Test
    @DisplayName("LP yield + IC yield 모두 존재 → LP 우선 배치 (ST-05-4 baseline)")
    void lp_priority_baseline() {
        yields("A", 1, 1);

        AllocationResult r = allocator.allocate(ctx(Map.of("A", 5), fullLedger()));

        assertThat(r.scheduleCount()).isEqualTo(5);
        assertThat(r.schedules()).allSatisfy(s ->
            assertThat(s.getMachineId()).startsWith("LP-"));
    }

    @Test
    @DisplayName("LP yield 0 → IC 만 사용 (single-machine 품번)")
    void only_ic_when_lp_yield_zero() {
        yields("A", 0, 2);

        AllocationResult r = allocator.allocate(ctx(Map.of("A", 4), fullLedger()));

        assertThat(r.scheduleCount()).isEqualTo(2);   // 2 × yield 2 = 4
        assertThat(r.schedules()).allSatisfy(s ->
            assertThat(s.getMachineId()).startsWith("IC-"));
    }

    // ---------- 결정성 ----------

    @Test
    @DisplayName("결정성 — 같은 입력 5회 반복 → 같은 출력")
    void deterministic_same_input_same_output() {
        yields("A", 1, 0);
        AllocationContext context = ctx(Map.of("A", 5), fullLedger());

        AllocationResult r1 = allocator.allocate(context);
        AllocationResult r2 = allocator.allocate(context);

        assertThat(r1.scheduleCount()).isEqualTo(r2.scheduleCount());
        // 슬롯 (date, machine, rotation, slotPosition) 동일성 검증 — UUID 는 제외
        List<String> slots1 = r1.schedules().stream()
            .map(s -> s.getMachineId() + "/" + s.getRotationNo() + "/" + s.getSlotPosition())
            .toList();
        List<String> slots2 = r2.schedules().stream()
            .map(s -> s.getMachineId() + "/" + s.getRotationNo() + "/" + s.getSlotPosition())
            .toList();
        assertThat(slots1).isEqualTo(slots2);
    }

    // ---------- VcSchedule status / metadata ----------

    @Test
    @DisplayName("schedule status = CANDIDATE (BR-X01 사용자 확정 전)")
    void schedules_in_candidate_status() {
        yields("A", 1, 0);

        AllocationResult r = allocator.allocate(ctx(Map.of("A", 3), fullLedger()));

        assertThat(r.schedules()).allSatisfy(s ->
            assertThat(s.getStatus()).isEqualTo(com.scheduling.vc.domain.VcScheduleStatus.CANDIDATE));
    }

    @Test
    @DisplayName("linkedOrderIds CSV — orderInput 의 orderId 포함")
    void linked_order_ids_csv() {
        yields("A", 1, 0);
        UUID oid = UUID.randomUUID();
        AllocationContext context = new AllocationContext(
            Map.of("A", 2),
            Map.of("A", List.of(new OrderInput(oid, "A", D, 2))),
            fullLedger(),
            List.of(D));

        AllocationResult r = allocator.allocate(context);

        assertThat(r.schedules()).allSatisfy(s ->
            assertThat(s.getLinkedOrderIds()).contains(oid.toString()));
    }

    // ---------- 빈 입력 ----------

    @Test
    @DisplayName("빈 Q_required → schedule 0 + conflict 0")
    void empty_qrequired_no_output() {
        when(yieldCalc.currentMatrix()).thenReturn(
            new YieldMatrix(1, Instant.now(CLOCK), Map.of(), Map.of(), Set.of()));

        AllocationResult r = allocator.allocate(ctx(Map.of(), fullLedger()));

        assertThat(r.schedules()).isEmpty();
        assertThat(r.conflicts()).isEmpty();
    }

    // ---------- 메트릭 ----------

    @Test
    @DisplayName("metrics — duration timer + conflicts 카운터")
    void metrics_emitted() {
        when(compatQuery.unschedulableHoseIds()).thenReturn(Set.of("UNSCHED"));
        yields("UNSCHED", 1, 1);

        AllocationResult r = allocator.allocate(ctx(Map.of("UNSCHED", 5), fullLedger()));

        assertThat(r.hasConflicts()).isTrue();
        org.mockito.Mockito.verify(metrics).recordDuration(
            org.mockito.ArgumentMatchers.eq("vc_allocator"),
            org.mockito.ArgumentMatchers.eq("duration"),
            org.mockito.ArgumentMatchers.any(java.time.Duration.class));
        org.mockito.Mockito.verify(metrics).increment("vc_allocator", "conflicts");
    }
}
