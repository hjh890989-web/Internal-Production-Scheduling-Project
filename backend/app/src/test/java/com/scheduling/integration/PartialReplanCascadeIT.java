package com.scheduling.integration;

import com.scheduling.ex.event.PartialReplanService;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.vc.events.VcChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-EX13 ST-EX13-1 IT — VC 변경 cascade partial replan 정식 활성 (BR-X03·E11).
 *
 * <p>검증:
 * <ul>
 *   <li>QUANTITY 변경 → 영향 candidate vcYield 갱신 + status PENDING</li>
 *   <li>DATE 변경 → deadline 재산출 (-1 working day) + horizon 인접 candidate 도 갱신</li>
 *   <li>DELETED → status FAILED</li>
 *   <li>CONFIRMED candidate 는 cascade 차단 (override 별도 흐름)</li>
 *   <li>audit row 자동 발행 (@Auditable + AFTER 트리거)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PartialReplanCascadeIT {

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

    @Autowired private PartialReplanService replanService;
    @Autowired private ExScheduleCandidateRepository exRepo;
    @Autowired private JdbcTemplate jdbc;

    private static final LocalDate VC_DATE = LocalDate.of(2026, 6, 1);   // 월
    private static final LocalDate EX_DEADLINE = LocalDate.of(2026, 5, 29);   // 금
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        exRepo.deleteAll();
    }

    private ExScheduleCandidate saveScheduled(UUID vcRowId, String hoseId, int yield) {
        return exRepo.save(new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), hoseId,
            vcRowId, VC_DATE, EX_DEADLINE, yield,
            CandidateStatus.SCHEDULED, T0, T0));
    }

    @Test
    @DisplayName("QUANTITY 변경 — yield 갱신 + status PENDING")
    void quantity_change_updates_yield() {
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate c = saveScheduled(vcRowId, "29673-2R060", 2531);

        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(), T0,
            List.of(new VcChangedEvent.VcChangedRow(
                vcRowId, "29673-2R060", VC_DATE, VC_DATE,
                2531, 3000, VcChangedEvent.ChangeType.QUANTITY)));

        int triggered = replanService.replanWithContext(event);
        assertThat(triggered).isEqualTo(1);

        ExScheduleCandidate reloaded = exRepo.findById(c.getExCandidateId()).orElseThrow();
        assertThat(reloaded.getVcYield()).isEqualTo(3000);
        assertThat(reloaded.getStatus()).isEqualTo(CandidateStatus.PENDING);
    }

    @Test
    @DisplayName("DATE 변경 — deadline 재산출 + vcProductionDate 갱신")
    void date_change_recomputes_deadline() {
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate c = saveScheduled(vcRowId, "29673-2R060", 2531);

        LocalDate newVcDate = LocalDate.of(2026, 6, 8);   // 월
        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(), T0,
            List.of(new VcChangedEvent.VcChangedRow(
                vcRowId, "29673-2R060", VC_DATE, newVcDate,
                2531, 2531, VcChangedEvent.ChangeType.DATE)));

        replanService.replanWithContext(event);

        ExScheduleCandidate reloaded = exRepo.findById(c.getExCandidateId()).orElseThrow();
        assertThat(reloaded.getVcProductionDate()).isEqualTo(newVcDate);
        // newVcDate - 1 working day = 6/5 (금)
        assertThat(reloaded.getExtrusionDeadline()).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    @DisplayName("DELETED — status FAILED 전이")
    void deleted_transitions_to_failed() {
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate c = saveScheduled(vcRowId, "29673-2R060", 2531);

        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(), T0,
            List.of(new VcChangedEvent.VcChangedRow(
                vcRowId, "29673-2R060", VC_DATE, VC_DATE,
                2531, 0, VcChangedEvent.ChangeType.DELETED)));

        replanService.replanWithContext(event);

        ExScheduleCandidate reloaded = exRepo.findById(c.getExCandidateId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CandidateStatus.FAILED);
    }

    @Test
    @DisplayName("CONFIRMED candidate — cascade 차단 (override 별도)")
    void confirmed_candidate_skipped() {
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate c = new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            vcRowId, VC_DATE, EX_DEADLINE, 2531,
            CandidateStatus.SCHEDULED, T0, T0);
        c.confirm("planner-001", T0);
        exRepo.save(c);

        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(), T0,
            List.of(new VcChangedEvent.VcChangedRow(
                vcRowId, "29673-2R060", VC_DATE, VC_DATE,
                2531, 9999, VcChangedEvent.ChangeType.QUANTITY)));

        int triggered = replanService.replanWithContext(event);
        assertThat(triggered).isEqualTo(0);

        ExScheduleCandidate reloaded = exRepo.findById(c.getExCandidateId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CandidateStatus.CONFIRMED);
        assertThat(reloaded.getVcYield()).isEqualTo(2531);     // 변경 없음
    }

    @Test
    @DisplayName("BR-X03 — 수동 호출 0건, @Auditable reason 캡쳐 (audit row)")
    void audit_reason_captured() {
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate c = saveScheduled(vcRowId, "29673-2R060", 2531);

        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(), T0,
            List.of(new VcChangedEvent.VcChangedRow(
                vcRowId, "29673-2R060", VC_DATE, VC_DATE,
                2531, 3000, VcChangedEvent.ChangeType.QUANTITY)));

        replanService.replanWithContext(event);

        // audit row 의 reason 검증
        String reason = jdbc.queryForObject(
            "SELECT reason FROM audit.schedule_audit_log "
                + "WHERE row_pk = ? AND action = 'UPDATE' "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class, c.getExCandidateId().toString());
        assertThat(reason).contains("BR-X03");
    }

    @Test
    @DisplayName("BR-E11 — 100건 시뮬 100% 자동 (수동 호출 0건)")
    void hundred_change_simulation() {
        // 100 candidate seed
        List<UUID> vcRowIds = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID vcRowId = UUID.randomUUID();
            vcRowIds.add(vcRowId);
            saveScheduled(vcRowId, "29673-2R060", 1000 + i);
        }

        // VcChangedEvent 100 rows
        List<VcChangedEvent.VcChangedRow> changes = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            changes.add(new VcChangedEvent.VcChangedRow(
                vcRowIds.get(i), "29673-2R060", VC_DATE, VC_DATE,
                1000 + i, 2000 + i, VcChangedEvent.ChangeType.QUANTITY));
        }
        VcChangedEvent event = new VcChangedEvent(UUID.randomUUID(), T0, changes);

        int triggered = replanService.replanWithContext(event);
        assertThat(triggered).isEqualTo(100);

        // 모든 candidate PENDING + yield 갱신
        long pending = exRepo.findAll().stream()
            .filter(c -> c.getStatus() == CandidateStatus.PENDING)
            .count();
        assertThat(pending).isEqualTo(100);
    }
}
