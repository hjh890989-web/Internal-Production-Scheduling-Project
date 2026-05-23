package com.scheduling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.kd.KdOrder;
import com.scheduling.master.kd.KdOrderRepository;
import com.scheduling.master.priority.ProductPriority;
import com.scheduling.master.priority.ProductPriorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 7 v1.1 — BR-V12·V13 REST layer IT (Service IT 는 BrV12V13IT).
 *
 * <p>검증:
 * <ul>
 *   <li>POST /split — PLANNER 200 + accepted/requestQueue JSON</li>
 *   <li>POST /supplement — PLANNER 200 + supplemented JSON</li>
 *   <li>RBAC — STK_USER 403, 미인증 401 (BR-X05 작성자 ≠ 승인자 위배 방지)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql("classpath:datasets/DS-VC-CONSTRAINT-47/master_seed.sql")
class CapacityOverflowControllerIT {

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
    @Autowired private KdOrderRepository kdRepo;

    private MockMvc mockMvc;

    private static final String BASE = "/api/v1/schedule/vc/capacity-overflow";
    private static final Instant T0 = Instant.parse("2026-05-23T00:00:00Z");
    private static final LocalDate D = LocalDate.of(2026, 4, 1);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        priorityRepo.deleteAll();
        kdRepo.deleteAll();
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("POST /split — PLANNER 200 + accepted/requestQueue JSON 응답")
    void split_planner_returns_200() throws Exception {
        Map<String, Object> payload = Map.of(
            "required", Map.of("29673-2R060", 60, "28422-2M800", 50),
            "dailyCapa", 100
        );
        mockMvc.perform(post(BASE + "/split")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalAccepted").isNumber())
            .andExpect(jsonPath("$.totalQueued").isNumber())
            .andExpect(jsonPath("$.accepted").isMap())
            .andExpect(jsonPath("$.requestQueue").isMap());
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("POST /split — STK_USER 403 (BR-X05 작성자 ≠ 승인자)")
    void split_stk_user_forbidden() throws Exception {
        Map<String, Object> payload = Map.of(
            "required", Map.of("29673-2R060", 60),
            "dailyCapa", 100
        );
        mockMvc.perform(post(BASE + "/split")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /split — 미인증 401")
    void split_unauthenticated_returns_401() throws Exception {
        Map<String, Object> payload = Map.of(
            "required", Map.of("29673-2R060", 60),
            "dailyCapa", 100
        );
        mockMvc.perform(post(BASE + "/split")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "planner-it-001")
    @DisplayName("POST /supplement — PLANNER 200 + 실제 KD 차감 (audit principal)")
    void supplement_planner_returns_200() throws Exception {
        kdRepo.save(new KdOrder(UUID.randomUUID(), "29673-2R060",
            100, 100, D, "CUST-A", KdOrder.Status.OPEN, T0, "seed"));

        Map<String, Object> payload = Map.of(
            "hoseId", "29673-2R060",
            "shortage", 80
        );
        mockMvc.perform(post(BASE + "/supplement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hoseId").value("29673-2R060"))
            .andExpect(jsonPath("$.shortage").value(80))
            .andExpect(jsonPath("$.supplemented").value(80))
            .andExpect(jsonPath("$.consumed").isArray())
            .andExpect(jsonPath("$.consumed[0].qty").value(80));
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("POST /split — invalid dailyCapa (@Min 1) → 400")
    void split_invalid_daily_capa_returns_400() throws Exception {
        Map<String, Object> payload = Map.of(
            "required", Map.of("29673-2R060", 60),
            "dailyCapa", 0    // @Min(1) 위반
        );
        mockMvc.perform(post(BASE + "/split")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("POST /supplement — invalid shortage (@Min 1) → 400")
    void supplement_invalid_shortage_returns_400() throws Exception {
        Map<String, Object> payload = Map.of(
            "hoseId", "29673-2R060",
            "shortage", 0    // @Min(1) 위반
        );
        mockMvc.perform(post(BASE + "/supplement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("POST /supplement — READ_ONLY 403")
    void supplement_read_only_forbidden() throws Exception {
        Map<String, Object> payload = Map.of(
            "hoseId", "29673-2R060",
            "shortage", 50
        );
        mockMvc.perform(post(BASE + "/supplement")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
    }

    @SuppressWarnings("unused")
    private void seedPriority(String hose, int rank) {
        priorityRepo.save(new ProductPriority(hose, (short) rank,
            "test", D, null, T0, "seed"));
    }
}
