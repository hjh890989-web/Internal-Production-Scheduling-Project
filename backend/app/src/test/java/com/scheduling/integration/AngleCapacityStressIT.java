package com.scheduling.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.domain.RotationSlot;
import com.scheduling.vc.yield.AngleCapacityValidator;
import com.scheduling.vc.yield.AngleCapacityViolation;
import com.scheduling.vc.yield.VcYieldCalculator;
import com.scheduling.vc.yield.YieldMatrix;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-05 ST-05-2 TK-05-2-3 — DS-ANGLE-STRESS-1000 회귀 (TC-VC-007).
 *
 * <p>REF-09 46품번 master_seed + 1000 무작위 시나리오 → AngleCapacityValidator 가
 * 의도적 위반 100% 식별 + 정상 시나리오 false-positive 0건.
 *
 * <p>VcYieldCalculator initialBuild 도 검증 — 46품번 yield 매트릭스 생성.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class AngleCapacityStressIT {

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

    @Autowired private AngleCapacityValidator validator;
    @Autowired private VcYieldCalculator yieldCalculator;
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private ObjectMapper objectMapper;

    private static final LocalDate D = LocalDate.of(2026, 2, 16);

    @BeforeEach
    void rebuildCaches() {
        // @Sql 적재 후 matrix + yield 재빌드
        matrixService.invalidate();
        yieldCalculator.rebuild();
    }

    private List<StressScenario> loadScenarios() throws Exception {
        ClassPathResource res = new ClassPathResource("datasets/DS-ANGLE-STRESS-1000/stress_scenarios.json");
        try (InputStream is = res.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<StressScenario>>() {});
        }
    }

    // ---------- TC-VC-007 ----------

    @Test
    @DisplayName("DS-ANGLE-STRESS-1000 — 의도적 위반 100% 식별 + 정상 false-positive 0건")
    void detects_all_intended_violations() throws Exception {
        List<StressScenario> scenarios = loadScenarios();
        assertThat(scenarios).hasSize(1000);

        int expectedViolations = 0;
        int detected = 0;
        int falsePositives = 0;

        for (StressScenario sc : scenarios) {
            // 단일 hose 의 slots 만 — Map 한 entry 로 그룹핑
            List<RotationSlot> slots = sc.slots().stream()
                .map(s -> new RotationSlot(D, s.machineId(), s.rotationNo(), s.slotPosition()))
                .toList();
            Map<String, List<RotationSlot>> assignments = Map.of(sc.hoseId(), slots);

            List<AngleCapacityViolation> violations = validator.validate(assignments);

            if (sc.expectedViolation()) {
                expectedViolations++;
                if (!violations.isEmpty()) detected++;
            } else if (!violations.isEmpty()) {
                falsePositives++;
            }
        }

        assertThat(detected)
            .as("의도적 위반 100%% 식별: %d/%d", detected, expectedViolations)
            .isEqualTo(expectedViolations);
        assertThat(falsePositives)
            .as("정상 시나리오 false-positive 0건")
            .isZero();
        assertThat(expectedViolations).as("회귀 의미 확보 (≥ 30 위반)").isGreaterThanOrEqualTo(30);
    }

    @Test
    @DisplayName("VcYieldCalculator initialBuild — 46품번 yield 매트릭스 (DS-VC-CONSTRAINT-47 정합)")
    void yield_calculator_initial_build() {
        YieldMatrix matrix = yieldCalculator.currentMatrix();
        assertThat(matrix).isNotNull();
        // 일부 품번은 LP+IC 양쪽 가능 → lpYields + unschedulable ≤ 46 (전체 품번 수)
        assertThat(matrix.lpYields().size() + matrix.unschedulableYields().size())
            .isLessThanOrEqualTo(46);
    }

    @Test
    @DisplayName("REF-09 unschedulable 3 품번 (DS-VC-CONSTRAINT-47) yield 매트릭스 → unschedulable 집합 포함")
    void ref09_unschedulable_in_yield_matrix() {
        YieldMatrix matrix = yieldCalculator.currentMatrix();
        // master_seed.sql 기준 zero-slot 품번 (7X375-H0020, 28415-08400, 37863-8EXJ0)
        assertThat(matrix.unschedulableYields())
            .contains("7X375-H0020", "28415-08400", "37863-8EXJ0");
    }

    /** DS-ANGLE-STRESS-1000/stress_scenarios.json 의 1 trial. */
    public record StressScenario(
        int trial,
        String hoseId,
        String machineType,
        String machineId,
        int rotationNo,
        int allowedAngles,
        int slotCount,
        List<SlotEntry> slots,
        boolean expectedViolation
    ) {
        public StressScenario(
            @com.fasterxml.jackson.annotation.JsonProperty("trial") int trial,
            @com.fasterxml.jackson.annotation.JsonProperty("hose_id") String hoseId,
            @com.fasterxml.jackson.annotation.JsonProperty("machine_type") String machineType,
            @com.fasterxml.jackson.annotation.JsonProperty("machine_id") String machineId,
            @com.fasterxml.jackson.annotation.JsonProperty("rotation_no") int rotationNo,
            @com.fasterxml.jackson.annotation.JsonProperty("allowed_angles") int allowedAngles,
            @com.fasterxml.jackson.annotation.JsonProperty("slot_count") int slotCount,
            @com.fasterxml.jackson.annotation.JsonProperty("slots") List<SlotEntry> slots,
            @com.fasterxml.jackson.annotation.JsonProperty("expected_violation") boolean expectedViolation
        ) {
            this.trial = trial;
            this.hoseId = hoseId;
            this.machineType = machineType;
            this.machineId = machineId;
            this.rotationNo = rotationNo;
            this.allowedAngles = allowedAngles;
            this.slotCount = slotCount;
            this.slots = slots != null ? slots : List.of();
            this.expectedViolation = expectedViolation;
        }
    }

    public record SlotEntry(String machineId, String machineType, int rotationNo, int slotPosition) {
        public SlotEntry(
            @com.fasterxml.jackson.annotation.JsonProperty("machine_id") String machineId,
            @com.fasterxml.jackson.annotation.JsonProperty("machine_type") String machineType,
            @com.fasterxml.jackson.annotation.JsonProperty("rotation_no") int rotationNo,
            @com.fasterxml.jackson.annotation.JsonProperty("slot_position") int slotPosition
        ) {
            this.machineId = machineId;
            this.machineType = machineType;
            this.rotationNo = rotationNo;
            this.slotPosition = slotPosition;
        }
    }
}
