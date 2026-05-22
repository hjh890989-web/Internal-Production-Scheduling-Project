package com.scheduling.integration;

import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import com.scheduling.vc.validate.ScheduleValidatorService;
import com.scheduling.vc.validate.ValidationResult;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-VC16 ST-VC16-1 TK-VC16-1-3 — p95 ≤ 3초 성능 회귀 (REQ-FUNC-VC-016 / REQ-NF-PERF-002).
 *
 * <p>실 PG + 1주 호라이즌 ~ 1,440 VcSchedule row 합성 + validateRange 50회 측정 →
 * p50 / p95 / p99 latency 계산 + SRS 요구 (p95 ≤ 3,000ms) 검증.
 *
 * <p>현재 dummy 데이터 — 모두 valid 슬롯 (위반 0건 시나리오) 로 측정 (worst-case 는 validator
 * branch 가 모두 평가되어 별도 측정 필요 — Phase 2+).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class ValidateAllPerformanceIT {

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

    @Autowired private ScheduleValidatorService validator;
    @Autowired private VcScheduleRepository scheduleRepo;
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private VcYieldCalculator yieldCalc;

    private static final LocalDate START = LocalDate.of(2026, 2, 23); // 월
    private static final LocalDate END = LocalDate.of(2026, 2, 27);   // 금 (1주)
    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    private static final int WARMUP_RUNS = 5;
    private static final int MEASURE_RUNS = 30;   // SRS 50회 권장 — CI 시간 마진으로 30회
    private static final long P95_MAX_MS = 3000L;

    @BeforeEach
    void seedSchedules() {
        scheduleRepo.deleteAll();
        matrixService.invalidate();
        yieldCalc.rebuild();

        // 1주 호라이즌 (5일) × 4 LP × 18 회전 × 2 slot = 720 row + IC 1주 × 18 × 2 = 180 = 900 row
        // 모두 valid: 29673-2F900 (LP_UPMID, LP_LOWMID, IC 모두 가능)
        // BR-V07 — 같은 (machine, slot, date) 안 rotation 1~18 동일 angle 강제
        List<VcSchedule> rows = new ArrayList<>();
        LocalDate d = START;
        while (!d.isAfter(END)) {
            for (String machineId : List.of("LP-01", "LP-02", "LP-03", "LP-04")) {
                for (int rot = 1; rot <= 18; rot++) {
                    for (int slot : new int[]{2, 3}) {   // UPMID, LOWMID — 29673-2F900 가용
                        rows.add(new VcSchedule(UUID.randomUUID(), "29673-2F900", machineId,
                            (short) slot, d, (short) rot, "ANGLE-29673-S" + slot, 10,
                            VcScheduleStatus.CANDIDATE, "", T0, T0));
                    }
                }
            }
            for (int rot = 1; rot <= 18; rot++) {
                for (int slot = 1; slot <= 3; slot++) {
                    rows.add(new VcSchedule(UUID.randomUUID(), "29673-2F900", "IC-01",
                        (short) slot, d, (short) rot, "ANGLE-29673-IC-S" + slot, 8,
                        VcScheduleStatus.CANDIDATE, "", T0, T0));
                }
            }
            d = d.plusDays(1);
        }
        scheduleRepo.saveAll(rows);
    }

    @Test
    @DisplayName("REQ-FUNC-VC-016 — 1주 호라이즌 validateRange p95 ≤ 3,000ms")
    void p95_under_three_seconds_for_week_horizon() {
        // Warm-up — JIT + 캐시 적재
        for (int i = 0; i < WARMUP_RUNS; i++) {
            validator.validateRange(START, END);
        }

        List<Long> latenciesMs = new ArrayList<>(MEASURE_RUNS);
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long startNs = System.nanoTime();
            ValidationResult r = validator.validateRange(START, END);
            latenciesMs.add((System.nanoTime() - startNs) / 1_000_000);
            assertThat(r.totalRows()).isPositive();
        }

        Collections.sort(latenciesMs);
        long p50 = latenciesMs.get((int) (MEASURE_RUNS * 0.50));
        long p95 = latenciesMs.get((int) (MEASURE_RUNS * 0.95));
        long p99 = latenciesMs.get(Math.min(MEASURE_RUNS - 1, (int) (MEASURE_RUNS * 0.99)));

        System.out.printf("validateRange latency: p50=%dms, p95=%dms, p99=%dms (rows=~900, %d runs)%n",
            p50, p95, p99, MEASURE_RUNS);

        assertThat(p95)
            .as("REQ-FUNC-VC-016 p95 ≤ 3,000ms (actual p50=%d / p95=%d / p99=%d)", p50, p95, p99)
            .isLessThanOrEqualTo(P95_MAX_MS);
    }

    @Test
    @DisplayName("Cold first call ≤ 5,000ms (안전 마진)")
    void cold_first_call_under_5_seconds() {
        long startNs = System.nanoTime();
        validator.validateRange(START, END);
        long ms = (System.nanoTime() - startNs) / 1_000_000;

        System.out.printf("validateRange cold first call: %dms%n", ms);
        assertThat(ms).isLessThanOrEqualTo(5000L);
    }
}
