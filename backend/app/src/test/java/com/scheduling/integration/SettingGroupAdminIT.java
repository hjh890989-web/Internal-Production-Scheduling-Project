package com.scheduling.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.setting.SettingGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 21 ST-CRUD-2 — SETTING_GROUP admin IT (4 cases).
 *
 * <p>RBAC: write IT_OPS only, read 4 role.
 * setting_group_id 범위 1~8 위반 400. audit_log.actor 검증 (BR-X02).
 *
 * @see com.scheduling.master.setting.SettingGroupAdminController
 * @see com.scheduling.master.setting.SettingGroupAdminService
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SettingGroupAdminIT {

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
    @Autowired private SettingGroupRepository settingGroupRepository;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // =========================================================================
    // IT-1 PLANNER read 200
    // =========================================================================

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("IT-1 PLANNER GET /api/v1/master/setting-groups — 200 + 8 rows (seed)")
    void should_allow_PLANNER_to_read_setting_groups() throws Exception {
        mockMvc.perform(get("/api/v1/master/setting-groups"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(8));
    }

    // =========================================================================
    // IT-2 STK_USER POST 403
    // =========================================================================

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("IT-2 STK_USER POST setting-groups — 403 (IT_OPS only write)")
    void should_forbid_STK_USER_from_creating_setting_group() throws Exception {
        mockMvc.perform(post("/api/v1/master/setting-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupNumber\":1,\"groupName\":\"TEST\",\"active\":true}"))
            .andExpect(status().isForbidden());
    }

    // =========================================================================
    // IT-3 IT_OPS POST groupNumber=9 → 400 (범위 위반 BR-V12·V13)
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS")
    @DisplayName("IT-3 IT_OPS POST groupNumber=9 — 400 (setting_group_id 범위 위반 BR-V12·V13)")
    void should_enforce_BR_V12_V13_group_number_range_on_create() throws Exception {
        mockMvc.perform(post("/api/v1/master/setting-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupNumber\":9,\"groupName\":\"INVALID\",\"active\":true}"))
            .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // IT-4 IT_OPS PUT display_name 변경 → 200 + audit_log 영속 검증 (BR-X02)
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("IT-4 IT_OPS PUT groupNumber=1 display_name 변경 — 200 + audit_log.actor 영속 (BR-X02)")
    void should_update_display_name_and_persist_audit_log() throws Exception {
        // PUT groupNumber=1 display_name 변경
        MvcResult result = mockMvc.perform(put("/api/v1/master/setting-groups/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"groupName\":\"G1-소형-Updated\",\"active\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groupNumber").value(1))
            .andExpect(jsonPath("$.groupName").value("G1-소형-Updated"))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("updatedBy").asText()).isEqualTo("00000007");

        // BR-X02 — audit_log.actor 영속 검증
        String actor = jdbc.queryForObject(
            "SELECT actor FROM audit.schedule_audit_log "
                + "WHERE table_name='setting_group' AND row_pk='1' "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class);
        assertThat(actor).isEqualTo("00000007");
    }
}
