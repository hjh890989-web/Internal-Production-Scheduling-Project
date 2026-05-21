package com.scheduling.integration;

import com.scheduling.vc.capacity.CapacityLedger;
import com.scheduling.vc.capacity.CapacityLedgerBuilder;
import com.scheduling.vc.capacity.SlotAvailability;
import com.scheduling.vc.domain.RotationSlot;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-05 ST-05-1 TK-05-1-2/3 — CapacityLedger Testcontainers IT.
 *
 * <p>실 PG + V008/V009 (vc_machine seed) + V010 (vc_schedule) 적용 후:
 * <ul>
 *   <li>1일 격자 = LP 4 × 18 × 8 + IC × 18 × 6 = 684 셀 (BR-V05)</li>
 *   <li>저압 회전 capa = 72 (BR-V05)</li>
 *   <li>IC 회전 capa = 18</li>
 *   <li>주말 제외</li>
 *   <li>기존 CANDIDATE → RESERVED / CONFIRMED·DONE → CONFIRMED</li>
 *   <li>7일 호라이즌 build ≤ 500ms (4800 entry — 여유 margin)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CapacityLedgerIT {

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

    @Autowired private CapacityLedgerBuilder builder;
    @Autowired private VcScheduleRepository scheduleRepo;

    // TK-06-1-1 master.holiday seed 후 — 설날 연휴(2/16~18)·삼일절(3/1) 회피, 2026-02-23(월) ~ 27(금) 사용
    private static final LocalDate MON = LocalDate.of(2026, 2, 23);  // 월 (설날 직후 평일 주)
    private static final LocalDate SAT = LocalDate.of(2026, 2, 28);  // 토
    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    @BeforeEach
    void cleanState() {
        scheduleRepo.deleteAll();
    }

    @Test
    @DisplayName("BR-V05 일일 격자 = 684 셀 (LP 4×18×8 + IC 1×18×6)")
    void daily_capacity_matches_br_v05() {
        CapacityLedger ledger = builder.build(MON, MON);

        assertThat(ledger.totalCellCount()).isEqualTo(684);
        // 모든 셀 AVAILABLE (스케줄 0)
        assertThat(ledger.cells().values()).allMatch(v -> v == SlotAvailability.AVAILABLE);
    }

    @Test
    @DisplayName("BR-V05 저압 회전 capa = 72 (LP 4대 × 18 회전, distinct)")
    void lp_rotations_72() {
        CapacityLedger ledger = builder.build(MON, MON);
        assertThat(ledger.countLpRotationsAvailable(MON)).isEqualTo(72L);
    }

    @Test
    @DisplayName("IC 회전 capa = 18 (IC 1대 × 18 회전)")
    void ic_rotations_18() {
        CapacityLedger ledger = builder.build(MON, MON);
        assertThat(ledger.countIcRotationsAvailable(MON)).isEqualTo(18L);
    }

    @Test
    @DisplayName("주말 (토) → 격자 0")
    void weekend_excluded() {
        CapacityLedger ledger = builder.build(SAT, SAT);
        assertThat(ledger.totalCellCount()).isZero();
    }

    @Test
    @DisplayName("7일 호라이즌 — 영업일 5일 × 684 = 3420 셀")
    void seven_day_horizon_weekdays_only() {
        // 2026-02-16(월) ~ 2026-02-22(일) — 영업일 5 (월~금)
        CapacityLedger ledger = builder.build(MON, MON.plusDays(6));
        assertThat(ledger.totalCellCount()).isEqualTo(684 * 5);
    }

    @Test
    @DisplayName("기존 CANDIDATE → RESERVED, CONFIRMED → CONFIRMED")
    void existing_schedules_mapped() {
        VcSchedule cand = new VcSchedule(UUID.randomUUID(), "29673-2F900", "LP-01",
            (short) 3, MON, (short) 5, "ANGLE-01", 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);
        VcSchedule conf = new VcSchedule(UUID.randomUUID(), "29673-2F900", "IC-01",
            (short) 2, MON, (short) 7, "ANGLE-02", 80, VcScheduleStatus.CONFIRMED,
            "", T0, T0);
        scheduleRepo.save(cand);
        scheduleRepo.save(conf);

        CapacityLedger ledger = builder.build(MON, MON);

        assertThat(ledger.check(new RotationSlot(MON, "LP-01", 5, 3)))
            .isEqualTo(SlotAvailability.RESERVED);
        assertThat(ledger.check(new RotationSlot(MON, "IC-01", 7, 2)))
            .isEqualTo(SlotAvailability.CONFIRMED);
        // 미배치 셀 — AVAILABLE
        assertThat(ledger.check(new RotationSlot(MON, "LP-01", 6, 1)))
            .isEqualTo(SlotAvailability.AVAILABLE);
        // LP 회전 capa — 1 slot RESERVED 여도 같은 rotation 의 7 slot 가용 → distinct 카운트 변화 X (72 유지)
        assertThat(ledger.countLpRotationsAvailable(MON)).isEqualTo(72L);
    }

    @Test
    @DisplayName("LP-01 rotation 5 의 모든 8 slot RESERVED → distinct count -1 (71)")
    void all_slots_of_rotation_reserved_reduces_count() {
        for (int slot = 1; slot <= 8; slot++) {
            VcSchedule s = new VcSchedule(UUID.randomUUID(), "29673-2F900", "LP-01",
                (short) slot, MON, (short) 5, "ANGLE-01", 100, VcScheduleStatus.CANDIDATE,
                "", T0, T0);
            scheduleRepo.save(s);
        }
        CapacityLedger ledger = builder.build(MON, MON);
        assertThat(ledger.countLpRotationsAvailable(MON)).isEqualTo(71L);
    }

    @Test
    @DisplayName("findAvailableForMachineOnDate — 정렬 (rotation_no asc, slot_position asc)")
    void available_slots_sorted() {
        CapacityLedger ledger = builder.build(MON, MON);
        var lp01 = ledger.findAvailableForMachineOnDate(MON, "LP-01");
        // LP-01: 18 × 8 = 144 셀
        assertThat(lp01).hasSize(144);
        // 첫 셀 — rotation 1, slot 1
        assertThat(lp01.get(0).rotationNo()).isEqualTo(1);
        assertThat(lp01.get(0).slotPosition()).isEqualTo(1);
        // 마지막 셀 — rotation 18, slot 8
        assertThat(lp01.get(143).rotationNo()).isEqualTo(18);
        assertThat(lp01.get(143).slotPosition()).isEqualTo(8);
    }

    @Test
    @DisplayName("성능 — 7일 호라이즌 빌드 ≤ 500ms (3420 셀)")
    void build_performance_seven_days() {
        // 워밍업
        builder.build(MON, MON);

        long start = System.nanoTime();
        CapacityLedger ledger = builder.build(MON, MON.plusDays(6));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(ledger.totalCellCount()).isEqualTo(684 * 5);
        assertThat(elapsedMs).as("7일 호라이즌 빌드 (실측 %dms)", elapsedMs)
            .isLessThanOrEqualTo(500);
    }
}
