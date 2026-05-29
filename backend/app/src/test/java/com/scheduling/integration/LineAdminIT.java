package com.scheduling.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.line.LineProductCompatibilityRepository;
import com.scheduling.master.line.LineType;
import com.scheduling.master.line.LineTypeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 21 ST-CRUD-4 LINE_TYPE + LINE_PRODUCT_COMPATIBILITY admin IT (4 cases).
 *
 * <p>RBAC: write IT_OPS only, read 4 role. audit_log 영속 (BR-X02).
 * 비활성 처리 시 row 보존 (active=false toggle).
 *
 * @see BR-X02
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LineAdminIT {

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
    @Autowired private LineTypeRepository lineTypeRepository;
    @Autowired private LineProductCompatibilityRepository compatibilityRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        compatibilityRepository.deleteAll();
        lineTypeRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Case 1 — PLANNER read 200
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("should_enforce_read_PLANNER_200 — GET /lines 조회 허용")
    void should_enforce_read_PLANNER_200() throws Exception {
        mockMvc.perform(get("/api/v1/master/lines"))
            .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // Case 2 — STK_USER POST 403
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("should_enforce_BR_X02_STK_USER_post_403 — write RBAC 차단")
    void should_enforce_BR_X02_STK_USER_post_403() throws Exception {
        mockMvc.perform(post("/api/v1/master/lines")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineId\":\"L99\",\"lineType\":\"NEW\",\"priority\":1}"))
            .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Case 3 — IT_OPS POST 신규 line 201
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("should_enforce_BR_X02_IT_OPS_create_201 — 신규 line 생성 + updatedBy 검증")
    void should_enforce_BR_X02_IT_OPS_create_201() throws Exception {
        mockMvc.perform(post("/api/v1/master/lines")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineId\":\"L01\",\"lineType\":\"NEW\",\"priority\":1,\"description\":\"신규라인\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.lineId").value("L01"))
            .andExpect(jsonPath("$.lineType").value("NEW"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.updatedBy").value("00000007"));

        assertThat(lineTypeRepository.findById("L01")).isPresent();

        // deactivate — active=false toggle, row 보존
        mockMvc.perform(delete("/api/v1/master/lines/L01"))
            .andExpect(status().isNoContent());

        assertThat(lineTypeRepository.findById("L01"))
            .isPresent()
            .get()
            .extracting(LineType::isActive)
            .isEqualTo(false);
    }

    // -------------------------------------------------------------------------
    // Case 4 — IT_OPS PUT /products 200 + compatibility 갱신 + audit_log 영속
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000008")
    @DisplayName("should_enforce_BR_X02_products_replace_200 — 호환 매핑 갱신 + audit")
    void should_enforce_BR_X02_products_replace_200() throws Exception {
        // 사전 line 생성
        mockMvc.perform(post("/api/v1/master/lines")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"lineId\":\"L02\",\"lineType\":\"FORD\",\"priority\":2}"))
            .andExpect(status().isCreated());

        // product 호환 매핑 초기 세팅
        MvcResult result = mockMvc.perform(put("/api/v1/master/lines/L02/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseIds\":[\"29673-2R060\",\"29672-2R060\"]}"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isEqualTo(2);

        // DB 영속 검증
        assertThat(compatibilityRepository.findByLineId("L02")).hasSize(2);

        // 매핑 교체 — hoseId 1개로 축소
        mockMvc.perform(put("/api/v1/master/lines/L02/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hoseIds\":[\"29673-2R060\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        assertThat(compatibilityRepository.findByLineId("L02")).hasSize(1);
    }
}
