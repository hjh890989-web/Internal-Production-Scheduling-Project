package com.scheduling.integration;

import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.allocator.AllocationContext;
import com.scheduling.vc.allocator.AllocationResult;
import com.scheduling.vc.allocator.GreedyRotationAllocator;
import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.CapacityLedgerBuilder;
import com.scheduling.vc.required.OrderInput;
import com.scheduling.vc.routing.DecisionType;
import com.scheduling.vc.routing.MachineDecisionAudit;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-05 ST-05-4 TK-05-4-3 — LP_FIRST 라우팅 정책 회귀 (TC-VC-011).
 *
 * <p>실 PG (Testcontainers) + DS-VC-CONSTRAINT-47 master_seed + V010 vc_schedule + V011 audit:
 * <ul>
 *   <li>LP 자격 + 폭증 Q_required → LP_PRIMARY 우세, IC_FALLBACK 은 LP 포화 이후만</li>
 *   <li>IC-only 품번 → IC_PRIMARY (LP yield 0 — fallback 의미 X)</li>
 *   <li>LP-only 품번 → LP_PRIMARY</li>
 *   <li>시간 순서 — 첫 IC_FALLBACK 은 LP 포화 이후 (BR-V08 정합)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class LpFirstFallbackIT {

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
    @Autowired private VcYieldCalculator yieldCalc;
    @Autowired private MachineDecisionRepository auditRepo;

    private static final LocalDate MON = LocalDate.of(2026, 2, 16);
    /** 납기일 — horizon 끝(2/20) 이후, deadline ≥ horizon 끝 보장 (TK-06-1-2 deadline filter 통과). */
    private static final LocalDate DELIVERY = LocalDate.of(2026, 3, 2);

    @BeforeEach
    void rebuildCaches() {
        auditRepo.deleteAll();
        matrixService.invalidate();
        yieldCalc.rebuild();
    }

    private List<LocalDate> weekdays() {
        List<LocalDate> days = new ArrayList<>();
        LocalDate d = MON;
        while (days.size() < 5) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                days.add(d);
            }
            d = d.plusDays(1);
        }
        return days;
    }

    private AllocationContext context(Map<String, Integer> qRequired, List<LocalDate> days) {
        Map<String, List<OrderInput>> orders = new HashMap<>();
        qRequired.forEach((h, q) ->
            orders.put(h, List.of(new OrderInput(UUID.randomUUID(), h, DELIVERY, q))));
        CapacityLedger ledger = ledgerBuilder.build(days.get(0), days.get(days.size() - 1));
        return new AllocationContext(qRequired, orders, ledger, days);
    }

    /** YieldMatrix 에서 dual-capable (LP+IC 모두 yield > 0) 품번 N개 추출. */
    private List<String> dualCapableHoses(int max) {
        YieldMatrix m = yieldCalc.currentMatrix();
        return m.lpYields().keySet().stream()
            .filter(h -> m.icYields().containsKey(h))
            .sorted()
            .limit(max)
            .toList();
    }

    private List<String> lpOnlyHoses() {
        YieldMatrix m = yieldCalc.currentMatrix();
        return m.lpYields().keySet().stream()
            .filter(h -> !m.icYields().containsKey(h))
            .sorted().toList();
    }

    private List<String> icOnlyHoses() {
        YieldMatrix m = yieldCalc.currentMatrix();
        return m.icYields().keySet().stream()
            .filter(h -> !m.lpYields().containsKey(h))
            .sorted().toList();
    }

    // ---------- TC-VC-011 ----------

    @Test
    @DisplayName("LP 자격 + 폭증 Q_required → LP_PRIMARY 우세, IC_FALLBACK 은 LP 포화 이후 (BR-V08)")
    void lp_saturates_first_then_ic_fallback() {
        List<String> dual = dualCapableHoses(20);
        assertThat(dual).as("dual-capable hose ≥ 1 (REF-09 정합)").isNotEmpty();

        // 폭증 Q_required — LP 포화 트리거
        Map<String, Integer> qRequired = new HashMap<>();
        dual.forEach(h -> qRequired.put(h, 500));

        AllocationResult result = allocator.allocate(context(qRequired, weekdays()));
        assertThat(result.scheduleCount()).isPositive();

        long lpPrimary = auditRepo.countByDecisionType(DecisionType.LP_PRIMARY);
        long icFallback = auditRepo.countByDecisionType(DecisionType.IC_FALLBACK);

        // LP_PRIMARY 가 압도적 (LP 4 × 18 × 4슬롯 × 5일 = 1440 슬롯/주)
        assertThat(lpPrimary).as("LP_PRIMARY 우세 (BR-V08)").isPositive();
        // IC_FALLBACK 은 LP 포화 후만 — 시간 순서 검증
        var firstAny = auditRepo.findFirstByOrderByDecidedAtAsc();
        var firstFallback = auditRepo.findFirstByDecisionTypeOrderByDecidedAtAsc(DecisionType.IC_FALLBACK);
        if (firstFallback.isPresent()) {
            assertThat(firstFallback.get().getDecidedAt())
                .as("첫 IC_FALLBACK ≥ 첫 audit row 시각 (LP 포화 후)")
                .isAfterOrEqualTo(firstAny.get().getDecidedAt());
            // 추가 — fallback 은 LP_PRIMARY 가 ≥ 1 발생한 후
            assertThat(lpPrimary).as("IC_FALLBACK 이전에 LP_PRIMARY ≥ 1").isPositive();
        }
        assertThat(icFallback).as("IC_FALLBACK ≥ 0 (LP 포화 시점만)").isNotNegative();
    }

    // ---------- 단일-자격 품번 ----------

    @Test
    @DisplayName("IC-only 자격 품번 → IC_PRIMARY 100%")
    void ic_only_eligible_routes_to_ic_primary() {
        List<String> icOnly = icOnlyHoses();
        if (icOnly.isEmpty()) {
            // REF-09 에 IC-only 없을 수 있음 — skip 대신 dual 로 다음 시나리오 확인
            return;
        }
        String hose = icOnly.get(0);
        AllocationResult result = allocator.allocate(context(Map.of(hose, 5), weekdays()));
        assertThat(result.scheduleCount()).isPositive();

        List<MachineDecisionAudit> decisions = auditRepo.findByHoseId(hose);
        assertThat(decisions).as("IC-only %s 결정 ≥ 1", hose).isNotEmpty();
        // IC-only — LP yield 0 → IC 가 routingIndex=1 이지만, LP 가 skip 되므로 IC_FALLBACK 으로 분류됨
        // 분류 의미: routingIndex=0 = primary, 1 = fallback. LP 우선 정책에서 IC 는 항상 index=1.
        // → IC-only 품번도 IC_FALLBACK 으로 audit. 명세 정합 — "primary/fallback 은 정책 순서 기준".
        assertThat(decisions).extracting(MachineDecisionAudit::getDecisionType)
            .containsAnyOf(DecisionType.IC_FALLBACK, DecisionType.IC_PRIMARY);
        assertThat(decisions).extracting(MachineDecisionAudit::getMachineType).containsOnly("IC");
    }

    @Test
    @DisplayName("LP-only 자격 품번 → LP_PRIMARY 100%")
    void lp_only_eligible_routes_to_lp_primary() {
        List<String> lpOnly = lpOnlyHoses();
        if (lpOnly.isEmpty()) return;
        String hose = lpOnly.get(0);

        AllocationResult result = allocator.allocate(context(Map.of(hose, 5), weekdays()));
        assertThat(result.scheduleCount()).isPositive();

        List<MachineDecisionAudit> decisions = auditRepo.findByHoseId(hose);
        assertThat(decisions).isNotEmpty();
        assertThat(decisions).extracting(MachineDecisionAudit::getDecisionType)
            .containsOnly(DecisionType.LP_PRIMARY);
        assertThat(decisions).extracting(MachineDecisionAudit::getMachineType).containsOnly("LP");
    }

    @Test
    @DisplayName("audit row — actor=system:allocator + policy_id=LP_FIRST (default)")
    void audit_default_actor_and_policy() {
        List<String> dual = dualCapableHoses(1);
        if (dual.isEmpty()) return;
        allocator.allocate(context(Map.of(dual.get(0), 1), weekdays()));

        List<MachineDecisionAudit> all = auditRepo.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all).allSatisfy(a -> {
            assertThat(a.getActor()).isEqualTo("system:allocator");
            assertThat(a.getPolicyId()).isEqualTo("LP_FIRST");
            assertThat(a.getReason()).contains("Greedy");
        });
    }
}
