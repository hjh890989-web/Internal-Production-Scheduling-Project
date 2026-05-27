package com.scheduling.integration;

import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 16 EP-CONFIRM HTTP IT — TK-CONFIRM-5-2 응답 매핑 검증.
 *
 * <p>VcConfirmExceptionHandler 의 RFC 7807 ProblemDetail 매핑:
 * <ul>
 *   <li>BR-X05 dual-review → 409 Conflict (createdBy == plannerId)</li>
 *   <li>BR-X01 already-confirmed → 409 Conflict (CONFIRMED 재확정)</li>
 *   <li>STK_USER POST /confirm → 403 (RBAC)</li>
 *   <li>정상 PLANNER 확정 → 200 + Body</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VcConfirmControllerHttpIT {

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
    @Autowired private VcScheduleRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    private static final Instant T0 = Instant.parse("2026-05-27T00:00:00Z");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("DELETE FROM app.vc_schedule");
    }

    private VcSchedule saveCandidate(String createdBy, short slot) {
        VcSchedule s = new VcSchedule(
            UUID.randomUUID(), "29673-2R060", "LP-01",
            slot, LocalDate.now().plusDays(5), (short) 5,
            "ANG-X07", 100, VcScheduleStatus.CANDIDATE,
            "", T0, T0);
        if (createdBy != null) s.assignCreatedBy(createdBy);
        return repository.save(s);
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000002")
    @DisplayName("PLANNER 다른 actor 확정 — 200 OK + CONFIRMED")
    void planner_confirm_success_200() throws Exception {
        VcSchedule s = saveCandidate("00000001", (short) 1);

        mockMvc.perform(post("/api/v1/schedule/vc/" + s.getVcScheduleId() + "/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedBy").value("00000002"));
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000001")
    @DisplayName("BR-X05 — createdBy == plannerId 시 409 Conflict + brCode=BR-X05")
    void same_actor_confirm_409() throws Exception {
        VcSchedule s = saveCandidate("00000001", (short) 2);

        mockMvc.perform(post("/api/v1/schedule/vc/" + s.getVcScheduleId() + "/confirm"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.brCode").value("BR-X05"))
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("BR-X05 dual-review")));
    }

    @Test
    @WithMockUser(roles = "STK_USER", username = "00000005")
    @DisplayName("RBAC — STK_USER POST /confirm 403 Forbidden")
    void stk_user_confirm_forbidden() throws Exception {
        VcSchedule s = saveCandidate("00000001", (short) 3);

        mockMvc.perform(post("/api/v1/schedule/vc/" + s.getVcScheduleId() + "/confirm"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000002")
    @DisplayName("BR-X01 — 이미 CONFIRMED row 재확정 409 Conflict")
    void already_confirmed_409() throws Exception {
        VcSchedule s = saveCandidate("00000001", (short) 4);
        // 첫 confirm 으로 CONFIRMED 전이
        mockMvc.perform(post("/api/v1/schedule/vc/" + s.getVcScheduleId() + "/confirm"))
            .andExpect(status().isOk());

        // 재확정 시도
        mockMvc.perform(post("/api/v1/schedule/vc/" + s.getVcScheduleId() + "/confirm"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.brCode").value("BR-X01"));
    }
}
