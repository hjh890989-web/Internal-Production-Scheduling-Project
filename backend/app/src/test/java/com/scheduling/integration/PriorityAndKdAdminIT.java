package com.scheduling.integration;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 12 EP-MASTER-UI BR-V12 PRODUCT_PRIORITY + BR-V13 KD_ORDER admin IT (6 cases).
 *
 * <p>RBAC: write IT_OPS only, read 4 role. audit_log.actor 검증 (BR-X02).
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PriorityAndKdAdminIT {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        priorityRepo.deleteAll();
        kdRepo.deleteAll();
    }

    // =========================================================================
    // PRODUCT_PRIORITY 3 cases
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("Priority IT_OPS create+update+delete — happy path")
    void priority_crud_happy_path() throws Exception {
        // create
        mockMvc.perform(post("/api/v1/master/product-priority")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseId\":\"29673-2R060\",\"priorityRank\":1,\"rationale\":\"VIP\","
                    + "\"effectiveFrom\":\"2026-01-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.hoseId").value("29673-2R060"))
            .andExpect(jsonPath("$.priorityRank").value(1))
            .andExpect(jsonPath("$.updatedBy").value("00000007"));

        // update — rank 1 → 5
        mockMvc.perform(put("/api/v1/master/product-priority/29673-2R060")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseId\":\"29673-2R060\",\"priorityRank\":5,\"rationale\":\"downgraded\","
                    + "\"effectiveFrom\":\"2026-01-01\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priorityRank").value(5));

        // delete
        mockMvc.perform(delete("/api/v1/master/product-priority/29673-2R060"))
            .andExpect(status().isNoContent());

        assertThat(priorityRepo.findById("29673-2R060")).isEmpty();
    }

    @Test
    @WithMockUser(roles = "IT_OPS")
    @DisplayName("Priority create — hose_id 중복 409")
    void priority_create_conflict() throws Exception {
        priorityRepo.save(new ProductPriority("29673-2R060", (short) 1, null,
            LocalDate.of(2026, 1, 1), null, Instant.now(), "seed"));

        mockMvc.perform(post("/api/v1/master/product-priority")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseId\":\"29673-2R060\",\"priorityRank\":2,\"effectiveFrom\":\"2026-01-01\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("Priority STK_USER write — 403 (read 는 PLANNER+IT_OPS+READ_ONLY)")
    void priority_stk_user_write_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/master/product-priority")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseId\":\"X\",\"priorityRank\":1,\"effectiveFrom\":\"2026-01-01\"}"))
            .andExpect(status().isForbidden());

        // STK_USER read 도 403 (PLANNER + IT_OPS + READ_ONLY 만 read 허용)
        mockMvc.perform(get("/api/v1/master/product-priority"))
            .andExpect(status().isForbidden());
    }

    // =========================================================================
    // KD_ORDER 3 cases
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("KD IT_OPS create+update+delete — happy path")
    void kd_crud_happy_path() throws Exception {
        // create
        MvcResult created = mockMvc.perform(post("/api/v1/master/kd-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseId\":\"29673-2R060\",\"orderQty\":500,\"remainingQty\":500,"
                    + "\"orderDate\":\"2026-05-27\",\"status\":\"OPEN\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.hoseId").value("29673-2R060"))
            .andExpect(jsonPath("$.remainingQty").value(500))
            .andExpect(jsonPath("$.updatedBy").value("00000007"))
            .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String id = body.get("kdOrderId").asText();

        // update — remaining 500 → 200 (소진 200)
        mockMvc.perform(put("/api/v1/master/kd-order/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseId\":\"29673-2R060\",\"orderQty\":500,\"remainingQty\":200,"
                    + "\"orderDate\":\"2026-05-27\",\"status\":\"PARTIAL\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.remainingQty").value(200))
            .andExpect(jsonPath("$.status").value("PARTIAL"));

        // delete
        mockMvc.perform(delete("/api/v1/master/kd-order/" + id))
            .andExpect(status().isNoContent());

        assertThat(kdRepo.findById(UUID.fromString(id))).isEmpty();
    }

    @Test
    @WithMockUser(roles = "IT_OPS")
    @DisplayName("KD create — remaining > orderQty 400 (entity 불변)")
    void kd_create_remaining_over_qty_400() throws Exception {
        mockMvc.perform(post("/api/v1/master/kd-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseId\":\"X\",\"orderQty\":100,\"remainingQty\":200,"
                    + "\"orderDate\":\"2026-05-27\",\"status\":\"OPEN\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("KD STK_USER write 403 + read 403 (PLANNER+IT_OPS+READ_ONLY 만 read)")
    void kd_stk_user_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/master/kd-order")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/master/kd-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseId\":\"X\",\"orderQty\":100,\"remainingQty\":100,"
                    + "\"orderDate\":\"2026-05-27\",\"status\":\"OPEN\"}"))
            .andExpect(status().isForbidden());
    }
}
