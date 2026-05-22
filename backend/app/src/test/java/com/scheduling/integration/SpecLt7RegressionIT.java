package com.scheduling.integration;

import com.scheduling.master.api.ProductSpecLookup;
import com.scheduling.master.api.ProductSpecSummary;
import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.allocator.AllocationContext;
import com.scheduling.vc.allocator.AllocationResult;
import com.scheduling.vc.allocator.GreedyRotationAllocator;
import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.CapacityLedgerBuilder;
import com.scheduling.vc.required.OrderInput;
import com.scheduling.vc.routing.MachineDecisionRepository;
import com.scheduling.vc.rule.SpecLt7CapRule;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-21 ST-21-5 TK-21-5-4 — TC-VC-027 spec<7 가류기당 ≤4 회귀.
 *
 * <p>실 PG + DS-VC-CONSTRAINT-47 + V016 ex_constraint seed (28442-6T010·28415-08400·25490-03HA0
 * 가 spec<7) + ProductSpec VIEW + SpecLt7CapRule 통합 검증.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class SpecLt7RegressionIT {

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
    @Autowired private ProductSpecLookup productSpecLookup;
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

    // ---------- TC-VC-027 ----------

    @Test
    @DisplayName("ProductSpec VIEW — V016 seed (28415-08400 spec=5, 25490-03HA0 spec=6) 노출")
    void product_spec_view_seeded() {
        Optional<ProductSpecSummary> s1 = productSpecLookup.findById("28415-08400");
        Optional<ProductSpecSummary> s2 = productSpecLookup.findById("25490-03HA0");

        assertThat(s1).isPresent();
        assertThat(s1.get().isSpecLt7()).isTrue();
        assertThat(s1.get().spec()).isEqualTo(5);

        assertThat(s2).isPresent();
        assertThat(s2.get().isSpecLt7()).isTrue();
        assertThat(s2.get().spec()).isEqualTo(6);
    }

    @Test
    @DisplayName("spec=NULL hose (EX 미등록) — 룰 미적용, 자유 배치")
    void spec_null_hose_unaffected() {
        // 29673-2F910 — ex_constraint 미등록 (spec=null → isSpecLt7=false)
        AllocationResult r = allocator.allocate(context(Map.of("29673-2F910", 5), workingDays()));
        assertThat(r.scheduleCount()).isPositive();

        // 룰 미적용 — angle 4 초과 무관 (가용 슬롯 채워짐)
        Optional<ProductSpecSummary> s = productSpecLookup.findById("29673-2F910");
        assertThat(s).isPresent();
        assertThat(s.get().isSpecLt7()).isFalse();
    }

    @Test
    @DisplayName("BR-V17 — spec<7 (25490-03HA0 angle=2) 가류기당 누계 ≤ 4")
    void spec_lt7_cap_enforced() {
        // 25490-03HA0 큰 Q → 같은 (machine, date) 에 angle 누계 검증
        AllocationResult r = allocator.allocate(context(Map.of("25490-03HA0", 100), workingDays()));

        // ProductSpec lookup — spec<7 식별
        Set<String> specLt7Hoses = new HashSet<>();
        for (ProductSpecSummary s : productSpecLookup.findAllSpecLt7()) {
            specLt7Hoses.add(s.hoseId());
        }

        // (machine, date) 별 spec<7 angle 누계
        Map<String, Integer> angles = r.schedules().stream()
            .filter(row -> specLt7Hoses.contains(row.getHoseId()))
            .collect(Collectors.toMap(
                row -> row.getMachineId() + "/" + row.getProductionDate(),
                row -> productSpecLookup.findById(row.getHoseId())
                    .map(ProductSpecSummary::angleCount).orElse(0),
                Integer::sum));

        assertThat(angles.values())
            .as("BR-V17 spec<7 가류기당 ≤ 4 angle")
            .allMatch(a -> a <= SpecLt7CapRule.SPEC_LT7_MAX_ANGLES);
    }

    @Test
    @DisplayName("spec≥7 hose (28421-2M800 spec=9) — 룰 영향 0")
    void spec_gte_7_unaffected() {
        AllocationResult r = allocator.allocate(context(Map.of("28421-2M800", 30), workingDays()));
        assertThat(r.scheduleCount()).isPositive();

        // 28421-2M800 spec=9 → isSpecLt7=false
        ProductSpecSummary s = productSpecLookup.findById("28421-2M800").orElseThrow();
        assertThat(s.isSpecLt7()).isFalse();
    }
}
