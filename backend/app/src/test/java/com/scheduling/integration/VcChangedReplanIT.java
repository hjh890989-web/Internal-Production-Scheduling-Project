package com.scheduling.integration;

import com.scheduling.ex.event.ImpactedRowFinder;
import com.scheduling.ex.event.PartialReplanService;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.vc.events.VcChangedEvent;
import com.scheduling.vc.internal.VcChangedPublisher;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-EX13 ST-EX13-1 통합 IT — VcChanged 이벤트 → 영향 EX candidate 자동 재계산.
 *
 * <p>Sprint 3 단계: publisher + listener + impacted finder + replan stub 통합.
 * Sprint 4 EP-10 완료 후 partial replan 본격 활성 (yield 재계산 + grouping 재배치).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VcChangedReplanIT {

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

    @Autowired private ImpactedRowFinder finder;
    @Autowired private PartialReplanService replanService;
    @Autowired private ExScheduleCandidateRepository candidateRepo;
    @Autowired private VcChangedPublisher publisher;

    @BeforeEach
    void clean() {
        candidateRepo.deleteAll();
    }

    private ExScheduleCandidate seedCandidate(UUID vcRowId, String hose, LocalDate deadline,
                                                CandidateStatus status) {
        Instant now = Instant.parse("2026-05-22T00:00:00Z");
        ExScheduleCandidate c = new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), hose, vcRowId,
            deadline.plusDays(1), deadline, 100, status, now, now);
        return candidateRepo.save(c);
    }

    // ---------- ImpactedRowFinder + PartialReplanService 통합 ----------

    @Test
    @DisplayName("QUANTITY 변경 → 매핑 candidate 1건 → PENDING 재전환")
    void quantity_change_replan() {
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate seed = seedCandidate(vcRowId, "29673-2F900",
            LocalDate.of(2026, 3, 5), CandidateStatus.READY);

        VcChangedEvent event = new VcChangedEvent(UUID.randomUUID(), Instant.now(), List.of(
            new VcChangedEvent.VcChangedRow(vcRowId, "29673-2F900", null, null,
                100, 200, VcChangedEvent.ChangeType.QUANTITY)));

        List<UUID> impacted = finder.findImpacted(event);
        int triggered = replanService.triggerReplan(impacted);

        assertThat(impacted).containsExactly(seed.getExCandidateId());
        assertThat(triggered).isEqualTo(1);

        ExScheduleCandidate updated = candidateRepo.findById(seed.getExCandidateId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CandidateStatus.PENDING);
    }

    @Test
    @DisplayName("DATE 변경 → 매핑 + 인접 candidate 모두 PENDING (horizon ±3일)")
    void date_change_replan_nearby() {
        // 같은 hose 의 3 candidate (deadline 2026-03-04, 3-05, 3-06)
        UUID vcRowIdA = UUID.randomUUID();
        UUID vcRowIdB = UUID.randomUUID();
        UUID vcRowIdC = UUID.randomUUID();
        ExScheduleCandidate cA = seedCandidate(vcRowIdA, "X", LocalDate.of(2026, 3, 4), CandidateStatus.READY);
        ExScheduleCandidate cB = seedCandidate(vcRowIdB, "X", LocalDate.of(2026, 3, 5), CandidateStatus.READY);
        ExScheduleCandidate cC = seedCandidate(vcRowIdC, "X", LocalDate.of(2026, 3, 6), CandidateStatus.READY);

        // vc row B 의 date 변경 (3/5 → 3/7) — 인접 ±3 일 (3/2 ~ 3/10) 모두 영향
        VcChangedEvent event = new VcChangedEvent(UUID.randomUUID(), Instant.now(), List.of(
            new VcChangedEvent.VcChangedRow(vcRowIdB, "X",
                LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 7),
                100, 100, VcChangedEvent.ChangeType.DATE)));

        List<UUID> impacted = finder.findImpacted(event);
        int triggered = replanService.triggerReplan(impacted);

        // A, B, C 모두 영향
        assertThat(impacted).contains(cA.getExCandidateId(), cB.getExCandidateId(), cC.getExCandidateId());
        assertThat(triggered).isEqualTo(3);
        assertThat(candidateRepo.findAll()).allMatch(c -> c.getStatus() == CandidateStatus.PENDING);
    }

    @Test
    @DisplayName("CONFIRMED candidate replan skip (Sprint 4 BR-V07 D-Day lock 정합)")
    void confirmed_candidate_replan_skip() {
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate seed = seedCandidate(vcRowId, "X", LocalDate.of(2026, 3, 5),
            CandidateStatus.CONFIRMED);

        VcChangedEvent event = new VcChangedEvent(UUID.randomUUID(), Instant.now(), List.of(
            new VcChangedEvent.VcChangedRow(vcRowId, "X", null, null,
                100, 200, VcChangedEvent.ChangeType.QUANTITY)));

        List<UUID> impacted = finder.findImpacted(event);
        int triggered = replanService.triggerReplan(impacted);

        assertThat(impacted).containsExactly(seed.getExCandidateId());
        assertThat(triggered).as("CONFIRMED 는 replan 제외").isZero();

        // CONFIRMED 유지
        ExScheduleCandidate after = candidateRepo.findById(seed.getExCandidateId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(CandidateStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Publisher.publishChanges — 변경 row 0건 → 이벤트 미발행 (no-op)")
    void publisher_no_op_for_empty_changes() {
        // 0건 publish — 이벤트 자체 미발행, ExScheduleCandidate 상태 변화 0
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate seed = seedCandidate(vcRowId, "X", LocalDate.of(2026, 3, 5),
            CandidateStatus.READY);

        publisher.publishChanges(UUID.randomUUID(), List.of());

        ExScheduleCandidate after = candidateRepo.findById(seed.getExCandidateId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(CandidateStatus.READY);
    }

    // 참고: Publisher → @ApplicationModuleListener 비동기 E2E 검증은
    // Sprint 4 EP-10 (Confirmed 상태 + VcScheduleService.override 실 호출) 완료 후
    // spring-modulith-events-jpa 인프라 활성 시 본격 검증. Sprint 3 단계에서는
    // Listener 직접 호출 (ImpactedRowFinder + PartialReplanService) 만 검증.
}
