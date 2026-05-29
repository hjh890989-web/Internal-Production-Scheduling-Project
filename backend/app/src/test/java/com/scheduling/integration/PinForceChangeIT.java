package com.scheduling.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.security.auth.AppUser;
import com.scheduling.security.auth.AppUserRepository;
import com.scheduling.security.auth.UserAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 22 ST-SEC-2 + ST-SEC-4 IT — PIN 30일 강제 변경 + IT_OPS reset 강제 만료 (NFR-SEC-007).
 *
 * <p>검증:
 * <ul>
 *   <li>30일 초과 미변경 사용자 로그인 → pinExpired=true / 정상 사용자 → false</li>
 *   <li>change-pin (현재 PIN 재인증 + 새 PIN) → 200, 이후 로그인 pinExpired=false</li>
 *   <li>change-pin 현재 PIN 불일치 → 401</li>
 *   <li>IT_OPS resetPin → 대상 사용자 첫 로그인 pinExpired=true (ST-SEC-4 강제 만료)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PinForceChangeIT {

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
    @Autowired private AppUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserAdminService userAdminService;
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    private static final String EMP_ID = "00000010";
    private static final String PIN = "1234";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        userRepository.deleteAll();
    }

    private void agePinChange(String employeeId, int daysAgo) {
        jdbc.update("UPDATE app.user_account SET last_pin_change_at = ? WHERE employee_id = ?",
            Timestamp.from(Instant.now().minus(daysAgo, ChronoUnit.DAYS)), employeeId);
    }

    private boolean loginPinExpired(String pin) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"" + EMP_ID + "\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
            .get("pinExpired").asBoolean();
    }

    private String loginToken(String pin) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"" + EMP_ID + "\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("30일 초과 미변경 → pinExpired=true / 정상 → false")
    void login_flags_pin_expiry() throws Exception {
        userRepository.save(new AppUser(EMP_ID, passwordEncoder.encode(PIN), AppUser.Role.PLANNER));

        // 신규 (now) → false
        assertThat(loginPinExpired(PIN)).isFalse();

        // 31일 경과 → true
        agePinChange(EMP_ID, 31);
        assertThat(loginPinExpired(PIN)).isTrue();
    }

    @Test
    @DisplayName("change-pin — 현재 PIN 재인증 + 새 PIN → 200 + 이후 pinExpired=false")
    void change_pin_clears_expiry() throws Exception {
        userRepository.save(new AppUser(EMP_ID, passwordEncoder.encode(PIN), AppUser.Role.PLANNER));
        agePinChange(EMP_ID, 31);
        assertThat(loginPinExpired(PIN)).isTrue();

        String token = loginToken(PIN);
        String newPin = "5678";
        mockMvc.perform(post("/api/v1/auth/change-pin")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPin\":\"" + PIN + "\",\"newPin\":\"" + newPin + "\"}"))
            .andExpect(status().isOk());

        // 새 PIN 으로 로그인 → 만료 해제
        assertThat(loginPinExpired(newPin)).isFalse();
    }

    @Test
    @DisplayName("change-pin — 현재 PIN 불일치 → 401")
    void change_pin_wrong_current_rejected() throws Exception {
        userRepository.save(new AppUser(EMP_ID, passwordEncoder.encode(PIN), AppUser.Role.PLANNER));
        String token = loginToken(PIN);

        mockMvc.perform(post("/api/v1/auth/change-pin")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPin\":\"9999\",\"newPin\":\"5678\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("현재 PIN")));
    }

    @Test
    @DisplayName("ST-SEC-4 — IT_OPS resetPin → 대상 사용자 첫 로그인 pinExpired=true (강제 만료)")
    void it_ops_reset_forces_expiry() throws Exception {
        userRepository.save(new AppUser(EMP_ID, passwordEncoder.encode(PIN), AppUser.Role.PLANNER));
        assertThat(loginPinExpired(PIN)).isFalse();

        // IT_OPS 가 임시 PIN 으로 reset → 강제 만료 (now-31d)
        String tempPin = "0000";
        userAdminService.resetPin(EMP_ID, tempPin);

        // 임시 PIN 로그인 가능하지만 즉시 강제 변경 (pinExpired=true)
        assertThat(loginPinExpired(tempPin)).isTrue();
    }
}
