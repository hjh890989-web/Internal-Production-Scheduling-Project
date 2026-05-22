package com.scheduling.integration;

import com.scheduling.ex.required.ExtrusionDemandCalculator;
import com.scheduling.ex.yield.YieldFormula;
import com.scheduling.master.api.ExConstraintLookup;
import com.scheduling.master.api.ExConstraintSummary;
import com.scheduling.master.api.ProductInventoryLookup;
import com.scheduling.master.api.ProductInventorySummary;
import com.scheduling.master.api.ShiftLookup;
import com.scheduling.master.api.ShiftSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-08 ST-08-1·2·3 통합 IT — 실 PG (Testcontainers) + V018/19/20 seed.
 *
 * <p>핵심 시연 (Sprint 3 DoD):
 * <ul>
 *   <li>BR-E05 reference — 29673-2R060 주간전반 (speed=14.06, length=1000, effective_min=180) = 2,531</li>
 *   <li>Shift 마스터 4 row (DAY_EARLY·DAY_LATE·NIGHT_EARLY·NIGHT_LATE), 모두 effective_min=180</li>
 *   <li>ExConstraint 47품번 6 핵심 — speed/length 정확</li>
 *   <li>ProductInventory 4 시나리오 (부족·target 도달·충분·0)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class YieldAndDemandIT {

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

    @Autowired private YieldFormula formula;
    @Autowired private ExtrusionDemandCalculator demandCalc;
    @Autowired private ShiftLookup shiftLookup;
    @Autowired private ExConstraintLookup exLookup;
    @Autowired private ProductInventoryLookup invLookup;

    // ---------- Shift 마스터 ----------

    @Test
    @DisplayName("V018 Shift seed — 4 row (DAY_EARLY/DAY_LATE/NIGHT_EARLY/NIGHT_LATE) effective_min=180")
    void shift_master_seeded_with_75pct_efficiency() {
        List<ShiftSummary> shifts = shiftLookup.findAll();
        assertThat(shifts).hasSize(4);
        assertThat(shifts).extracting(ShiftSummary::shiftCode)
            .containsExactly("DAY_EARLY", "DAY_LATE", "NIGHT_EARLY", "NIGHT_LATE");
        assertThat(shifts).allMatch(s -> s.effectiveMin() == 180);
        assertThat(shifts).allMatch(s -> s.nominalMin() == 240);
    }

    // ---------- BR-E05 reference (TC-EX-005) ----------

    @Test
    @DisplayName("BR-E05 reference — 29673-2R060 주간전반 (실 PG ex_constraint + Shift) = 2,531")
    void br_e05_reference_29673_2R060_yields_2531() {
        Optional<ExConstraintSummary> ex = exLookup.findById("29673-2R060");
        Optional<ShiftSummary> shift = shiftLookup.findByCode("DAY_EARLY");

        assertThat(ex).isPresent();
        assertThat(shift).isPresent();
        assertThat(ex.get().hasYieldInput()).isTrue();

        long yield = formula.compute(
            ex.get().speedMPerMin(), shift.get().effectiveMin(), ex.get().lengthMm());

        assertThat(yield)
            .as("BR-E05 — Sprint 3 단일 최중요 검증 (14.06 × 180 = 2530.8 → round 2531)")
            .isEqualTo(2531L);
    }

    // ---------- V019 ex_constraint 풀 확장 seed ----------

    @Test
    @DisplayName("V019 ex_constraint seed — 6 핵심 품번 speed/length/die/line 검증")
    void ex_constraint_seeded_with_full_columns() {
        assertThat(exLookup.findById("29673-2R060")).isPresent().get()
            .satisfies(e -> {
                assertThat(e.speedMPerMin()).isEqualByComparingTo("14.060");
                assertThat(e.lengthMm()).isEqualTo(1000);
                assertThat(e.dieCode()).isEqualTo("DIE-2R060");
                assertThat(e.lineCode()).isEqualTo("L1");
            });
        assertThat(exLookup.findById("28442-6T010")).isPresent().get()
            .satisfies(e -> {
                assertThat(e.speedMPerMin()).isEqualByComparingTo("18.000");
                assertThat(e.lengthMm()).isEqualTo(600);
            });
    }

    // ---------- V020 product_inventory seed (TC-EX-010) ----------

    @Test
    @DisplayName("V020 ProductInventory 4 시나리오 — 부족/target/충분/0")
    void inventory_seeded_4_scenarios() {
        // 부족: 29673-2R060 target=500, current=100 → shortage=+400
        ProductInventorySummary s1 = invLookup.findById("29673-2R060").orElseThrow();
        assertThat(s1.shortage()).isEqualTo(400);
        // target 도달: 29673-2F900 target=300, current=300
        ProductInventorySummary s2 = invLookup.findById("29673-2F900").orElseThrow();
        assertThat(s2.shortage()).isZero();
        // 충분: 28912-2U000 target=200, current=250 → shortage=-50
        ProductInventorySummary s3 = invLookup.findById("28912-2U000").orElseThrow();
        assertThat(s3.shortage()).isEqualTo(-50);
        // 모두 0: 28415-08400
        ProductInventorySummary s4 = invLookup.findById("28415-08400").orElseThrow();
        assertThat(s4.targetStock()).isZero();
        assertThat(s4.currentStock()).isZero();
    }

    // ---------- Q_ext 통합 (TC-EX-010) ----------

    @Test
    @DisplayName("Q_ext 통합 — 29673-2R060 Q_vc=200, inventory(500, 100) → 600 (Q_vc + 400 부족 보충)")
    void q_ext_for_shortage_hose() {
        int qExt = demandCalc.computeForHose("29673-2R060", 200);
        assertThat(qExt).as("Q_ext = max(0, 200 + 500 − 100) = 600").isEqualTo(600);
    }

    @Test
    @DisplayName("Q_ext 통합 — 28912-2U000 Q_vc=100, inventory(200, 250) → 50 (충분 재고 차감)")
    void q_ext_for_sufficient_hose() {
        int qExt = demandCalc.computeForHose("28912-2U000", 100);
        assertThat(qExt).as("Q_ext = max(0, 100 + 200 − 250) = 50").isEqualTo(50);
    }

    @Test
    @DisplayName("Q_ext 통합 — 미등록 hose → Q_ext = Q_vc (보수적)")
    void q_ext_for_unknown_hose() {
        int qExt = demandCalc.computeForHose("UNKNOWN-HOSE", 100);
        assertThat(qExt).isEqualTo(100);
    }
}
