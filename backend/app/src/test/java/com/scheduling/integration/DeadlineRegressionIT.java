package com.scheduling.integration;

import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.allocator.AllocationConflict;
import com.scheduling.vc.allocator.AllocationContext;
import com.scheduling.vc.allocator.AllocationResult;
import com.scheduling.vc.allocator.GreedyRotationAllocator;
import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.CapacityLedgerBuilder;
import com.scheduling.vc.deadline.BackwardDeadlineCalculator;
import com.scheduling.vc.deadline.DeadlineMap;
import com.scheduling.vc.required.OrderInput;
import com.scheduling.vc.routing.MachineDecisionRepository;
import com.scheduling.vc.yield.VcYieldCalculator;
import com.scheduling.vc.yield.YieldMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-06 ST-06-1 TK-06-1-3 — TC-VC-008 D-2 deadline 회귀 100건.
 *
 * <p>실 PG + DS-VC-CONSTRAINT-47 master_seed + V012 master.holiday (2026 KR 법정공휴일) +
 * GreedyRotationAllocator deadline filter:
 * <ul>
 *   <li>100 시나리오 — 각 시나리오마다 모든 schedule row {@code production_date ≤ deadline}</li>
 *   <li>K-V04 D-2 준수율 ≥ 98% (REQ-NF-KPI-008) — Q_required 1~10 + 호라이즌 2주</li>
 *   <li>같은 hose 다중 수주 — 가장 이른 납기 기준 deadline (hard 제약)</li>
 * </ul>
 *
 * <p>시나리오 random 생성 (seed 고정 → 재현 가능) — dual-capable 품번 + 다양한 납기 분포
 * (월~금, 설날·추석 직후 포함).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class DeadlineRegressionIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
    }

    @Autowired private GreedyRotationAllocator allocator;
    @Autowired private CapacityLedgerBuilder ledgerBuilder;
    @Autowired private BackwardDeadlineCalculator deadlineCalc;
    @Autowired private WorkingCalendar calendar;
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private VcYieldCalculator yieldCalc;
    @Autowired private MachineDecisionRepository auditRepo;

    /** 호라이즌 시작 — 설날 직후 첫 영업일 주 (2026-02-23 월). */
    private static final LocalDate HORIZON_START = LocalDate.of(2026, 2, 23);
    private static final int HORIZON_WEEKS = 2;

    @BeforeEach
    void rebuildCaches() {
        auditRepo.deleteAll();
        matrixService.invalidate();
        yieldCalc.rebuild();
    }

    private record Scenario(String scenarioId, List<OrderInput> orders) {}

    private List<String> dualCapableHoses() {
        YieldMatrix m = yieldCalc.currentMatrix();
        return m.lpYields().keySet().stream()
            .filter(h -> m.icYields().containsKey(h))
            .sorted()
            .toList();
    }

    /** 호라이즌 영업일 — calendar 기반 (휴일 제외). */
    private List<LocalDate> horizonWorkingDays() {
        LocalDate end = HORIZON_START.plusWeeks(HORIZON_WEEKS);
        return calendar.workingDaysInRange(HORIZON_START, end);
    }

    /** Random 100 시나리오 — seed 고정으로 재현 가능. */
    private List<Scenario> generateScenarios(int count) {
        Random rng = new Random(20260521L);
        List<String> hoses = dualCapableHoses();
        assertThat(hoses).as("dual-capable hose ≥ 5 (REF-09)").hasSizeGreaterThanOrEqualTo(5);

        List<LocalDate> horizon = horizonWorkingDays();
        assertThat(horizon).isNotEmpty();

        // 납기 후보 — horizon 끝 + 3~14 영업일 후 (deadline 이 horizon 내로 들어오도록)
        List<LocalDate> deliveryCandidates = new ArrayList<>();
        LocalDate horizonEnd = horizon.get(horizon.size() - 1);
        LocalDate d = horizonEnd;
        for (int i = 0; i < 30; i++) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            deliveryCandidates.add(d);
        }

        List<Scenario> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String hose = hoses.get(rng.nextInt(hoses.size()));
            LocalDate delivery = deliveryCandidates.get(rng.nextInt(deliveryCandidates.size()));
            int qty = 1 + rng.nextInt(10);
            out.add(new Scenario("SC-" + i,
                List.of(new OrderInput(UUID.randomUUID(), hose, delivery, qty))));
        }
        return out;
    }

    private AllocationContext context(Scenario s, List<LocalDate> workingDays) {
        Map<String, Integer> qRequired = new HashMap<>();
        Map<String, List<OrderInput>> ordersByHose = new HashMap<>();
        for (OrderInput o : s.orders()) {
            qRequired.merge(o.hoseId(), o.qty(), Integer::sum);
            ordersByHose.computeIfAbsent(o.hoseId(), k -> new ArrayList<>()).add(o);
        }
        CapacityLedger ledger = ledgerBuilder.build(workingDays.get(0), workingDays.get(workingDays.size() - 1));
        return new AllocationContext(qRequired, ordersByHose, ledger, workingDays);
    }

    // ---------- TC-VC-008 ----------

    @Test
    @DisplayName("100 시나리오 — 모든 schedule row production_date ≤ deadline (BR-X07)")
    void all_rows_within_d2_deadline_for_100_scenarios() {
        List<Scenario> scenarios = generateScenarios(100);
        assertThat(scenarios).hasSize(100);

        List<LocalDate> workingDays = horizonWorkingDays();

        int totalSchedules = 0;
        int violations = 0;
        int deadlineConflicts = 0;
        StringBuilder failures = new StringBuilder();

        for (Scenario s : scenarios) {
            AllocationResult result = allocator.allocate(context(s, workingDays));
            totalSchedules += result.scheduleCount();

            DeadlineMap deadlines = deadlineCalc.compute(
                s.orders().stream().collect(java.util.stream.Collectors.groupingBy(OrderInput::hoseId)));

            for (var row : result.schedules()) {
                if (!deadlines.isWithinDeadline(row.getHoseId(), row.getProductionDate())) {
                    violations++;
                    failures.append("DEADLINE viol ").append(s.scenarioId())
                        .append(": ").append(row.getHoseId()).append(" prod=").append(row.getProductionDate())
                        .append(" deadline=").append(deadlines.get(row.getHoseId()).orElse(null)).append('\n');
                }
            }

            deadlineConflicts += (int) result.conflicts().stream()
                .filter(c -> c.category() == AllocationConflict.Category.DEADLINE_EXCEEDED).count();
        }

        assertThat(violations).as("BR-X07 위반 0건: %s", failures).isZero();
        assertThat(totalSchedules).as("schedule ≥ 1 생성").isPositive();
        // K-V04 D-2 준수율 — deadline conflict 가 발생한 경우는 horizon 너무 짧을 때만 (정상은 ≤ 2건)
        double complianceRate = 1.0 - (double) deadlineConflicts / scenarios.size();
        assertThat(complianceRate)
            .as("K-V04 D-2 준수율 (REQ-NF-KPI-008) actual=%.2f", complianceRate)
            .isGreaterThanOrEqualTo(0.98);
    }

    @Test
    @DisplayName("같은 hose 다중 수주 → 가장 이른 납기 기준 deadline (hard 제약)")
    void single_hose_multi_orders_uses_earliest_delivery() {
        List<String> hoses = dualCapableHoses();
        String hose = hoses.get(0);

        // 같은 hose, 3개 수주 — 가장 이른 납기 = 2026-03-09 (deadline = 3/5 목)
        LocalDate early = LocalDate.of(2026, 3, 9);
        LocalDate mid = LocalDate.of(2026, 3, 16);
        LocalDate late = LocalDate.of(2026, 3, 23);

        List<OrderInput> orders = List.of(
            new OrderInput(UUID.randomUUID(), hose, mid, 5),
            new OrderInput(UUID.randomUUID(), hose, early, 3),
            new OrderInput(UUID.randomUUID(), hose, late, 4)
        );
        Scenario s = new Scenario("MULTI", orders);

        List<LocalDate> workingDays = horizonWorkingDays();
        AllocationResult result = allocator.allocate(context(s, workingDays));

        LocalDate expectedDeadline = calendar.subtractWorkingDays(early, 2);
        // 모든 schedule production_date ≤ expectedDeadline
        assertThat(result.schedules())
            .as("가장 이른 납기 %s 기준 deadline %s", early, expectedDeadline)
            .allMatch(r -> !r.getProductionDate().isAfter(expectedDeadline));
    }

    @Test
    @DisplayName("Deadline 외 production — schedule 0 + DEADLINE_EXCEEDED conflict (BR-X07)")
    void deadline_outside_horizon_yields_conflict() {
        List<String> hoses = dualCapableHoses();
        String hose = hoses.get(0);

        // horizon 시작 직전 영업일 = deadline → 모든 horizon 일자가 deadline 이후
        LocalDate delivery = calendar.addWorkingDays(HORIZON_START, 1); // deadline = HORIZON_START - 1 working day
        Scenario s = new Scenario("OUT",
            List.of(new OrderInput(UUID.randomUUID(), hose, delivery, 5)));

        List<LocalDate> workingDays = horizonWorkingDays();
        AllocationResult result = allocator.allocate(context(s, workingDays));

        // 일부 (Q_required=5, yield 큰 경우는 1회 안에 가능). 다만 deadline 이 horizon 첫째 날보다 빠르면 0.
        LocalDate deadline = deadlineCalc.deadlineFor(delivery);
        if (deadline.isBefore(workingDays.get(0))) {
            assertThat(result.scheduleCount()).isZero();
            assertThat(result.conflicts())
                .anyMatch(c -> c.category() == AllocationConflict.Category.DEADLINE_EXCEEDED);
        }
    }
}
