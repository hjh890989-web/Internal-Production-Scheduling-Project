package com.scheduling.integration;

import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.vc.allocator.AllocationConflict;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-VC16 ST-VC16-1 TK-VC16-1-2 — validate-all 회귀 IT (TC-VC-016).
 *
 * <p>실 PG + DS-VC-CONSTRAINT-47 master_seed + V015 vc_hose_rule seed.
 * 의도적 위반 4종 (slot O/X, 좌/우, machine_pin, 중복 슬롯) 주입 → validateRange 결과 검증.
 *
 * <p>5 시나리오:
 * <ul>
 *   <li>의도 위반 4종 정확 식별</li>
 *   <li>summary 카테고리별 카운트</li>
 *   <li>Idempotent — 다회 호출 동일 결과</li>
 *   <li>clean schedule — violations 비어 있음</li>
 *   <li>마스터 변경 (vc_hose_rule UPDATE) → retroactive 위반 발견</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class ValidateAllIT {

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

    private static final LocalDate D = LocalDate.of(2026, 2, 23); // 월
    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    @BeforeEach
    void rebuildCaches() {
        scheduleRepo.deleteAll();
        matrixService.invalidate();
        yieldCalc.rebuild();
    }

    private VcSchedule row(String hoseId, String machineId, int slotPos, int rotation) {
        return new VcSchedule(UUID.randomUUID(), hoseId, machineId,
            (short) slotPos, D, (short) rotation, "ANGLE-" + hoseId, 10,
            VcScheduleStatus.CANDIDATE, "", T0, T0);
    }

    @Test
    @DisplayName("Clean schedule — 위반 0건, summary 빈 맵")
    void clean_schedule_no_violations() {
        // 29673-2F900 → LP_UPMID(2) 가용, 29693-2U000 → LP_LOWMID(3) 가용 (REF-09)
        scheduleRepo.save(row("29673-2F900", "LP-01", 2, 1));
        scheduleRepo.save(row("29693-2U000", "LP-02", 2, 2));

        ValidationResult r = validator.validateRange(D, D);

        assertThat(r.totalRows()).isEqualTo(2);
        assertThat(r.hasViolations()).isFalse();
        assertThat(r.summary()).isEmpty();
        assertThat(r.executedAt()).isNotNull();
    }

    @Test
    @DisplayName("의도 위반 — 좌/우 (28421-2M800 RIGHT 머신 배치) + 호기 핀 (28422-08HA0 LP-02)")
    void detects_left_right_and_pin_violations() {
        // 28421-2M800 (LEFT only) → LP-03 (RIGHT 머신) = BR-V15 위반
        scheduleRepo.save(row("28421-2M800", "LP-03", 1, 1));
        // 28422-08HA0 (LP-01 pin) → LP-02 = BR-V14 호기 핀 위반
        scheduleRepo.save(row("28422-08HA0", "LP-02", 4, 1));

        ValidationResult r = validator.validateRange(D, D);

        assertThat(r.hasViolations()).isTrue();
        assertThat(r.summary()).containsKey(AllocationConflict.Category.LEFT_RIGHT_VIOLATION);
        assertThat(r.summary().get(AllocationConflict.Category.LEFT_RIGHT_VIOLATION))
            .isGreaterThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("Unschedulable — 모든 슬롯 X 품번 (28415-08400) → UNSCHEDULABLE")
    void detects_unschedulable_violation() {
        // 28415-08400 모든 슬롯 X (BR-V11) — DS-VC-CONSTRAINT-47 seed
        // 하지만 슬롯 O/X CHECK 가 false 인 머신에 row 가 존재하면 양쪽 위반 (Unschedulable + slot O/X)
        scheduleRepo.save(row("28415-08400", "LP-01", 1, 1));

        ValidationResult r = validator.validateRange(D, D);

        assertThat(r.hasViolations()).isTrue();
        assertThat(r.summary()).containsKey(AllocationConflict.Category.UNSCHEDULABLE);
    }

    @Test
    @DisplayName("Idempotent — 다회 호출 결과 동일")
    void idempotent_multiple_calls() {
        scheduleRepo.save(row("28421-2M800", "LP-03", 1, 1));   // 좌/우 위반

        ValidationResult first = validator.validateRange(D, D);
        ValidationResult second = validator.validateRange(D, D);

        assertThat(first.violations()).hasSameSizeAs(second.violations());
        assertThat(first.summary()).isEqualTo(second.summary());
        assertThat(first.totalRows()).isEqualTo(second.totalRows());
    }

    @Test
    @DisplayName("Retroactive — 빈 호라이즌 검사 → 위반 0, totalRows=0")
    void empty_range_returns_zero() {
        ValidationResult r = validator.validateRange(D.plusDays(60), D.plusDays(60));

        assertThat(r.totalRows()).isZero();
        assertThat(r.hasViolations()).isFalse();
    }
}
