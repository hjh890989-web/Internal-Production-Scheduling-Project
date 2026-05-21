package com.scheduling.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.vc.SlotCompatibilityMatrix;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.master.vc.SlotPosition;
import com.scheduling.master.vc.VcConstraintRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-04 ST-04-1 TK-04-1-4 — DS-VC-CONSTRAINT-47 100건 회귀 + Unschedulable + 47품번 IT.
 *
 * <p>TC-VC-002 (100건 위반 0) + TC-VC-001 (≤1초) + TC-VC-003 (Unschedulable 분리).
 *
 * <p>{@code @Sql} 로 매 테스트 전 master_seed.sql 적재 → 46품번 (REF-09 실측) 시드.
 * {@link SlotCompatibilityMatrixService#invalidate} 호출로 매트릭스 강제 재빌드 (LISTEN/NOTIFY
 * 비동기 race 회피).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class SlotMatrixAccuracyIT {

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

    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private VcConstraintRepository repository;
    @Autowired private ObjectMapper objectMapper;

    private static final int EXPECTED_HOSE_COUNT = 46;     // REF-09 실측
    private static final int EXPECTED_CASE_COUNT = 100;

    @BeforeEach
    void rebuild() {
        // @Sql 적재 → matrix 강제 재빌드 (LISTEN/NOTIFY 비동기 race 회피)
        matrixService.invalidate();
    }

    private List<SlotCase> loadCases() throws Exception {
        ClassPathResource resource = new ClassPathResource("datasets/DS-VC-CONSTRAINT-47/slot_violation_cases.json");
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<SlotCase>>() {});
        }
    }

    private List<String> loadExpectedUnschedulable() throws Exception {
        ClassPathResource resource = new ClassPathResource("datasets/DS-VC-CONSTRAINT-47/unschedulable_expected.json");
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<String>>() {});
        }
    }

    // ---------- TC-VC-002 100건 회귀 (정확도 100% / 위반 0) ----------

    @Test
    @DisplayName("DS-VC-CONSTRAINT-47 × 100 케이스 — 위반 0건 (TC-VC-002)")
    void slot_violations_zero_across_100_cases() throws Exception {
        List<SlotCase> cases = loadCases();
        assertThat(cases).hasSize(EXPECTED_CASE_COUNT);

        SlotCompatibilityMatrix matrix = matrixService.current();
        assertThat(matrix.byHose()).hasSize(EXPECTED_HOSE_COUNT);

        int correct = 0;
        int violations = 0;
        StringBuilder errors = new StringBuilder();

        for (SlotCase c : cases) {
            boolean actual = matrix.isEligible(c.hoseId(), SlotPosition.valueOf(c.slotPosition()));
            if (actual == c.expectedEligible()) {
                correct++;
            } else {
                violations++;
                errors.append(String.format("Case %s: hose=%s slot=%s expected=%s actual=%s%n",
                    c.caseId(), c.hoseId(), c.slotPosition(), c.expectedEligible(), actual));
            }
        }

        assertThat(violations)
            .as("Slot O/X 위반 — 반드시 0 (TC-VC-002):%n%s", errors)
            .isZero();
        assertThat(correct).isEqualTo(EXPECTED_CASE_COUNT);
    }

    // ---------- 47품번 (실측 46) 매트릭스 등록 ----------

    @Test
    @DisplayName("REF-09 46품번 모두 매트릭스 등록 (TC-VC-001 정합)")
    void all_hoses_indexed() {
        SlotCompatibilityMatrix matrix = matrixService.current();
        assertThat(matrix.byHose()).hasSize(EXPECTED_HOSE_COUNT);
        assertThat(repository.count()).isEqualTo(EXPECTED_HOSE_COUNT);
    }

    @Test
    @DisplayName("모든 7 슬롯 위치에 ≥ 1 가능 품번 (현실 검증)")
    void every_slot_has_at_least_one_eligible() {
        SlotCompatibilityMatrix matrix = matrixService.current();
        for (SlotPosition slot : SlotPosition.values()) {
            assertThat(matrix.eligibleHoseIdsFor(slot))
                .as("Slot %s 에 가능 품번 ≥ 1", slot)
                .isNotEmpty();
        }
    }

    // ---------- TC-VC-003 Unschedulable 분리 ----------

    @Test
    @DisplayName("REF-09 zero-slot 품번 100% 식별 — 7X375-H0020 + 28415-08400 포함 (TC-VC-003)")
    void unschedulable_matches_ref09_expected() throws Exception {
        List<String> expected = loadExpectedUnschedulable();

        SlotCompatibilityMatrix matrix = matrixService.current();
        Set<String> actual = matrix.unschedulableHoseIds();

        assertThat(actual)
            .as("REF-09 Unschedulable 정답: %s", expected)
            .containsExactlyInAnyOrderElementsOf(expected);
        // 스펙 명시 — 핵심 zero-slot 품번
        assertThat(actual).contains("7X375-H0020", "28415-08400");

        // Repository vs Matrix 일관성
        List<String> repoUnsched = repository.findUnschedulable().stream()
            .map(v -> v.getHoseId()).toList();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(repoUnsched);
    }

    // ---------- TC-VC-001 100회 rebuild p95 ≤ 1000ms ----------

    @Test
    @DisplayName("rebuild p95 ≤ 1000ms — 100회 측정 (TC-VC-001)")
    void rebuild_p95_within_1_second() {
        // JIT 워밍업 5회
        for (int i = 0; i < 5; i++) {
            matrixService.invalidate();
        }

        List<Long> durations = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            long startNanos = System.nanoTime();
            matrixService.invalidate();
            durations.add((System.nanoTime() - startNanos) / 1_000_000);
        }
        Collections.sort(durations);
        long p95 = durations.get(94);
        long p99 = durations.get(98);
        long max = durations.get(99);

        assertThat(p95)
            .as("rebuild p95 ≤ 1000ms (실측 p95=%dms, p99=%dms, max=%dms)", p95, p99, max)
            .isLessThanOrEqualTo(1000L);
    }

    /** slot_violation_cases.json 의 1 케이스. */
    record SlotCase(String caseId, String hoseId, String slotPosition, boolean expectedEligible) {
        public SlotCase(
            @com.fasterxml.jackson.annotation.JsonProperty("case_id") String caseId,
            @com.fasterxml.jackson.annotation.JsonProperty("hose_id") String hoseId,
            @com.fasterxml.jackson.annotation.JsonProperty("slot_position") String slotPosition,
            @com.fasterxml.jackson.annotation.JsonProperty("expected_eligible") boolean expectedEligible
        ) {
            this.caseId = caseId;
            this.hoseId = hoseId;
            this.slotPosition = slotPosition;
            this.expectedEligible = expectedEligible;
        }
    }
}
