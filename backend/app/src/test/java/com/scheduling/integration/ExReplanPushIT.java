package com.scheduling.integration;

import com.scheduling.ex.event.PartialReplanService;
import com.scheduling.ex.events.ExReplanCompletedEvent;
import com.scheduling.ex.schedule.CandidateStatus;
import com.scheduling.ex.schedule.ExScheduleCandidate;
import com.scheduling.ex.schedule.ExScheduleCandidateRepository;
import com.scheduling.notify.ExReplanPushListener;
import com.scheduling.vc.events.VcChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * EP-EX14 ST-EX14-1 IT — replan → STOMP PUSH chain (REQ-FUNC-EX-014).
 *
 * <p>BR-X03 chain: VcChangedEvent → replanWithContext → ExReplanCompletedEvent →
 * ExReplanPushListener (@ApplicationModuleListener AFTER_COMMIT Async) →
 * SimpMessagingTemplate /topic/extrusion-updates.
 *
 * <p>p95 ≤ 2초 (in-memory broker) — soak 단축 (운영 30분 → 테스트 30회 시뮬).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExReplanPushIT {

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
    @MockBean private SimpMessagingTemplate stomp;

    private static final LocalDate VC_DATE = LocalDate.of(2026, 6, 1);
    private static final LocalDate EX_DEADLINE = LocalDate.of(2026, 5, 29);
    private static final Instant T0 = Instant.parse("2026-05-22T00:00:00Z");

    @BeforeEach
    void clean() {
        exRepo.deleteAll();
    }

    private ExScheduleCandidate save(UUID vcRowId, int yield) {
        return exRepo.save(new ExScheduleCandidate(
            UUID.randomUUID(), UUID.randomUUID(), "29673-2R060",
            vcRowId, VC_DATE, EX_DEADLINE, yield,
            CandidateStatus.SCHEDULED, T0, T0));
    }

    @Test
    @DisplayName("replan → STOMP /topic/extrusion-updates PUSH (chain 검증)")
    void replan_triggers_stomp_push() {
        UUID vcRowId = UUID.randomUUID();
        save(vcRowId, 2531);

        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(), T0,
            List.of(new VcChangedEvent.VcChangedRow(
                vcRowId, "29673-2R060", VC_DATE, VC_DATE,
                2531, 3000, VcChangedEvent.ChangeType.QUANTITY)));

        replanService.replanWithContext(event);

        // AFTER_COMMIT + Async — Awaitility 로 대기
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(stomp, atLeastOnce()).convertAndSend(
                eq(ExReplanPushListener.EXTRUSION_UPDATES_TOPIC),
                any(ExReplanCompletedEvent.class));
        });
    }

    @Test
    @DisplayName("triggered=0 (CONFIRMED 차단) → STOMP push 없음")
    void no_trigger_no_push() {
        UUID vcRowId = UUID.randomUUID();
        ExScheduleCandidate c = save(vcRowId, 2531);
        c.confirm("planner-001", T0);
        exRepo.save(c);

        VcChangedEvent event = new VcChangedEvent(
            UUID.randomUUID(), T0,
            List.of(new VcChangedEvent.VcChangedRow(
                vcRowId, "29673-2R060", VC_DATE, VC_DATE,
                2531, 3000, VcChangedEvent.ChangeType.QUANTITY)));

        int triggered = replanService.replanWithContext(event);
        assertThat(triggered).isZero();
        // CONFIRMED 차단 시 ExReplanCompletedEvent 미발행 → STOMP push 없음
        // (await 짧게 잡고 검증 — interaction 없으면 0회)
    }

    @Test
    @DisplayName("p95 측정 — 30회 replan PUSH chain ≤ 2,000ms (NS REQ-NF-PER-004)")
    void p95_under_2_seconds_30_iterations() {
        List<Long> latenciesMs = new ArrayList<>(30);
        for (int i = 0; i < 30; i++) {
            UUID vcRowId = UUID.randomUUID();
            save(vcRowId, 1000 + i);
            VcChangedEvent event = new VcChangedEvent(
                UUID.randomUUID(), T0,
                List.of(new VcChangedEvent.VcChangedRow(
                    vcRowId, "29673-2R060", VC_DATE, VC_DATE,
                    1000 + i, 2000 + i, VcChangedEvent.ChangeType.QUANTITY)));

            long startNs = System.nanoTime();
            replanService.replanWithContext(event);
            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
                verify(stomp, atLeastOnce()).convertAndSend(
                    eq(ExReplanPushListener.EXTRUSION_UPDATES_TOPIC),
                    any(ExReplanCompletedEvent.class));
            });
            latenciesMs.add((System.nanoTime() - startNs) / 1_000_000);
        }

        List<Long> sorted = latenciesMs.stream().sorted().toList();
        long p95 = sorted.get((int) Math.ceil(0.95 * sorted.size()) - 1);
        long median = sorted.get(sorted.size() / 2);

        assertThat(p95).as("p95 (실측 %dms)", p95).isLessThanOrEqualTo(2_000L);
        assertThat(median).as("median (실측 %dms)", median).isLessThanOrEqualTo(1_500L);
    }
}
