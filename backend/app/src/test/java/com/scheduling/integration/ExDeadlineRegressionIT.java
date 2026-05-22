package com.scheduling.integration;

import com.scheduling.ex.deadline.BackwardExtrusionCalculator;
import com.scheduling.ex.deadline.ExDeadlineMap;
import com.scheduling.ex.event.ExtrusionScheduleService;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.events.VcConfirmedEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-07 ST-07-1 TK-07-1-3 — TC-EX-001 D-1 deadline 회귀 100건.
 *
 * <p>실 PG + V012 master.holiday seed + BackwardExtrusionCalculator + ExtrusionScheduleService
 * 통합 검증.
 *
 * <p>100 random VcConfirmedEvent → ExScheduleCandidate 생성 → 모든 row
 * {@code extrusion_deadline ≤ vc_production_date − 1 working day} 만족 검증.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExDeadlineRegressionIT {

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

    @Autowired private ExtrusionScheduleService extrusionService;
    @Autowired private BackwardExtrusionCalculator deadlineCalc;
    @Autowired private WorkingCalendar calendar;
    @Autowired private ExScheduleCandidateRepository candidateRepo;

    @BeforeEach
    void clean() {
        candidateRepo.deleteAll();
    }

    private VcConfirmedEvent.VcConfirmedRow row(String hose, LocalDate date) {
        return new VcConfirmedEvent.VcConfirmedRow(
            UUID.randomUUID(), hose, date, "LP-01", (short) 1, (short) 1, 100);
    }

    private List<LocalDate> workingDayCandidates(int count) {
        List<LocalDate> out = new ArrayList<>();
        LocalDate d = LocalDate.of(2026, 2, 23);
        while (out.size() < count) {
            d = d.plusDays(1);
            if (calendar.isWorkingDay(d)) out.add(d);
        }
        return out;
    }

    // ---------- TC-EX-001 ----------

    @Test
    @DisplayName("100 random 시나리오 — 모든 candidate.extrusion_deadline ≤ vc_date - 1 working day")
    void all_candidates_within_d1_deadline_for_100_scenarios() {
        Random rng = new Random(20260522L);
        List<LocalDate> days = workingDayCandidates(60);
        String[] hoses = {"29673-2F900", "29693-2U000", "28912-2U000",
                          "28442-6T010", "A6722030900", "29689-2U000"};

        List<VcConfirmedEvent> events = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String hose = hoses[rng.nextInt(hoses.length)];
            LocalDate vcDate = days.get(rng.nextInt(days.size()));
            VcConfirmedEvent event = new VcConfirmedEvent(
                UUID.randomUUID(), Instant.now(), List.of(row(hose, vcDate)));
            events.add(event);
        }

        int totalCreated = 0;
        for (VcConfirmedEvent event : events) {
            totalCreated += extrusionService.generateCandidates(event);
        }

        List<ExScheduleCandidate> all = candidateRepo.findAll();
        assertThat(all).hasSize(totalCreated);
        assertThat(all).hasSizeGreaterThanOrEqualTo(100);

        // 모든 candidate — extrusion_deadline ≤ vc_date − 1 working day
        List<ExScheduleCandidate> violations = all.stream()
            .filter(c -> {
                LocalDate expected = calendar.subtractWorkingDays(c.getVcProductionDate(), 1);
                return c.getExtrusionDeadline().isAfter(expected);
            })
            .toList();

        assertThat(violations).as("BR-E01 위반: %d candidate", violations.size()).isEmpty();

        // NS-S07 D-1 준수율 = 100% (회귀 시점)
        long compliant = all.size() - violations.size();
        double rate = (double) compliant / all.size();
        assertThat(rate).as("NS-S07 D-1 준수율 (REQ-NF-KPI-009)").isGreaterThanOrEqualTo(0.98);

        // 모든 candidate.status = PENDING (EP-08 yield 계산 전)
        assertThat(all).allMatch(c -> c.getStatus() == CandidateStatus.PENDING);
    }

    @Test
    @DisplayName("같은 hose 다중 vc_date → 가장 이른 vc_date 기준 deadline (hard 제약)")
    void single_hose_multi_vc_rows_uses_earliest_vc_date() {
        LocalDate early = LocalDate.of(2026, 3, 5);
        LocalDate mid = LocalDate.of(2026, 3, 11);
        LocalDate late = LocalDate.of(2026, 3, 18);

        VcConfirmedEvent event = new VcConfirmedEvent(UUID.randomUUID(), Instant.now(), List.of(
            row("28912-2U000", mid),
            row("28912-2U000", early),
            row("28912-2U000", late)
        ));

        ExDeadlineMap deadlines = deadlineCalc.compute(event);
        LocalDate expected = calendar.subtractWorkingDays(early, 1);

        assertThat(deadlines.get("28912-2U000")).contains(expected);
    }

    @Test
    @DisplayName("멱등 — 같은 vc_row_id 재발행 시 기존 row 보존 (UNIQUE)")
    void idempotent_same_vc_row_id() {
        UUID rowId = UUID.randomUUID();
        VcConfirmedEvent.VcConfirmedRow shared = new VcConfirmedEvent.VcConfirmedRow(
            rowId, "29673-2F900", LocalDate.of(2026, 3, 5),
            "LP-01", (short) 1, (short) 1, 100);

        VcConfirmedEvent event = new VcConfirmedEvent(UUID.randomUUID(), Instant.now(), List.of(shared));

        int first = extrusionService.generateCandidates(event);
        int second = extrusionService.generateCandidates(event);   // 재발행

        assertThat(first).isEqualTo(1);
        assertThat(second).as("재발행 시 기존 row 보존 → 신규 0").isZero();
        assertThat(candidateRepo.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("VcConfirmedEvent → ExScheduleCandidate 1:1 매핑 (status=PENDING)")
    void one_to_one_mapping_pending() {
        VcConfirmedEvent event = new VcConfirmedEvent(UUID.randomUUID(), Instant.now(), List.of(
            row("29673-2F900", LocalDate.of(2026, 3, 5)),
            row("28912-2U000", LocalDate.of(2026, 3, 11)),
            row("28442-6T010", LocalDate.of(2026, 3, 16))
        ));

        int created = extrusionService.generateCandidates(event);
        assertThat(created).isEqualTo(3);

        List<ExScheduleCandidate> all = candidateRepo.findAll();
        assertThat(all).hasSize(3);
        assertThat(all).allMatch(c -> c.getStatus() == CandidateStatus.PENDING);
        assertThat(all).allMatch(c -> c.getScheduleId().equals(event.scheduleId()));
    }
}
