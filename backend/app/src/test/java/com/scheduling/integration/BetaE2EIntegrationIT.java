package com.scheduling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.SlackNotifier;
import com.scheduling.order.diff.DiffType;
import com.scheduling.order.diff.OrderChangeEntity;
import com.scheduling.order.diff.OrderChangeRepository;
import com.scheduling.order.events.OrderCommittedEvent;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import com.scheduling.vc.events.MesDegradedModeChangedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 19 EP-BETA-LAUNCH ST-BETA-2 — 본 PC E2E 통합 시나리오 IT (TK-BETA-2-1·2·3).
 *
 * <p>Sprint 13~18 누적 자산을 단일 시나리오로 결합 — 베타 cutover 진입 직전 회귀 1건으로 전체 chain 보장.
 *
 * <p>시나리오:
 * <ol>
 *   <li>(Sprint 13) PLANNER1 수주 commit — POST /api/v1/orders/{trackingId}/commit
 *       → OrderCommittedEvent 발행 검증 (AFTER_COMMIT async)</li>
 *   <li>(Sprint 14/16/17) PLANNER1 작성 VcSchedule INSERT — createdBy=00000001 (BR-X05)</li>
 *   <li>(Sprint 16) PLANNER1 본인 작성 row confirm 시도 → 409 BR-X05 dual-review reject</li>
 *   <li>(Sprint 16) PLANNER2 (00000002) confirm → 200 + CONFIRMED</li>
 *   <li>(Sprint 18) MesDegradedModeChangedEvent 강제 publish
 *       → DegradedModePushListener → SlackNotifier.alert + STOMP convertAndSend 검증</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(BetaE2EIntegrationIT.EventCaptureConfig.class)
class BetaE2EIntegrationIT {

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
        registry.add("scheduling.notification.slack.enabled", () -> "false");
    }

    /** Order chain commit event 캡쳐 (Sprint 13 OrderDiffAndCommitIT 패턴 재사용). */
    static class CommittedEventCapture {
        final AtomicReference<OrderCommittedEvent> last = new AtomicReference<>();
        @EventListener
        void on(OrderCommittedEvent ev) { last.set(ev); }
    }

    @TestConfiguration
    static class EventCaptureConfig {
        @Bean CommittedEventCapture committedCapture() { return new CommittedEventCapture(); }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderChangeRepository orderChangeRepo;
    @Autowired private VcScheduleRepository vcRepo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CommittedEventCapture committedCapture;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private PlatformTransactionManager txManager;
    @MockitoSpyBean private SlackNotifier slackNotifier;
    @MockitoSpyBean private SimpMessagingTemplate stomp;

    private MockMvc mockMvc;

    private static final Instant T0 = Instant.parse("2026-05-28T00:00:00Z");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        orderChangeRepo.deleteAll();
        // E2E 시나리오 row 정리 (BR-X02 audit 은 정리 불필요 — append only)
        jdbc.update("DELETE FROM app.vc_schedule WHERE hose_id LIKE 'E2E-BETA-%'");
        committedCapture.last.set(null);
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000001")
    @DisplayName("Sprint 19 E2E — 수주 commit → VC INSERT → BR-X05 dual-review → MES degraded push")
    void e2e_beta_integration_scenario() throws Exception {
        UUID trackingId = UUID.randomUUID();

        // === Step 1 (Sprint 13) — PLANNER1 수주 commit ===
        seedOrderChange(trackingId, "E2E-BETA-001", "CRITICAL");

        mockMvc.perform(post("/api/v1/orders/" + trackingId + "/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Sprint 19 E2E 통합 검증\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.decidedBy").value("00000001"));

        // OrderCommittedEvent AFTER_COMMIT 비동기 발행 검증
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            OrderCommittedEvent ev = committedCapture.last.get();
            assertThat(ev).isNotNull();
            assertThat(ev.trackingId()).isEqualTo(trackingId);
            assertThat(ev.committedBy()).isEqualTo("00000001");
        });

        // === Step 2 (Sprint 14/16/17) — PLANNER1 작성 VcSchedule INSERT ===
        // Phase 5+ Allocator chain 미진행 — 직접 INSERT (Sprint 17 AllocationContext.requestedBy 정합)
        VcSchedule row = new VcSchedule(UUID.randomUUID(), "E2E-BETA-001", "LP-01",
            (short) 1, LocalDate.now().plusDays(5), (short) 5,
            "ANG-E2E", 100, VcScheduleStatus.CANDIDATE, "", T0, T0);
        row.assignCreatedBy("00000001");        // BR-X05 actor (PLANNER1)
        vcRepo.save(row);
        UUID scheduleId = row.getVcScheduleId();

        VcSchedule reloaded = vcRepo.findById(scheduleId).orElseThrow();
        assertThat(reloaded.getCreatedBy()).isEqualTo("00000001");
        assertThat(reloaded.getStatus()).isEqualTo(VcScheduleStatus.CANDIDATE);
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000001")
    @DisplayName("Sprint 19 E2E — PLANNER1 본인 작성 confirm → 409 BR-X05")
    void e2e_planner1_self_authored_rejected() throws Exception {
        VcSchedule row = saveCandidateAsActor("E2E-BETA-002", (short) 2, "00000001");

        mockMvc.perform(post("/api/v1/schedule/vc/" + row.getVcScheduleId() + "/confirm"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.brCode").value("BR-X05"));
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000002")
    @DisplayName("Sprint 19 E2E — PLANNER2 다른 actor confirm → 200 + CONFIRMED")
    void e2e_planner2_different_actor_confirms() throws Exception {
        VcSchedule row = saveCandidateAsActor("E2E-BETA-003", (short) 3, "00000001");

        mockMvc.perform(post("/api/v1/schedule/vc/" + row.getVcScheduleId() + "/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedBy").value("00000002"));
    }

    @Test
    @DisplayName("Sprint 19 E2E — MES degraded 전이 publish → Slack + STOMP 양쪽 push")
    void e2e_mes_degraded_push() {
        MesDegradedModeChangedEvent event = new MesDegradedModeChangedEvent(
            "LP-01", false, true, Instant.now());

        new TransactionTemplate(txManager).executeWithoutResult(status ->
            eventPublisher.publishEvent(event));

        verify(slackNotifier, timeout(5_000)).alert(
            eq(ChangeSeverity.CRITICAL),
            contains("MES degraded 진입"),
            contains("LP-01"));
        verify(stomp, timeout(5_000)).convertAndSend(
            eq("/topic/mes-degraded-updates"),
            any(Object.class));
    }

    // =========================================================================
    // Test helpers
    // =========================================================================

    private void seedOrderChange(UUID trackingId, String hoseId, String severity) {
        Instant now = Instant.now();
        orderChangeRepo.save(new OrderChangeEntity(
            UUID.randomUUID(), trackingId, DiffType.NEW, hoseId,
            LocalDate.now().plusDays(5),
            UUID.randomUUID(), null, "[]", 1, 2, severity, now));
    }

    private VcSchedule saveCandidateAsActor(String hoseId, short slot, String actor) {
        VcSchedule row = new VcSchedule(UUID.randomUUID(), hoseId, "LP-01",
            slot, LocalDate.now().plusDays(5), (short) 5,
            "ANG-E2E", 100, VcScheduleStatus.CANDIDATE, "", T0, T0);
        row.assignCreatedBy(actor);
        return vcRepo.save(row);
    }
}
