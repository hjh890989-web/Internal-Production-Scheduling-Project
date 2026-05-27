package com.scheduling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.order.diff.DiffType;
import com.scheduling.order.diff.OrderChangeEntity;
import com.scheduling.order.diff.OrderChangeRepository;
import com.scheduling.order.events.OrderCommittedEvent;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 13 EP-OC-FULL Diff + Commit Controller IT — 5 cases (TK-OC-6-1·2).
 *
 * <p>Diff 2 — GET summary 정확 (severity count) + 빈 trackingId 200 empty.
 * Commit 3 — PLANNER commit 200 + Event 발행 / STK_USER 403 / 미존재 404.
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import(OrderDiffAndCommitIT.EventCaptureConfig.class)
class OrderDiffAndCommitIT {

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
    @Autowired private OrderChangeRepository changeRepo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CommittedEventCapture eventCapture;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        changeRepo.deleteAll();
        eventCapture.last.set(null);
    }

    private UUID seed3RowsWithSeverity(UUID trackingId) {
        Instant now = Instant.now();
        changeRepo.save(new OrderChangeEntity(UUID.randomUUID(), trackingId,
            DiffType.NEW, "29673-2R060", LocalDate.of(2026, 6, 1),
            UUID.randomUUID(), null, "[]", 1, 2, "CRITICAL", now));
        changeRepo.save(new OrderChangeEntity(UUID.randomUUID(), trackingId,
            DiffType.MODIFIED, "29673-2R030", LocalDate.of(2026, 6, 2),
            UUID.randomUUID(), UUID.randomUUID(), "[]", 1, 2, "IMPORTANT", now));
        changeRepo.save(new OrderChangeEntity(UUID.randomUUID(), trackingId,
            DiffType.MODIFIED, "29673-2R040", LocalDate.of(2026, 6, 3),
            UUID.randomUUID(), UUID.randomUUID(), "[]", 1, 2, "STANDARD", now));
        return trackingId;
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000001")
    @DisplayName("Diff GET — 3 row + severity count (CRITICAL/IMPORTANT/STANDARD 각 1)")
    void diff_returns_severity_count() throws Exception {
        UUID tracking = seed3RowsWithSeverity(UUID.randomUUID());

        mockMvc.perform(get("/api/v1/orders/" + tracking + "/diff"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(3))
            .andExpect(jsonPath("$.criticalCount").value(1))
            .andExpect(jsonPath("$.importantCount").value(1))
            .andExpect(jsonPath("$.standardCount").value(1))
            .andExpect(jsonPath("$.unclassifiedCount").value(0))
            .andExpect(jsonPath("$.rows.length()").value(3));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("Diff GET — 빈 trackingId 200 empty (PARSED 만 진행 시나리오)")
    void diff_empty_returns_zero_summary() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID() + "/diff"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRows").value(0))
            .andExpect(jsonPath("$.rows.length()").value(0));
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000001")
    @DisplayName("Commit — PLANNER 200 + OrderCommittedEvent 발행 (AFTER_COMMIT 비동기)")
    void commit_planner_publishes_event() throws Exception {
        UUID tracking = seed3RowsWithSeverity(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders/" + tracking + "/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Sprint 13 검증\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trackingId").value(tracking.toString()))
            .andExpect(jsonPath("$.decidedBy").value("00000001"))
            .andExpect(jsonPath("$.affectedRows").value(3));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            OrderCommittedEvent ev = eventCapture.last.get();
            assertThat(ev).isNotNull();
            assertThat(ev.trackingId()).isEqualTo(tracking);
            assertThat(ev.committedBy()).isEqualTo("00000001");
            assertThat(ev.reason()).isEqualTo("Sprint 13 검증");
        });

        // BR-X02 — Sprint 13 hotfix AuditLogService 검증 (mutation-less endpoint audit)
        String actor = jdbc.queryForObject(
            "SELECT actor FROM audit.schedule_audit_log "
                + "WHERE table_name='order_change' AND row_pk=? "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class, tracking.toString());
        assertThat(actor).isEqualTo("00000001");

        String reason = jdbc.queryForObject(
            "SELECT reason FROM audit.schedule_audit_log "
                + "WHERE table_name='order_change' AND row_pk=? "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class, tracking.toString());
        assertThat(reason).contains("EP-OC-FULL 수주 import 확정").contains("Sprint 13 검증");
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("Commit — STK_USER 403 (PLANNER only, BR-X05)")
    void commit_stk_user_forbidden() throws Exception {
        UUID tracking = seed3RowsWithSeverity(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders/" + tracking + "/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"unauthorized\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("Commit — 미존재 trackingId 404 + event 미발행")
    void commit_missing_tracking_returns_404() throws Exception {
        mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"미존재 시도\"}"))
            .andExpect(status().isNotFound());

        // event 발행 안 됨 검증
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertThat(eventCapture.last.get()).isNull();
    }
}
