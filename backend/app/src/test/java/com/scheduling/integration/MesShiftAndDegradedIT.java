package com.scheduling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.vc.mes.DegradedModeService;
import com.scheduling.vc.mes.MesShiftEvent;
import com.scheduling.vc.mes.MesShiftEventRepository;
import com.scheduling.vc.mes.MesShiftPort;
import com.scheduling.vc.mes.MesShiftSource;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 17 EP-DAY-LOCK BR-X06 IT — TK-DAY-LOCK-3·4 통합 검증.
 *
 * <p>검증:
 * <ul>
 *   <li>{@link MesShiftPort#reportProduction} — INSERT (UNIQUE machine+date+shift)</li>
 *   <li>POST /api/v1/mes/shift/fallback — PLANNER 200 / STK_USER 403 (RBAC)</li>
 *   <li>GET /api/v1/mes/degraded/status — mesEnabled=false 시 anyDegraded=false (Sprint 17 baseline)</li>
 *   <li>UNIQUE 중복 입력 → UPDATE (재호출 시 actual_qty 갱신)</li>
 *   <li>audit_log row INSERT 자동 (V044 trigger)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MesShiftAndDegradedIT {

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
    @Autowired private MesShiftPort mesPort;
    @Autowired private MesShiftEventRepository repository;
    @Autowired private DegradedModeService degradedService;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // audit_log 은 NFR-SEC-004 immutable — cleanup 미수행 (테스트 격리는 mes_shift_event 만)
        jdbc.update("DELETE FROM app.mes_shift_event");
    }

    // =========================================================================
    // Port 직접
    // =========================================================================

    @Test
    @DisplayName("MesShiftPort.reportProduction — EXCEL_FALLBACK source INSERT + actor 캡쳐")
    void port_excel_fallback_insert() {
        MesShiftEvent saved = mesPort.reportProduction(
            "LP-01", LocalDate.now(), (short) 1, 100, 95,
            MesShiftSource.EXCEL_FALLBACK, "00000001", "test 폴백");

        assertThat(saved.getShiftEventId()).isNotNull();
        assertThat(saved.getSource()).isEqualTo(MesShiftSource.EXCEL_FALLBACK);
        assertThat(saved.getReportedBy()).isEqualTo("00000001");
    }

    @Test
    @DisplayName("MesShiftPort.reportProduction — UNIQUE (machine+date+shift) 재호출 시 actual_qty UPDATE")
    void port_update_on_duplicate() {
        LocalDate d = LocalDate.now();
        mesPort.reportProduction("LP-02", d, (short) 1, 100, 50,
            MesShiftSource.MES, null, null);
        mesPort.reportProduction("LP-02", d, (short) 1, 100, 95,
            MesShiftSource.EXCEL_FALLBACK, "00000001", "MES 누락 보정");

        var saved = repository.findByMachineIdAndShiftDateAndShiftNo("LP-02", d, (short) 1);
        assertThat(saved).isPresent();
        assertThat(saved.get().getActualQty()).isEqualTo(95);
        assertThat(saved.get().getSource()).isEqualTo(MesShiftSource.EXCEL_FALLBACK);
    }

    @Test
    @DisplayName("V044 audit trigger — mes_shift_event INSERT → audit_log row 생성 (actor=reported_by)")
    void v044_audit_trigger_captures_actor() {
        mesPort.reportProduction("LP-03", LocalDate.now(), (short) 2, 100, 80,
            MesShiftSource.EXCEL_FALLBACK, "00000002", "audit 검증");

        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit.schedule_audit_log WHERE table_name='mes_shift_event'",
            Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);

        String actor = jdbc.queryForObject(
            "SELECT actor FROM audit.schedule_audit_log "
                + "WHERE table_name='mes_shift_event' ORDER BY occurred_at DESC LIMIT 1",
            String.class);
        assertThat(actor).isEqualTo("00000002");
    }

    // =========================================================================
    // Controller HTTP
    // =========================================================================

    @Test
    @WithMockUser(roles = "PLANNER", username = "00000001")
    @DisplayName("POST /shift/fallback — PLANNER 200 + EXCEL_FALLBACK + reported_by")
    void http_planner_fallback_200() throws Exception {
        String json = """
            {"machineId":"LP-04","shiftDate":"2026-05-27","shiftNo":3,
             "plannedQty":100,"actualQty":90,"note":"PLANNER 폴백"}
            """;
        mockMvc.perform(post("/api/v1/mes/shift/fallback")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source").value("EXCEL_FALLBACK"))
            .andExpect(jsonPath("$.reportedBy").value("00000001"));
    }

    @Test
    @WithMockUser(roles = "STK_USER", username = "00000004")
    @DisplayName("POST /shift/fallback — STK_USER 403 (RBAC PLANNER+IT_OPS only)")
    void http_stk_user_fallback_forbidden() throws Exception {
        String json = """
            {"machineId":"LP-04","shiftDate":"2026-05-27","shiftNo":3,"plannedQty":100}
            """;
        mockMvc.perform(post("/api/v1/mes/shift/fallback")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STK_USER", username = "00000004")
    @DisplayName("GET /degraded/status — 4 role 모두 read (Sprint 17 mesEnabled=false default → anyDegraded=false)")
    void http_degraded_status_default_normal() throws Exception {
        mockMvc.perform(get("/api/v1/mes/degraded/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anyDegraded").value(false))
            .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("MES disabled")));
    }

    @Test
    @DisplayName("DegradedModeService.isDegraded — mesEnabled=false 시 항상 false")
    void degraded_service_disabled_returns_false() {
        for (String m : DegradedModeService.ACTIVE_MACHINES) {
            assertThat(degradedService.isDegraded(m)).isFalse();
        }
        assertThat(degradedService.snapshot().anyDegraded()).isFalse();
    }
}
