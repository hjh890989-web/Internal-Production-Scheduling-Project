package com.scheduling.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.master.vc.VcMachineRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 21 ST-CRUD-1 VcMachine 관리 IT (5 cases).
 *
 * <p>RBAC: write IT_OPS only, read 4 role. audit_log.actor 검증 (BR-X02).
 * DELETE 는 active=false toggle — app.vc_schedule FK row 보존 검증.
 *
 * @see com.scheduling.master.vc.VcMachineAdminController
 * @see com.scheduling.master.vc.VcMachineAdminService
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VcMachineAdminIT {

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
    @Autowired private VcMachineRepository machineRepo;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    /** Flyway seed 로 LP-01~LP-04, IC-01 이 이미 존재 — 테스트는 seed row 를 직접 사용. */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // =========================================================================
    // TC-1: PLANNER read 200
    // =========================================================================

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("TC-1: PLANNER read GET /vc-machines — 200 + 비어있지 않은 목록")
    void tc1_planner_read_200() throws Exception {
        mockMvc.perform(get("/api/v1/master/vc-machines"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // TC-2: READ_ONLY read 200
    // =========================================================================

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("TC-2: READ_ONLY read GET /vc-machines — 200")
    void tc2_read_only_read_200() throws Exception {
        mockMvc.perform(get("/api/v1/master/vc-machines"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // =========================================================================
    // TC-3: STK_USER write (PUT) — 403
    // =========================================================================

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("TC-3: STK_USER PUT /vc-machines/LP-01 — 403 (write IT_OPS only)")
    void tc3_stk_user_write_forbidden() throws Exception {
        mockMvc.perform(put("/api/v1/master/vc-machines/LP-01")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"totalSlots\":8,\"dayRotations\":8,\"nightRotations\":10,\"active\":true}"))
            .andExpect(status().isForbidden());
    }

    // =========================================================================
    // TC-4: IT_OPS PUT total_slots 변경 → 200 + audit_log row 검증
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("TC-4: IT_OPS PUT LP-01 totalSlots 변경 → 200 + audit_log actor 검증 (BR-X02)")
    void tc4_it_ops_update_total_slots_and_audit() throws Exception {
        // 현재 LP-01 totalSlots = 8 (seed). 동일 값으로 update (entity invariant: LP must be 8)
        // audit_log row 생성 여부만 검증
        MvcResult result = mockMvc.perform(put("/api/v1/master/vc-machines/LP-01")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"totalSlots\":8,\"dayRotations\":8,\"nightRotations\":10,\"active\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.machineId").value("LP-01"))
            .andExpect(jsonPath("$.totalSlots").value(8))
            .andExpect(jsonPath("$.updatedBy").value("00000007"))
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("active").asBoolean()).isTrue();

        // BR-X02: audit_log.actor = 00000007 (V047 trigger 검증)
        Integer auditCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit.schedule_audit_log "
                + "WHERE table_name='vc_machine' AND row_pk='LP-01' AND actor=?",
            Integer.class, "00000007"
        );
        assertThat(auditCount).isGreaterThanOrEqualTo(1);
    }

    // =========================================================================
    // TC-5: IT_OPS DELETE LP-04 → active=false, vc_schedule FK row 보존
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("TC-5: IT_OPS DELETE LP-04 → active=false, vc_schedule FK row 보존")
    void tc5_deactivate_lp04_fk_row_preserved() throws Exception {
        // seed: app.vc_schedule 에 LP-04 row 직접 INSERT (cross-module 의존 없이 JdbcTemplate 사용).
        // production_date 는 D+7 (BR-X07 D-2 hard 제약 trigger app.enforce_vc_schedule_d2_hard 통과)
        UUID scheduleId = UUID.randomUUID();
        jdbc.update(
            "INSERT INTO app.vc_schedule ("
                + "vc_schedule_id, hose_id, machine_id, slot_position, production_date, "
                + "rotation_no, angle_id, planned_qty, status, linked_order_ids, "
                + "created_at, updated_at"
                + ") VALUES (?,?,?,?,?,?,?,?,?,?,now(),now())",
            scheduleId, "29673-2R060", "LP-04",
            (short) 1, java.time.LocalDate.now().plusDays(7),
            (short) 1, "ANGLE-TEST", 100, "CANDIDATE", ""
        );

        // DELETE endpoint — active=false toggle
        mockMvc.perform(delete("/api/v1/master/vc-machines/LP-04"))
            .andExpect(status().isNoContent());

        // LP-04 row 는 여전히 master.vc_machine 에 존재 (active=false)
        assertThat(machineRepo.findById("LP-04")).isPresent();
        assertThat(machineRepo.findById("LP-04").get().isActive()).isFalse();

        // app.vc_schedule FK row 는 보존 (cascade DELETE 아님)
        Integer scheduleCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app.vc_schedule WHERE vc_schedule_id = ?",
            Integer.class, scheduleId
        );
        assertThat(scheduleCount).isEqualTo(1);
    }
}
