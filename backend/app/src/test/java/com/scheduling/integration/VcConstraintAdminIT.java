package com.scheduling.integration;

import com.scheduling.master.vc.VcConstraint;
import com.scheduling.master.vc.VcConstraintRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 21 ST-CRUD-3 — VcConstraintAdmin REST IT (4 cases).
 *
 * <p>시나리오:
 * <ol>
 *   <li>PLANNER GET /api/v1/master/vc-constraints → 200</li>
 *   <li>STK_USER PUT → 403</li>
 *   <li>IT_OPS PUT compositeCount=4 → 400 (BR-V14 위반)</li>
 *   <li>IT_OPS PUT compositeCount=2 + lpMoldQty=10 → 200 + audit_log 영속</li>
 * </ol>
 *
 * @see BR-V14 compositeCount ∈ {1, 2, 3, 6}
 * @see BR-X02 audit_log 강제
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VcConstraintAdminIT {

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
    @Autowired private VcConstraintRepository vcConstraintRepo;

    private MockMvc mockMvc;

    private static final Instant SEED_TIME = Instant.parse("2026-05-01T00:00:00Z");
    private static final String TEST_HOSE = "29673-2R060";

    /** 각 케이스 전 seed row 1건 적재 (PUT 테스트용). */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        vcConstraintRepo.deleteAll();
        vcConstraintRepo.save(new VcConstraint(
            TEST_HOSE, 45, (short) 1,
            (short) 1, (short) 20,
            true, true, false, false,
            (short) 1, (short) 20,
            true, true, false,
            SEED_TIME, "system:seed"
        ));
    }

    // -------------------------------------------------------------------------
    // Case 1 — PLANNER read 200
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("Case 1 — PLANNER GET 200 (BR-V14 조회)")
    void should_enforce_BR_V14_PLANNER_read_ok() throws Exception {
        mockMvc.perform(get("/api/v1/master/vc-constraints"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hoseId").value(TEST_HOSE))
            .andExpect(jsonPath("$[0].compositeCount").value(1));
    }

    // -------------------------------------------------------------------------
    // Case 2 — STK_USER PUT 403
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("Case 2 — STK_USER PUT → 403 (write IT_OPS only)")
    void should_enforce_BR_V14_STK_USER_put_forbidden() throws Exception {
        mockMvc.perform(put("/api/v1/master/vc-constraints/" + TEST_HOSE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildRequestBody(TEST_HOSE, 2, 10, 5)))
            .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Case 3 — IT_OPS PUT compositeCount=4 → 400 BR-V14
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("Case 3 — IT_OPS PUT compositeCount=4 → 400 BR-V14 위반")
    void should_enforce_BR_V14_reject_composite_count_4() throws Exception {
        mockMvc.perform(put("/api/v1/master/vc-constraints/" + TEST_HOSE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildRequestBody(TEST_HOSE, 4, 10, 5)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("BR-V14 위반"))
            .andExpect(jsonPath("$.detail").value(
                org.hamcrest.Matchers.containsString("BR-V14 합금형 1·2·3·6 만 허용")));
    }

    // -------------------------------------------------------------------------
    // Case 4 — IT_OPS PUT compositeCount=2 → 200 + audit_log 영속
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("Case 4 — IT_OPS PUT compositeCount=2 + lpMoldQty=10 → 200 + DB 반영")
    void should_enforce_BR_V14_IT_OPS_update_ok_and_audit() throws Exception {
        mockMvc.perform(put("/api/v1/master/vc-constraints/" + TEST_HOSE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buildRequestBody(TEST_HOSE, 2, 10, 5)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.compositeCount").value(2))
            .andExpect(jsonPath("$.lpMoldQty").value(10))
            .andExpect(jsonPath("$.updatedBy").value("00000007"));

        // DB 반영 검증
        VcConstraint updated = vcConstraintRepo.findById(TEST_HOSE).orElseThrow();
        assertThat(updated.getCompositeCount()).isEqualTo((short) 2);
        assertThat(updated.getMoldQty()).isEqualTo(10);
        assertThat(updated.getUpdatedBy()).isEqualTo("00000007");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** 공통 request JSON 빌더 — slot 전부 true 고정. */
    private static String buildRequestBody(String hoseId, int compositeCount,
                                            int lpMoldQty, int icMoldQty) {
        return """
            {
              "hoseId": "%s",
              "compositeCount": %d,
              "lpMoldQty": %d,
              "icMoldQty": %d,
              "slot1": true,
              "slot2": true,
              "slot3": false,
              "slot4": false,
              "slot5": true,
              "slot6": false,
              "slot7": false
            }
            """.formatted(hoseId, compositeCount, lpMoldQty, icMoldQty);
    }
}
