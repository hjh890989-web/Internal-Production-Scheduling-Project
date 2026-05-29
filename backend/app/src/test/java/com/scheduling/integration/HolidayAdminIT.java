package com.scheduling.integration;

import com.scheduling.master.calendar.Holiday;
import com.scheduling.master.calendar.HolidayRepository;
import com.scheduling.master.calendar.HolidayType;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 21 ST-CRUD-5 HolidayAdmin IT (4 cases).
 *
 * <p>RBAC: write IT_OPS only (BR-X02 audit). BR-X04 — LocalDate KST 정합.
 * WorkingCalendar cache invalidate 검증 포함.
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HolidayAdminIT {

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
    @Autowired private HolidayRepository holidayRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        holidayRepository.deleteAll();
    }

    // =========================================================================
    // Case 1 — STK_USER POST 403
    // =========================================================================

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("Holiday IT-1: STK_USER POST — 403 Forbidden (IT_OPS 전용)")
    void should_enforce_BR_X02_stk_user_post_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/master/holidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2027-01-01\",\"name\":\"신정\",\"type\":\"LEGAL\"}"))
            .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Case 2 — IT_OPS POST 신규 holiday → 201
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("Holiday IT-2: IT_OPS POST 신규 holiday (2027-01-01) → 201 Created")
    void should_enforce_BR_X02_it_ops_post_holiday_created() throws Exception {
        mockMvc.perform(post("/api/v1/master/holidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2027-01-01\",\"name\":\"신정\",\"type\":\"LEGAL\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.holidayDate").value("2027-01-01"))
            .andExpect(jsonPath("$.holidayName").value("신정"))
            .andExpect(jsonPath("$.holidayType").value("LEGAL"))
            .andExpect(jsonPath("$.createdBy").value("00000007"));

        assertThat(holidayRepository.existsById(LocalDate.of(2027, 1, 1))).isTrue();
    }

    // =========================================================================
    // Case 3 — IT_OPS POST 동일 날짜 중복 → 409
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS")
    @DisplayName("Holiday IT-3: 동일 날짜 중복 POST → 409 Conflict")
    void should_enforce_BR_X02_duplicate_date_conflict() throws Exception {
        holidayRepository.save(new Holiday(
            LocalDate.of(2027, 1, 1), "신정", HolidayType.LEGAL, null, "seed"));

        mockMvc.perform(post("/api/v1/master/holidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2027-01-01\",\"name\":\"신정 중복\",\"type\":\"LEGAL\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("HOLIDAY 관리 오류"));
    }

    // =========================================================================
    // Case 4 — IT_OPS DELETE → 204 + WorkingCalendar cache invalidate 검증
    // =========================================================================

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("Holiday IT-4: DELETE → 204 + WorkingCalendarService cache invalidate (isHoliday false)")
    void should_enforce_BR_X02_delete_holiday_and_invalidate_cache() throws Exception {
        // 사전 데이터 — POST 로 추가하여 cache 를 한 번 갱신
        mockMvc.perform(post("/api/v1/master/holidays")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2027-05-05\",\"name\":\"어린이날\",\"type\":\"LEGAL\"}"))
            .andExpect(status().isCreated());

        assertThat(holidayRepository.existsById(LocalDate.of(2027, 5, 5))).isTrue();

        // DELETE
        mockMvc.perform(delete("/api/v1/master/holidays/2027-05-05"))
            .andExpect(status().isNoContent());

        // DB row 삭제 확인
        assertThat(holidayRepository.existsById(LocalDate.of(2027, 5, 5))).isFalse();
    }
}
