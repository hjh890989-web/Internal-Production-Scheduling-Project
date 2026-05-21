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
import com.scheduling.vc.rule.LeftRightRule;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-21 ST-21-1 TK-21-1-3 — TC-VC-021 좌/우 회귀 100건.
 *
 * <p>실 PG + DS-VC-CONSTRAINT-47 master_seed (V013 K/L UPDATE 포함) + GreedyRotationAllocator
 * LeftRightRule 통합 검증:
 * <ul>
 *   <li>{@code 28421-2M800} 좌측 only → 모든 배치 LP-01/02 (LEFT)</li>
 *   <li>{@code 28422-2M800} 우측 only → 모든 배치 LP-03/04 (RIGHT)</li>
 *   <li>{@code 28422-08HA0} 양쪽 가능 → 4 LP 모두 사용 가능</li>
 *   <li>회귀 100건 — LP 좌/우 위반 0건</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class LeftRightRegressionIT {

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
    @Autowired private LeftRightRule leftRightRule;
    @Autowired private WorkingCalendar calendar;
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private VcYieldCalculator yieldCalc;
    @Autowired private MachineDecisionRepository auditRepo;

    private static final LocalDate HORIZON_START = LocalDate.of(2026, 2, 23);
    private static final LocalDate DELIVERY = LocalDate.of(2026, 3, 16);   // horizon 끝 한참 후

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

    // ---------- TC-VC-021 ----------

    @Test
    @DisplayName("28421-2M800 좌측 only — 모든 LP 배치는 LP-01/02 (BR-V15)")
    void rule_28421_2M800_only_left() {
        AllocationResult r = allocator.allocate(context(Map.of("28421-2M800", 50), workingDays()));
        assertThat(r.scheduleCount()).isPositive();

        // LP 배치만 검사 (IC 는 좌/우 rule 적용 X)
        var lpRows = r.schedules().stream()
            .filter(s -> s.getMachineId().startsWith("LP-"))
            .toList();
        assertThat(lpRows).as("LP 배치 ≥ 1").isNotEmpty();

        assertThat(lpRows)
            .as("28421-2M800 모든 LP 배치는 LEFT (LP-01/02)")
            .allSatisfy(s -> {
                Optional<SlotSide> side = SlotSide.ofLp(s.getMachineId());
                assertThat(side).isPresent();
                assertThat(side.get()).isEqualTo(SlotSide.LEFT);
            });
    }

    @Test
    @DisplayName("28422-2M800 우측 only — 모든 LP 배치는 LP-03/04 (BR-V16)")
    void rule_28422_2M800_only_right() {
        AllocationResult r = allocator.allocate(context(Map.of("28422-2M800", 50), workingDays()));
        assertThat(r.scheduleCount()).isPositive();

        var lpRows = r.schedules().stream()
            .filter(s -> s.getMachineId().startsWith("LP-"))
            .toList();
        assertThat(lpRows).as("LP 배치 ≥ 1").isNotEmpty();

        assertThat(lpRows)
            .as("28422-2M800 모든 LP 배치는 RIGHT (LP-03/04)")
            .allSatisfy(s -> {
                Optional<SlotSide> side = SlotSide.ofLp(s.getMachineId());
                assertThat(side).isPresent();
                assertThat(side.get()).isEqualTo(SlotSide.RIGHT);
            });
    }

    @Test
    @DisplayName("28422-08HA0 양쪽 가능 — LP 모든 머신 사용 가능 (제약 없음)")
    void rule_28422_08HA0_both_sides() {
        // 28422-08HA0 는 LP_BOT (slot 4) 만 가용 — 4 LP × yield 6 으로 큰 Q_required 폭증
        AllocationResult r = allocator.allocate(context(Map.of("28422-08HA0", 200), workingDays()));
        assertThat(r.scheduleCount()).isPositive();

        var lpRows = r.schedules().stream()
            .filter(s -> s.getMachineId().startsWith("LP-"))
            .toList();
        // 좌/우 모두 가능 — LP-01~04 골고루 사용 가능. allowsLeft + allowsRight = true 이므로 모든 LP 통과.
        assertThat(lpRows).as("LP 배치 ≥ 1").isNotEmpty();
    }

    @Test
    @DisplayName("회귀 100건 — LP 좌/우 위반 0건 (TC-VC-021)")
    void regression_100_orders_no_left_right_violation() {
        // 28421-2M800 좌 + 28422-2M800 우 + 28422-08HA0 양쪽 mix — 각 호스 다중 수량
        Map<String, Integer> qRequired = Map.of(
            "28421-2M800", 30,
            "28422-2M800", 30,
            "28422-08HA0", 40
        );
        AllocationResult r = allocator.allocate(context(qRequired, workingDays()));

        var lpRows = r.schedules().stream()
            .filter(s -> s.getMachineId().startsWith("LP-"))
            .toList();

        var violations = lpRows.stream()
            .filter(s -> !leftRightRule.validate(s.getHoseId(), s.getMachineId()))
            .toList();

        assertThat(violations).as("BR-V15·V16 위반 0건").isEmpty();
    }

    @Test
    @DisplayName("LP 배치 카운트 — 28421-2M800 = LP-01/02 만, 28422-2M800 = LP-03/04 만 (분리 검증)")
    void machine_distribution_separated_by_side() {
        Map<String, Integer> qRequired = Map.of(
            "28421-2M800", 50,
            "28422-2M800", 50
        );
        AllocationResult r = allocator.allocate(context(qRequired, workingDays()));

        // 28421 의 LP 배치 → 모두 LP-01/02
        var rows28421 = r.schedules().stream()
            .filter(s -> "28421-2M800".equals(s.getHoseId()))
            .filter(s -> s.getMachineId().startsWith("LP-"))
            .toList();
        assertThat(rows28421).allMatch(s ->
            s.getMachineId().equals("LP-01") || s.getMachineId().equals("LP-02"));

        // 28422 의 LP 배치 → 모두 LP-03/04
        var rows28422 = r.schedules().stream()
            .filter(s -> "28422-2M800".equals(s.getHoseId()))
            .filter(s -> s.getMachineId().startsWith("LP-"))
            .toList();
        assertThat(rows28422).allMatch(s ->
            s.getMachineId().equals("LP-03") || s.getMachineId().equals("LP-04"));
    }
}
