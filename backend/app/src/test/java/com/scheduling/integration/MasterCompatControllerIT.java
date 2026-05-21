package com.scheduling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.vc.SlotCompatibilityMatrixService;
import com.scheduling.master.vc.VcConstraint;
import com.scheduling.master.vc.VcConstraintRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EP-04 ST-04-1 TK-04-1-3 — MasterCompatController MockMvc IT.
 *
 * <p>Testcontainers PG + @WithMockUser. 검증:
 * <ul>
 *   <li>GET / — 200 + ETag "v{version}" + Cache-Control max-age=300 + JSON 매트릭스</li>
 *   <li>If-None-Match 동일 → 304 Not Modified + body 없음</li>
 *   <li>GET /{hose}/{slot} — PointCheck 응답</li>
 *   <li>잘못된 slotPosition → 400 한국어 사유</li>
 *   <li>인증 미부여 → 401</li>
 *   <li>VcConstraint UPDATE 후 LISTEN/NOTIFY → 새 version → 새 ETag</li>
 * </ul>
 */
@SpringBootTest    // webEnvironment 기본 MOCK — WebApplicationContext 필요 (MockMvc)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MasterCompatControllerIT {

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
    @Autowired private VcConstraintRepository repository;
    @Autowired private SlotCompatibilityMatrixService matrixService;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        repository.deleteAll();
        repository.save(new VcConstraint(
            "29673-2F900", 45, (short) 1, (short) 1, (short) 20,
            false, true, true, false,
            (short) 1, (short) 20, true, true, true,
            T0, "test-seed"
        ));
        // LISTEN/NOTIFY → matrix rebuild — 안정화 대기
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .until(() -> matrixService.current().byHose().containsKey("29673-2F900"));
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("GET / — 200 + ETag + Cache-Control + byHose JSON")
    void get_full_matrix_with_etag() throws Exception {
        int version = matrixService.current().version();

        MvcResult result = mockMvc.perform(get("/api/v1/master/compat"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ETAG, "\"v" + version + "\""))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("max-age=300")))
            .andExpect(jsonPath("$.version").value(version))
            .andExpect(jsonPath("$.byHose.['29673-2F900'].LP_UPMID").value(true))
            .andExpect(jsonPath("$.byHose.['29673-2F900'].LP_TOP").value(false))
            .andExpect(jsonPath("$.bySlot.LP_UPMID").isArray())
            .andReturn();

        assertThat(result.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("STK_USER — 인증 사용자 모두 허용 (READ_ONLY 포함)")
    void stk_user_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/master/compat"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("If-None-Match 동일 → 304 Not Modified + body 없음")
    void if_none_match_returns_304() throws Exception {
        int version = matrixService.current().version();
        String etag = "\"v" + version + "\"";

        MvcResult result = mockMvc.perform(get("/api/v1/master/compat")
                .header(HttpHeaders.IF_NONE_MATCH, etag))
            .andExpect(status().isNotModified())
            .andExpect(header().string(HttpHeaders.ETAG, etag))
            .andReturn();
        assertThat(result.getResponse().getContentLength()).isLessThanOrEqualTo(0);
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("GET /{hose}/{slot} PointCheck — eligible true/false")
    void point_check_endpoint() throws Exception {
        mockMvc.perform(get("/api/v1/master/compat/29673-2F900/LP_UPMID"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hoseId").value("29673-2F900"))
            .andExpect(jsonPath("$.slotPosition").value("LP_UPMID"))
            .andExpect(jsonPath("$.eligible").value(true));

        mockMvc.perform(get("/api/v1/master/compat/29673-2F900/LP_TOP"))
            .andExpect(jsonPath("$.eligible").value(false));
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("잘못된 slotPosition → 400 + 한국어 사유")
    void invalid_slot_returns_400() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/master/compat/29673-2F900/INVALID_XYZ"))
            .andExpect(status().isBadRequest())
            .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("잘못된 슬롯 위치").contains("INVALID_XYZ").contains("LP_TOP");
    }

    @Test
    @DisplayName("인증 미부여 → 401")
    void unauthenticated_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/master/compat"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("VcConstraint UPDATE → LISTEN/NOTIFY → 새 version + 새 ETag (≤ 15s)")
    void listen_notify_changes_etag() throws Exception {
        int versionBefore = matrixService.current().version();

        // UPDATE → trigger pg_notify → listener → invalidate
        repository.save(new VcConstraint(
            "29673-2F900", 45, (short) 1, (short) 1, (short) 20,
            true, true, true, true,   // LP_TOP false → true 로 변경
            (short) 1, (short) 20, true, true, true,
            T0.plusSeconds(60), "test-update"
        ));

        // Awaitility callback 은 별도 thread — SecurityContext 손실. 버전 변경만 대기.
        Awaitility.await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .until(() -> matrixService.current().version() > versionBefore);

        // 메인 thread (WithMockUser context 유지) 에서 endpoint 호출.
        int versionAfter = matrixService.current().version();
        MvcResult result = mockMvc.perform(get("/api/v1/master/compat"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ETAG, "\"v" + versionAfter + "\""))
            .andExpect(jsonPath("$.byHose.['29673-2F900'].LP_TOP").value(true))
            .andReturn();
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
            .get("version").asInt()).isEqualTo(versionAfter);
    }
}
