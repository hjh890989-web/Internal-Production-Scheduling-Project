package com.scheduling.integration;

import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import com.scheduling.vc.domain.VcScheduleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 14 EP-VC-FULL TK-VC-2-5 — VcScheduleQueryController /slots IT.
 *
 * <p>range query 정확성 + 4 role read 검증. V039 sample 시드 외 추가 row 직접 시드.
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class VcScheduleQueryControllerIT {

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

    private MockMvc mockMvc;
    private static final LocalDate IN_RANGE = LocalDate.of(2026, 6, 1);
    private static final LocalDate OUT_RANGE = LocalDate.of(2026, 7, 15);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        // 매 BeforeEach 마다 cleanup (DirtiesContext AFTER_CLASS — 같은 context 내 BeforeEach 중복 INSERT 차단)
        repository.deleteAll();

        // V039 시드 (99999-SAMPLE-* CURRENT_DATE 기반) 외 본 IT 격리용 row 추가
        // IN_RANGE 2 row + OUT_RANGE 1 row — from/to 필터링 검증
        repository.save(new VcSchedule(UUID.randomUUID(), "IT-VCQUERY-A", "LP-01",
            (short) 5, IN_RANGE, (short) 1, "ANG-X", 100,
            VcScheduleStatus.CANDIDATE, "", Instant.now(), Instant.now()));
        repository.save(new VcSchedule(UUID.randomUUID(), "IT-VCQUERY-B", "LP-01",
            (short) 5, IN_RANGE.plusDays(1), (short) 1, "ANG-X", 100,
            VcScheduleStatus.CANDIDATE, "", Instant.now(), Instant.now()));
        repository.save(new VcSchedule(UUID.randomUUID(), "IT-VCQUERY-OUT", "LP-01",
            (short) 5, OUT_RANGE, (short) 1, "ANG-X", 100,
            VcScheduleStatus.CANDIDATE, "", Instant.now(), Instant.now()));
    }

    @Test
    @WithMockUser(roles = "PLANNER")
    @DisplayName("PLANNER /slots from-to 범위 — IN_RANGE 2 row 만 반환 (OUT_RANGE 제외)")
    void slots_range_filter_returns_in_range_only() throws Exception {
        mockMvc.perform(get("/api/v1/schedule/vc/slots")
                .param("from", IN_RANGE.toString())
                .param("to", IN_RANGE.plusDays(2).toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            // IT-VCQUERY-A / -B 모두 IN_RANGE 안 → 2 row
            // (V039 시드는 CURRENT_DATE 기반이라 IN_RANGE 2026-06-01 과 다른 시점 → 영향 0)
            .andExpect(jsonPath("$[?(@.hoseId == 'IT-VCQUERY-A')]").exists())
            .andExpect(jsonPath("$[?(@.hoseId == 'IT-VCQUERY-B')]").exists())
            .andExpect(jsonPath("$[?(@.hoseId == 'IT-VCQUERY-OUT')]").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("STK_USER /slots — 4 role 모두 read 가능 (시뮬뷰 시각 정합)")
    void slots_stk_user_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/schedule/vc/slots")
                .param("from", IN_RANGE.toString())
                .param("to", IN_RANGE.plusDays(2).toString()))
            .andExpect(status().isOk());
    }
}
