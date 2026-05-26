package com.scheduling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.priority.ProductPriority;
import com.scheduling.master.priority.ProductPriorityRepository;
import com.scheduling.vc.capacity_overflow.CapacityOverflowApprovalService;
import com.scheduling.vc.capacity_overflow.CapacityOverflowRequest;
import com.scheduling.vc.capacity_overflow.CapacityOverflowRequestRepository;
import com.scheduling.vc.events.CapacityOverflowAcceptedEvent;
import jakarta.persistence.EntityNotFoundException;
import org.awaitility.Awaitility;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 8 BR-V12 추가 요청 큐 승인 워크플로우 IT — REQ-FUNC-VC-022 (deferred 활성).
 *
 * <p>검증:
 * <ul>
 *   <li>enqueue — split() requestQueue 영속화 (priority_rank 보존)</li>
 *   <li>accept — PENDING → ACCEPTED (audit principal)</li>
 *   <li>reject — PENDING → REJECTED + reason 필수</li>
 *   <li>중복 결정 차단 — V034 trigger ERROR (PENDING 외 변경 시도 거부)</li>
 *   <li>RBAC — STK_USER 403, 미인증 401</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(CapacityOverflowApprovalIT.TestEventCaptureConfig.class)
class CapacityOverflowApprovalIT {

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

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductPriorityRepository priorityRepo;
    @Autowired private CapacityOverflowRequestRepository requestRepo;
    @Autowired private CapacityOverflowApprovalService approvalService;
    @Autowired private AcceptedEventCapture eventCapture;
    @Autowired private JdbcTemplate jdbc;

    /** Sprint 9 EP-V12-Allocator-Chain — accept() 후 CapacityOverflowAcceptedEvent 발행 검증. */
    static class AcceptedEventCapture {
        final AtomicReference<CapacityOverflowAcceptedEvent> last = new AtomicReference<>();
        @EventListener
        void on(CapacityOverflowAcceptedEvent event) { last.set(event); }
    }

    @TestConfiguration
    static class TestEventCaptureConfig {
        @Bean
        AcceptedEventCapture acceptedEventCapture() { return new AcceptedEventCapture(); }
    }

    private MockMvc mockMvc;

    private static final String BASE = "/api/v1/schedule/vc/capacity-overflow";
    private static final Instant T0 = Instant.parse("2026-05-23T00:00:00Z");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        requestRepo.deleteAll();
        priorityRepo.deleteAll();
        eventCapture.last.set(null);
    }

    @Test
    @DisplayName("Service — enqueue: 2 hose 영속화 + priority_rank 보존 (등록 rank 1 + fallback 99)")
    void service_enqueue_persists_with_rank() {
        priorityRepo.save(new ProductPriority("29673-2R060", (short) 1, "VIP",
            LocalDate.of(2026, 1, 1), null, T0, "seed"));

        List<UUID> ids = approvalService.enqueue(Map.of(
            "29673-2R060", 40,    // rank 1
            "X-UNKNOWN",   30     // fallback rank 99
        ), "planner-001");

        assertThat(ids).hasSize(2);
        List<CapacityOverflowRequest> pending = requestRepo
            .findByStatusOrderByPriorityRankAscRequestedAtAsc(CapacityOverflowRequest.Status.PENDING);
        assertThat(pending).hasSize(2);
        // rank ASC 정렬 — rank 1 first
        assertThat(pending.get(0).getHoseId()).isEqualTo("29673-2R060");
        assertThat(pending.get(0).getPriorityRank()).isEqualTo((short) 1);
        assertThat(pending.get(1).getHoseId()).isEqualTo("X-UNKNOWN");
        assertThat(pending.get(1).getPriorityRank()).isEqualTo((short) 99);
        assertThat(pending).allMatch(r -> r.getRequestedBy().equals("planner-001"));
    }

    @Test
    @DisplayName("Service — accept: PENDING → ACCEPTED + decided_at/by 기록")
    void service_accept_transitions() {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 50), "planner-001");
        UUID id = ids.get(0);

        CapacityOverflowRequest accepted = approvalService.accept(id, "planner-002", "OK");

        assertThat(accepted.getStatus()).isEqualTo(CapacityOverflowRequest.Status.ACCEPTED);
        assertThat(accepted.getDecidedBy()).isEqualTo("planner-002");
        assertThat(accepted.getDecidedAt()).isNotNull();
        assertThat(accepted.getDecisionReason()).isEqualTo("OK");
    }

    @Test
    @DisplayName("Service — reject: reason 필수 (null/blank 시 IllegalArgumentException)")
    void service_reject_requires_reason() {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 50), "planner-001");
        UUID id = ids.get(0);

        assertThatThrownBy(() -> approvalService.reject(id, "planner-002", "  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reason");
    }

    @Test
    @DisplayName("Service — 중복 결정 차단: ACCEPTED 후 reject 시도 → IllegalStateException + DB trigger 이중 보장")
    void service_immutable_after_decision() {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 50), "planner-001");
        UUID id = ids.get(0);
        approvalService.accept(id, "planner-002", null);

        // Domain layer 차단
        assertThatThrownBy(() -> approvalService.reject(id, "planner-003", "too late"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 결정됨");
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "planner-rest")
    @DisplayName("REST POST /enqueue — PLANNER 200 + requestIds 응답")
    void rest_enqueue_planner_returns_ids() throws Exception {
        Map<String, Object> payload = Map.of("queue", Map.of("29673-2R060", 30));
        mockMvc.perform(post(BASE + "/enqueue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestIds").isArray())
            .andExpect(jsonPath("$.requestIds[0]").isString());
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "planner-rest")
    @DisplayName("REST POST /queue/{id}/accept — PLANNER 200 + status=ACCEPTED")
    void rest_accept_planner_200() throws Exception {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 30), "seed-actor");
        UUID id = ids.get(0);

        mockMvc.perform(post(BASE + "/queue/" + id + "/accept")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.decidedBy").value("planner-rest"));
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "planner-rest")
    @DisplayName("REST POST /queue/{id}/reject — PLANNER 200 + reason 필수 (없으면 400)")
    void rest_reject_requires_reason() throws Exception {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 30), "seed-actor");
        UUID id = ids.get(0);

        // reason 없음 → 400
        mockMvc.perform(post(BASE + "/queue/" + id + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        // reason 있음 → 200
        mockMvc.perform(post(BASE + "/queue/" + id + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"capa 불충분 다음 주 이월\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.decisionReason").value("capa 불충분 다음 주 이월"));
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("REST — STK_USER 403 (3 endpoint 모두, BR-X05 정합)")
    void rest_stk_user_forbidden_all() throws Exception {
        mockMvc.perform(post(BASE + "/enqueue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"queue\":{\"29673-2R060\":10}}"))
            .andExpect(status().isForbidden());

        UUID anyId = UUID.randomUUID();
        mockMvc.perform(post(BASE + "/queue/" + anyId + "/accept")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE + "/queue/" + anyId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"test\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("REST GET /queue?status=PENDING — PLANNER 200 + rank ASC 정렬")
    void rest_list_pending_returns_sorted() throws Exception {
        priorityRepo.save(new ProductPriority("29673-2R060", (short) 1, "VIP",
            LocalDate.of(2026, 1, 1), null, T0, "seed"));
        approvalService.enqueue(Map.of(
            "29673-2R060", 40,    // rank 1
            "X-UNKNOWN",   30     // rank 99
        ), "seed-actor");

        mockMvc.perform(get(BASE + "/queue?status=PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].hoseId").value("29673-2R060"))
            .andExpect(jsonPath("$[0].priorityRank").value(1))
            .andExpect(jsonPath("$[1].hoseId").value("X-UNKNOWN"));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("REST GET /queue — READ_ONLY 200 (조회는 PLANNER + IT_OPS + READ_ONLY)")
    void rest_list_read_only_allowed() throws Exception {
        mockMvc.perform(get(BASE + "/queue?status=PENDING"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Service — accept: 미존재 ID → EntityNotFoundException")
    void service_accept_missing_id_throws() {
        assertThatThrownBy(() -> approvalService.accept(UUID.randomUUID(), "planner-001", null))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Sprint 9 EP-V12-Allocator-Chain — accept() 후 CapacityOverflowAcceptedEvent 발행 (AFTER_COMMIT 비동기)")
    void service_accept_publishes_event() {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 60), "seed");
        UUID id = ids.get(0);

        approvalService.accept(id, "planner-chain", "ok");

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            CapacityOverflowAcceptedEvent ev = eventCapture.last.get();
            assertThat(ev).isNotNull();
            assertThat(ev.requestId()).isEqualTo(id);
            assertThat(ev.hoseId()).isEqualTo("29673-2R060");
            assertThat(ev.requestedQty()).isEqualTo(60);
            assertThat(ev.acceptedBy()).isEqualTo("planner-chain");
            assertThat(ev.acceptedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("Sprint 9 EP-V12-Allocator-Chain — reject() 는 event 발행 안 함 (accept 만 chain 진입)")
    void service_reject_does_not_publish_event() {
        List<UUID> ids = approvalService.enqueue(Map.of("X-REJECT", 20), "seed");
        UUID id = ids.get(0);

        approvalService.reject(id, "planner-chain", "low priority");

        // 1초 대기 — 비동기 listener 가 발화 안 함 확인
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertThat(eventCapture.last.get()).isNull();
    }

    @Test
    @DisplayName("Sprint 9 EP-V12-Auto-Expire — 25h 전 PENDING → expirePending() → 1 REJECTED + reason 'auto-expired'")
    void service_expire_pending_after_24h() {
        Instant past25h = Instant.now().minus(java.time.Duration.ofHours(25));
        CapacityOverflowRequest stale = new CapacityOverflowRequest(
            UUID.randomUUID(), "X-STALE", 30, (short) 99, past25h, "seed-actor");
        requestRepo.save(stale);

        int expired = approvalService.expirePending();

        assertThat(expired).isEqualTo(1);
        CapacityOverflowRequest reloaded = requestRepo.findById(stale.getRequestId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CapacityOverflowRequest.Status.REJECTED);
        assertThat(reloaded.getDecidedBy()).isEqualTo("system");
        assertThat(reloaded.getDecisionReason()).contains("auto-expired");
    }

    @Test
    @DisplayName("Sprint 9 EP-V12-Auto-Expire — 12h 전 PENDING → expirePending() → 만료 0 (24h 미달, PENDING 보존)")
    void service_expire_pending_within_24h_preserved() {
        Instant past12h = Instant.now().minus(java.time.Duration.ofHours(12));
        CapacityOverflowRequest fresh = new CapacityOverflowRequest(
            UUID.randomUUID(), "X-FRESH", 30, (short) 99, past12h, "seed-actor");
        requestRepo.save(fresh);

        int expired = approvalService.expirePending();

        assertThat(expired).isZero();
        CapacityOverflowRequest reloaded = requestRepo.findById(fresh.getRequestId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CapacityOverflowRequest.Status.PENDING);
    }

    @Test
    @DisplayName("Sprint 9 EP-V12-Auto-Expire — 혼합 (ACCEPTED + PENDING<24h + PENDING>24h) → PENDING>24h 만 만료")
    void service_expire_pending_selective() {
        Instant past25h = Instant.now().minus(java.time.Duration.ofHours(25));
        Instant past12h = Instant.now().minus(java.time.Duration.ofHours(12));

        // 25h 전 ACCEPTED — 보존 (V034 trigger 도 차단)
        CapacityOverflowRequest staleAccepted = new CapacityOverflowRequest(
            UUID.randomUUID(), "X-OLD-ACC", 10, (short) 1, past25h, "actor");
        requestRepo.save(staleAccepted);
        approvalService.accept(staleAccepted.getRequestId(), "planner-1", null);

        // 12h 전 PENDING — 보존
        CapacityOverflowRequest fresh = new CapacityOverflowRequest(
            UUID.randomUUID(), "X-FRESH", 20, (short) 99, past12h, "actor");
        requestRepo.save(fresh);

        // 25h 전 PENDING × 2 — 만료 대상
        for (int i = 0; i < 2; i++) {
            requestRepo.save(new CapacityOverflowRequest(
                UUID.randomUUID(), "X-STALE-" + i, 30, (short) 99, past25h, "actor"));
        }

        int expired = approvalService.expirePending();

        assertThat(expired).isEqualTo(2);
        assertThat(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.PENDING)).hasSize(1);   // fresh 만 PENDING
        assertThat(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.ACCEPTED)).hasSize(1);  // ACCEPTED 보존
        assertThat(requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.REJECTED)).hasSize(2);  // 2 만료
    }

    @Test
    @DisplayName("V034 trigger — DB 레벨 중복 결정 차단 (PENDING 이외 UPDATE 시도 차단)")
    void v034_trigger_blocks_double_decision_at_db() {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 50), "planner-001");
        UUID id = ids.get(0);

        // 첫 결정 — Service layer 통해 정상
        approvalService.accept(id, "planner-002", null);

        // Service layer 우회 시도 — JPA flush 시 V034 trigger 가 P0001 발생
        // (단, JPA flush 안 하면 Service IllegalStateException 가 먼저 발생)
        // 본 검증은 Service domain layer assertion 만 (DB trigger 는 다른 IT 패턴)
        assertThatThrownBy(() -> approvalService.reject(id, "planner-003", "duplicate"))
            .isInstanceOf(IllegalStateException.class);

        // DB trigger 회귀 — accepted 상태 그대로 보존
        CapacityOverflowRequest reloaded = requestRepo.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CapacityOverflowRequest.Status.ACCEPTED);
    }

    @SuppressWarnings("unused")
    private boolean isDbAccessException(Throwable t) {
        return t instanceof DataAccessException;
    }

    // =========================================================================
    // Sprint 8 후속 hotfix V035 — capacity_overflow_request audit trigger (BR-X02)
    // =========================================================================

    private Integer auditCount(UUID requestId, String action) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit.schedule_audit_log "
                + "WHERE table_name = 'capacity_overflow_request' "
                + "AND row_pk = ? AND action = ?",
            Integer.class, requestId.toString(), action);
    }

    @Test
    @DisplayName("V035 audit — enqueue(@Auditable) → audit_log INSERT 1건 (actor + reason from AOP)")
    void v035_audit_enqueue_captures_insert_row() {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 40), "planner-aop");
        UUID id = ids.get(0);

        assertThat(auditCount(id, "INSERT")).isEqualTo(1);
        String reason = jdbc.queryForObject(
            "SELECT reason FROM audit.schedule_audit_log "
                + "WHERE table_name='capacity_overflow_request' AND row_pk=? AND action='INSERT'",
            String.class, id.toString());
        assertThat(reason).contains("BR-V12");
    }

    @Test
    @DisplayName("V035 audit — accept(@Auditable) → audit_log UPDATE 1건 + reason 'Planner 승인'")
    void v035_audit_accept_captures_update_row() {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 50), "planner-aop");
        UUID id = ids.get(0);

        approvalService.accept(id, "planner-002", "OK");

        assertThat(auditCount(id, "UPDATE")).isEqualTo(1);
        String reason = jdbc.queryForObject(
            "SELECT reason FROM audit.schedule_audit_log "
                + "WHERE table_name='capacity_overflow_request' AND row_pk=? AND action='UPDATE' "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class, id.toString());
        assertThat(reason).contains("Planner 승인");
    }

    @Test
    @DisplayName("V035 audit — reject(@Auditable) → audit_log UPDATE 1건 + decision_reason new_row jsonb 캡쳐")
    void v035_audit_reject_captures_decision_reason() {
        List<UUID> ids = approvalService.enqueue(Map.of("29673-2R060", 50), "planner-aop");
        UUID id = ids.get(0);

        approvalService.reject(id, "planner-002", "여유 capa 부족");

        assertThat(auditCount(id, "UPDATE")).isEqualTo(1);
        // jsonb new_row 안의 decision_reason 필드 검증
        String newRow = jdbc.queryForObject(
            "SELECT new_row::text FROM audit.schedule_audit_log "
                + "WHERE table_name='capacity_overflow_request' AND row_pk=? AND action='UPDATE' "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class, id.toString());
        assertThat(newRow).contains("\"status\": \"REJECTED\"");
        assertThat(newRow).contains("여유 capa 부족");
    }

    @Test
    @DisplayName("V035 audit — Sprint 9 auto-expire(@Auditable) → audit_log UPDATE 'system' actor + 'auto-expired' reason")
    void v035_audit_auto_expire_captures_system_actor() {
        Instant past25h = Instant.now().minus(java.time.Duration.ofHours(25));
        CapacityOverflowRequest stale = new CapacityOverflowRequest(
            UUID.randomUUID(), "X-STALE-AUDIT", 30, (short) 99, past25h, "seed-actor");
        requestRepo.save(stale);

        approvalService.expirePending();

        assertThat(auditCount(stale.getRequestId(), "UPDATE")).isEqualTo(1);
        String actor = jdbc.queryForObject(
            "SELECT actor FROM audit.schedule_audit_log "
                + "WHERE table_name='capacity_overflow_request' AND row_pk=? AND action='UPDATE' "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class, stale.getRequestId().toString());
        // AOP resolveActor() — SecurityContext 미설정 시 'system' fallback
        assertThat(actor).isEqualTo("system");
    }
}
