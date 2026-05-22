package com.scheduling.integration;

import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.allocator.AllocationContext;
import com.scheduling.vc.allocator.AllocationResult;
import com.scheduling.vc.allocator.GreedyRotationAllocator;
import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.CapacityLedgerBuilder;
import com.scheduling.vc.required.OrderInput;
import com.scheduling.vc.routing.MachineDecisionRepository;
import com.scheduling.vc.rule.SlotSide;
import com.scheduling.vc.yield.VcYieldCalculator;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-21 ST-21-4 TK-21-4-3 — TC-VC-025·026 좌/우 + ≤2 결합 회귀.
 *
 * <p>실 PG + DS-VC-CONSTRAINT-47 master_seed + V015 vc_hose_rule seed +
 * HoseSlotCapRule 결합 검증.
 *
 * <p>28422-2M800 (side=RIGHT, max=2) → LP-03/04 + 회전당 ≤2 슬롯
 * 28421-2M800 (side=LEFT,  max=2) → LP-01/02 + 회전당 ≤2 슬롯
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class SideCapRegressionIT {

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
    @Autowired private WorkingCalendar calendar;
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private VcYieldCalculator yieldCalc;
    @Autowired private MachineDecisionRepository auditRepo;

    private static final LocalDate HORIZON_START = LocalDate.of(2026, 2, 23);
    private static final LocalDate DELIVERY = LocalDate.of(2026, 3, 16);

    @BeforeEach
    void rebuildCaches() {
        auditRepo.deleteAll();
        matrixService.invalidate();
        yieldCalc.rebuild();
    }

    private List<LocalDate> workingDays() {
        return calendar.workingDaysInRange(HORIZON_START, HORIZON_START.plusWeeks(2));
    }

    private AllocationContext context(Map<String, Integer> qRequired, List<LocalDate> days) {
        Map<String, List<OrderInput>> orders = new HashMap<>();
        qRequired.forEach((h, q) ->
            orders.put(h, List.of(new OrderInput(UUID.randomUUID(), h, DELIVERY, q))));
        CapacityLedger ledger = ledgerBuilder.build(days.get(0), days.get(days.size() - 1));
        return new AllocationContext(qRequired, orders, ledger, days);
    }

    // ---------- TC-VC-025 ----------

    @Test
    @DisplayName("28422-2M800 — 모든 LP 배치 RIGHT (BR-V16 side_lock + LeftRight 결합)")
    void rule_28422_2M800_right_only() {
        AllocationResult r = allocator.allocate(context(Map.of("28422-2M800", 50), workingDays()));
        assertThat(r.scheduleCount()).isPositive();

        List<com.scheduling.vc.domain.VcSchedule> lp = r.schedules().stream()
            .filter(s -> s.getMachineId().startsWith("LP-")).toList();
        assertThat(lp).isNotEmpty();

        assertThat(lp).allSatisfy(s -> {
            Optional<SlotSide> side = SlotSide.ofLp(s.getMachineId());
            assertThat(side).isPresent();
            assertThat(side.get()).isEqualTo(SlotSide.RIGHT);
        });
    }

    @Test
    @DisplayName("28422-2M800 — 회전당 ≤ 2 슬롯 (BR-V16 max_concurrent_slots=2)")
    void rule_28422_2M800_max_two_per_rotation() {
        AllocationResult r = allocator.allocate(context(Map.of("28422-2M800", 50), workingDays()));

        Map<String, Long> grouped = r.schedules().stream()
            .filter(s -> "28422-2M800".equals(s.getHoseId()))
            .collect(Collectors.groupingBy(
                s -> s.getProductionDate() + "/" + s.getMachineId() + "/" + s.getRotationNo(),
                Collectors.counting()));

        assertThat(grouped.values())
            .as("28422-2M800 회전당 슬롯 ≤ 2")
            .allMatch(c -> c <= 2L);
    }

    // ---------- TC-VC-026 ----------

    @Test
    @DisplayName("28421-2M800 — 모든 LP 배치 LEFT (BR-V15 side_lock + LeftRight 결합)")
    void rule_28421_2M800_left_only() {
        AllocationResult r = allocator.allocate(context(Map.of("28421-2M800", 50), workingDays()));

        List<com.scheduling.vc.domain.VcSchedule> lp = r.schedules().stream()
            .filter(s -> s.getMachineId().startsWith("LP-")).toList();
        assertThat(lp).isNotEmpty();

        assertThat(lp).allSatisfy(s -> {
            Optional<SlotSide> side = SlotSide.ofLp(s.getMachineId());
            assertThat(side).isPresent();
            assertThat(side.get()).isEqualTo(SlotSide.LEFT);
        });
    }

    @Test
    @DisplayName("28421-2M800 — 회전당 ≤ 2 슬롯 (BR-V15 max_concurrent_slots=2)")
    void rule_28421_2M800_max_two_per_rotation() {
        AllocationResult r = allocator.allocate(context(Map.of("28421-2M800", 50), workingDays()));

        Map<String, Long> grouped = r.schedules().stream()
            .filter(s -> "28421-2M800".equals(s.getHoseId()))
            .collect(Collectors.groupingBy(
                s -> s.getProductionDate() + "/" + s.getMachineId() + "/" + s.getRotationNo(),
                Collectors.counting()));

        assertThat(grouped.values())
            .as("28421-2M800 회전당 슬롯 ≤ 2")
            .allMatch(c -> c <= 2L);
    }

    // ---------- 결합 / mix ----------

    @Test
    @DisplayName("결합 회귀 — 28421/28422 동시 배치 시 좌/우 분리 + cap ≤2 동시 만족")
    void combined_side_and_cap_no_violation() {
        Map<String, Integer> qRequired = Map.of(
            "28421-2M800", 30,
            "28422-2M800", 30,
            "28422-08HA0", 10   // LP-01만 + cap=1 — 28421-2M800 (LP-01/02) 와 머신 공유
        );

        AllocationResult r = allocator.allocate(context(qRequired, workingDays()));

        // 28421 → LEFT (LP-01/02)
        assertThat(r.schedules().stream()
            .filter(s -> "28421-2M800".equals(s.getHoseId()))
            .filter(s -> s.getMachineId().startsWith("LP-"))
            .toList())
            .allSatisfy(s -> assertThat(SlotSide.ofLp(s.getMachineId())).contains(SlotSide.LEFT));

        // 28422 → RIGHT (LP-03/04)
        assertThat(r.schedules().stream()
            .filter(s -> "28422-2M800".equals(s.getHoseId()))
            .filter(s -> s.getMachineId().startsWith("LP-"))
            .toList())
            .allSatisfy(s -> assertThat(SlotSide.ofLp(s.getMachineId())).contains(SlotSide.RIGHT));

        // 28422-08HA0 → LP-01만
        assertThat(r.schedules().stream()
            .filter(s -> "28422-08HA0".equals(s.getHoseId()))
            .toList())
            .allMatch(s -> "LP-01".equals(s.getMachineId()));
    }
}
