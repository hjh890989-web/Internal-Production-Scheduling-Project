package com.scheduling.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.api.SlotCompatibilityQuery;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.allocator.AllocationConflict;
import com.scheduling.vc.allocator.AllocationContext;
import com.scheduling.vc.allocator.AllocationResult;
import com.scheduling.vc.allocator.GreedyRotationAllocator;
import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.CapacityLedgerBuilder;
import com.scheduling.vc.domain.RotationSlot;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.required.OrderInput;
import com.scheduling.vc.yield.AngleCapacityValidator;
import com.scheduling.vc.yield.AngleCapacityViolation;
import com.scheduling.vc.yield.VcYieldCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-05 ST-05-3 TK-05-3-3 — DS-VC-ALLOC-100 회귀 + 1주 47품번 SLA (TC-VC-010 + TC-PER-002).
 *
 * <p>실 PG (Testcontainers) + DS-VC-CONSTRAINT-47 master_seed:
 * <ul>
 *   <li>100 시나리오 — schedule 생성 + Unschedulable 사전 분리 + 위반 0건 (TC-VC-010)</li>
 *   <li>1주 호라이즌 + 43품번 Q_required = 1 — 5분 SLA (TC-PER-002, 1회 측정)</li>
 *   <li>결정성 — 같은 시나리오 2회 → 같은 슬롯 배치</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class AllocationRegressionIT {

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
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private SlotCompatibilityQuery compatQuery;
    @Autowired private VcYieldCalculator yieldCalc;
    @Autowired private AngleCapacityValidator angleValidator;
    @Autowired private ObjectMapper objectMapper;

    private static final LocalDate MON = LocalDate.of(2026, 2, 16);
    /** 납기일 — horizon 끝(2/20) 이후, deadline ≥ horizon 끝 보장 (TK-06-1-2 deadline filter 통과). */
    private static final LocalDate DELIVERY = LocalDate.of(2026, 3, 2);

    @BeforeEach
    void rebuildCaches() {
        matrixService.invalidate();
        yieldCalc.rebuild();
    }

    private List<Scenario> loadScenarios() throws Exception {
        ClassPathResource res = new ClassPathResource("datasets/DS-VC-ALLOC-100/scenarios.json");
        try (InputStream is = res.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<Scenario>>() {});
        }
    }

    private AllocationContext contextFor(Scenario s, List<LocalDate> workingDays) {
        Map<String, Integer> qRequired = new HashMap<>();
        Map<String, List<OrderInput>> ordersByHose = new HashMap<>();
        for (HoseEntry h : s.hoses()) {
            qRequired.put(h.hoseId(), h.qRequired());
            ordersByHose.put(h.hoseId(),
                List.of(new OrderInput(UUID.randomUUID(), h.hoseId(), DELIVERY, h.qRequired())));
        }
        CapacityLedger ledger = ledgerBuilder.build(workingDays.get(0), workingDays.get(workingDays.size() - 1));
        return new AllocationContext(qRequired, ordersByHose, ledger, workingDays);
    }

    // ---------- TC-VC-010 ----------

    @Test
    @DisplayName("100 시나리오 — 위반 0건 (TC-VC-010)")
    void allocates_100_scenarios_with_zero_violations() throws Exception {
        List<Scenario> scenarios = loadScenarios();
        assertThat(scenarios).hasSize(100);

        // 1주 호라이즌 (월~금)
        List<LocalDate> workingDays = workingDaysForWeek(MON);

        int totalSchedules = 0;
        int slotOxViolations = 0;
        int angleViolations = 0;
        int duplicateSlotKeys = 0;
        int unschedulableConflicts = 0;
        StringBuilder failures = new StringBuilder();

        for (Scenario s : scenarios) {
            AllocationResult result = allocator.allocate(contextFor(s, workingDays));
            totalSchedules += result.scheduleCount();

            // a) Slot O/X 위반 — 모든 스케줄에 대해 matrix 재확인
            for (VcSchedule vc : result.schedules()) {
                String slotName = slotPositionName(vc.getMachineId(), vc.getSlotPosition());
                if (!compatQuery.isEligible(vc.getHoseId(), slotName)) {
                    slotOxViolations++;
                    failures.append("SLOT_OX violation in ").append(s.scenarioId())
                        .append(": ").append(vc.getHoseId()).append("→").append(slotName).append('\n');
                }
            }

            // b) 앵글 capa 위반
            Map<String, List<RotationSlot>> assignments = result.schedules().stream()
                .collect(Collectors.groupingBy(VcSchedule::getHoseId,
                    Collectors.mapping(VcSchedule::asSlot, Collectors.toList())));
            List<AngleCapacityViolation> aviols = angleValidator.validate(assignments);
            angleViolations += aviols.size();
            for (AngleCapacityViolation v : aviols) {
                failures.append("ANGLE violation in ").append(s.scenarioId())
                    .append(": ").append(v.userMessage()).append('\n');
            }

            // c) 동일 (machine, date, rotation, slotPosition) 중복 점유 X (UNIQUE 정합)
            List<String> keys = result.schedules().stream()
                .map(vc -> vc.getMachineId() + "/" + vc.getProductionDate() + "/"
                       + vc.getRotationNo() + "/" + vc.getSlotPosition())
                .toList();
            long distinctKeys = keys.stream().distinct().count();
            if (distinctKeys != keys.size()) {
                duplicateSlotKeys++;
                failures.append("DUPLICATE slot key in ").append(s.scenarioId()).append('\n');
            }

            // d) Unschedulable 사전 conflict — 시나리오에 포함된 unschedulable hose 는 conflict 로 보고됨
            long expectedUnschedCount = s.hoses().stream().filter(HoseEntry::expectedUnschedulable).count();
            long actualUnschedConflicts = result.conflicts().stream()
                .filter(c -> c.category() == AllocationConflict.Category.UNSCHEDULABLE)
                .count();
            if (actualUnschedConflicts != expectedUnschedCount) {
                failures.append("UNSCHED mismatch in ").append(s.scenarioId())
                    .append(": expected=").append(expectedUnschedCount)
                    .append(" actual=").append(actualUnschedConflicts).append('\n');
            }
            unschedulableConflicts += actualUnschedConflicts;
        }

        assertThat(slotOxViolations).as("Slot O/X 위반 0건 (TC-VC-010): %s", failures).isZero();
        assertThat(angleViolations).as("앵글 capa 위반 0건: %s", failures).isZero();
        assertThat(duplicateSlotKeys).as("중복 slot key 0건").isZero();
        assertThat(totalSchedules).as("schedule ≥ 1 생성").isPositive();
        assertThat(unschedulableConflicts).as("Unschedulable conflict ≥ 1 (시나리오 일부 의도)").isPositive();
    }

    // ---------- 결정성 ----------

    @Test
    @DisplayName("결정성 — 같은 시나리오 2회 → 같은 슬롯 배치")
    void deterministic_same_input_same_output() throws Exception {
        Scenario s = loadScenarios().get(0);
        List<LocalDate> workingDays = workingDaysForWeek(MON);

        AllocationResult r1 = allocator.allocate(contextFor(s, workingDays));
        AllocationResult r2 = allocator.allocate(contextFor(s, workingDays));

        List<String> k1 = r1.schedules().stream()
            .map(v -> v.getHoseId() + "@" + v.getMachineId() + "/" + v.getRotationNo() + "/" + v.getSlotPosition())
            .sorted().toList();
        List<String> k2 = r2.schedules().stream()
            .map(v -> v.getHoseId() + "@" + v.getMachineId() + "/" + v.getRotationNo() + "/" + v.getSlotPosition())
            .sorted().toList();
        assertThat(k1).isEqualTo(k2);
    }

    // ---------- TC-PER-002 ----------

    @Test
    @DisplayName("1주 호라이즌 + 43품번 Q_required=1 → SLA ≤ 5분 (TC-PER-002)")
    void week_horizon_full_product_under_5min_sla() {
        List<LocalDate> workingDays = workingDaysForWeek(MON);

        // 43 schedulable 품번 각 Q_required=1 (인메모리 합성 — DB 호출 최소화)
        // schedulable hose_id 는 시나리오 로딩에서 추출
        Map<String, Integer> qRequired = new HashMap<>();
        for (int i = 0; i < 43; i++) {
            qRequired.put(String.format("PERF-%03d", i), 1);   // matrix 미적재 → Unschedulable 처리되거나 yield 없음
        }
        // 실제 schedulable 품번 사용 — yield 매트릭스 에서 lpYields 키 추출
        var matrix = yieldCalc.currentMatrix();
        qRequired.clear();
        matrix.lpYields().keySet().forEach(h -> qRequired.put(h, 1));

        Map<String, List<OrderInput>> ordersByHose = new HashMap<>();
        qRequired.keySet().forEach(h ->
            ordersByHose.put(h, List.of(new OrderInput(UUID.randomUUID(), h, DELIVERY, 1))));

        CapacityLedger ledger = ledgerBuilder.build(workingDays.get(0), workingDays.get(workingDays.size() - 1));
        AllocationContext ctx = new AllocationContext(qRequired, ordersByHose, ledger, workingDays);

        long start = System.nanoTime();
        AllocationResult r = allocator.allocate(ctx);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(r.scheduleCount()).isPositive();
        assertThat(elapsedMs)
            .as("1주 호라이즌 + %d품번 SLA ≤ 5분 (실측 %dms)", qRequired.size(), elapsedMs)
            .isLessThan(5 * 60 * 1000L);
    }

    private List<LocalDate> workingDaysForWeek(LocalDate from) {
        List<LocalDate> days = new ArrayList<>();
        LocalDate d = from;
        while (days.size() < 5) {
            if (d.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                && d.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                days.add(d);
            }
            d = d.plusDays(1);
        }
        return days;
    }

    private String slotPositionName(String machineId, short slotPos) {
        if (machineId.startsWith("LP-")) {
            return switch (slotPos) {
                case 1 -> "LP_TOP"; case 2 -> "LP_UPMID";
                case 3 -> "LP_LOWMID"; case 4 -> "LP_BOT";
                default -> "LP_TOP";
            };
        }
        return switch (slotPos) {
            case 1 -> "IC_TOP"; case 2 -> "IC_MID"; case 3 -> "IC_BOT";
            default -> "IC_TOP";
        };
    }

    public record Scenario(String scenarioId, String description, List<HoseEntry> hoses) {
        public Scenario(
            @com.fasterxml.jackson.annotation.JsonProperty("scenario_id") String scenarioId,
            @com.fasterxml.jackson.annotation.JsonProperty("description") String description,
            @com.fasterxml.jackson.annotation.JsonProperty("hoses") List<HoseEntry> hoses
        ) {
            this.scenarioId = scenarioId;
            this.description = description;
            this.hoses = hoses != null ? hoses : List.of();
        }
    }

    public record HoseEntry(String hoseId, int qRequired, boolean expectedUnschedulable) {
        public HoseEntry(
            @com.fasterxml.jackson.annotation.JsonProperty("hose_id") String hoseId,
            @com.fasterxml.jackson.annotation.JsonProperty("q_required") int qRequired,
            @com.fasterxml.jackson.annotation.JsonProperty("expected_unschedulable") boolean expectedUnschedulable
        ) {
            this.hoseId = hoseId;
            this.qRequired = qRequired;
            this.expectedUnschedulable = expectedUnschedulable;
        }
    }
}
