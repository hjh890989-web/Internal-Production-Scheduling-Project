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
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-21 ST-21-3 TK-21-3-3 — TC-VC-024 machine_pin 회귀.
 *
 * <p>실 PG + DS-VC-CONSTRAINT-47 master_seed + V015 vc_hose_rule seed +
 * MachinePinRule + HoseSlotCapRule 통합 검증.
 *
 * <p>28422-08HA0 (machine_pin=LP-01, max=1, lp_only=TRUE):
 * <ul>
 *   <li>모든 배치 LP-01 (LP-02·03·04·IC 0건)</li>
 *   <li>같은 회전·머신 동시 슬롯 ≤ 1</li>
 *   <li>LP-01 포화 + 추가 Q_required → INSUFFICIENT_CAPACITY conflict</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class MachinePinRegressionIT {

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

    // ---------- TC-VC-024 ----------

    @Test
    @DisplayName("28422-08HA0 — 모든 배치 LP-01 (BR-V14 machine_pin)")
    void rule_28422_08HA0_only_LP01() {
        // 28422-08HA0: composite=1, lp_molds=6, yield=6/회전. Q=30 → 5 슬롯 필요
        AllocationResult r = allocator.allocate(context(Map.of("28422-08HA0", 30), workingDays()));
        assertThat(r.scheduleCount()).isPositive();

        assertThat(r.schedules())
            .as("28422-08HA0 모든 배치는 LP-01")
            .allMatch(s -> "LP-01".equals(s.getMachineId()));
    }

    @Test
    @DisplayName("28422-08HA0 — IC 0건 (BR-V14 lp_only)")
    void rule_28422_08HA0_no_ic() {
        AllocationResult r = allocator.allocate(context(Map.of("28422-08HA0", 30), workingDays()));

        assertThat(r.schedules())
            .noneMatch(s -> s.getMachineId().startsWith("IC-"));
    }

    @Test
    @DisplayName("28422-08HA0 — 회전당 동시 슬롯 ≤ 1 (BR-V14 max_concurrent_slots=1)")
    void rule_28422_08HA0_max_one_slot_per_rotation() {
        AllocationResult r = allocator.allocate(context(Map.of("28422-08HA0", 30), workingDays()));

        Map<String, Long> grouped = r.schedules().stream()
            .filter(s -> "28422-08HA0".equals(s.getHoseId()))
            .collect(Collectors.groupingBy(
                s -> s.getProductionDate() + "/" + s.getMachineId() + "/" + s.getRotationNo(),
                Collectors.counting()));

        assertThat(grouped.values())
            .as("28422-08HA0 회전당 동시 슬롯 ≤ 1")
            .allMatch(count -> count <= 1L);
    }

    @Test
    @DisplayName("28422-08HA0 — LP-01 회전 (18) × 2주 영업일 ≈ 180 + cap=1 → 큰 Q 미달 시 conflict")
    void conflict_when_LP01_horizon_saturated() {
        // LP-01 × 18 rotation × 2주 영업일 (10일 이내) × 1 slot/rotation × yield 6 = ~1080
        // 매우 큰 Q (10000) → 분명히 미달
        AllocationResult r = allocator.allocate(context(Map.of("28422-08HA0", 10000), workingDays()));

        // 일부 배치 + capa 부족 conflict 발생
        assertThat(r.conflicts())
            .anyMatch(c -> c.hoseId().equals("28422-08HA0"));
    }
}
