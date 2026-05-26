package com.scheduling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.scheduling.security.auth.AppUser;
import com.scheduling.security.auth.AppUserRepository;
import com.scheduling.security.auth.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 10 EP-AUTH 사번+PIN 로그인 IT — 5 cases (ST-AUTH-3 + ST-AUTH-4 통합 검증).
 *
 * <p>NFR-SEC-007 정합 — 사번 8자리 + PIN 4자리 + 5회/10분 잠금 + JWT 8h.
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthLoginIT {

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

    private MockMvc mockMvc;

    private static final String EMP_ID = "12345678";
    private static final String CORRECT_PIN = "1234";
    private static final String WRONG_PIN = "9999";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        userRepository.deleteAll();
        // 시드 — emp 12345678 / PIN 1234 / PLANNER
        userRepository.save(new AppUser(EMP_ID, passwordEncoder.encode(CORRECT_PIN), AppUser.Role.PLANNER));
    }

    private String loginPayload(String empId, String pin) {
        return "{\"employeeId\":\"" + empId + "\",\"pin\":\"" + pin + "\"}";
    }

    @Test
    @DisplayName("정상 로그인 — 200 OK + JWT + role=PLANNER + 8h expiresAt")
    void login_happy_path_returns_jwt() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(EMP_ID, CORRECT_PIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.employeeId").value(EMP_ID))
            .andExpect(jsonPath("$.role").value("PLANNER"))
            .andExpect(jsonPath("$.expiresAt").isString())
            .andReturn();

        // JWT 가 발급되었는지 확인 (3 part dot-separated)
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("token").asText().split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("잘못된 PIN — 401 + failed_attempts +1 DB 기록")
    void login_wrong_pin_returns_401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(EMP_ID, WRONG_PIN)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("로그인 실패"));

        AppUser u = userRepository.findByEmployeeId(EMP_ID).orElseThrow();
        assertThat(u.getFailedAttempts()).isEqualTo((short) 1);
        assertThat(u.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("5회 실패 → 자동 잠금 (locked_until 설정) → 6번째 시도 423 Locked")
    void login_five_failures_locks_account() throws Exception {
        // 5회 실패 — 5번째에서 카운터 5 도달 → lockedUntil 자동 설정
        for (int i = 0; i < LoginAttemptService.MAX_FAILED_ATTEMPTS; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload(EMP_ID, WRONG_PIN)))
                .andExpect(status().isUnauthorized());
        }

        AppUser locked = userRepository.findByEmployeeId(EMP_ID).orElseThrow();
        assertThat(locked.getFailedAttempts())
            .isGreaterThanOrEqualTo(LoginAttemptService.MAX_FAILED_ATTEMPTS);
        assertThat(locked.getLockedUntil()).isNotNull();

        // 6번째 — 정상 PIN 도 423 (계정 잠금 활성)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(EMP_ID, CORRECT_PIN)))
            .andExpect(status().isLocked())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("잠금")));
    }

    @Test
    @DisplayName("JWT 토큰으로 보호 endpoint (/api/v1/schedule/vc/capacity-overflow/queue) 200 통과")
    void jwt_token_authorizes_protected_endpoint() throws Exception {
        // 로그인 → token 획득
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(EMP_ID, CORRECT_PIN)))
            .andExpect(status().isOk())
            .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString())
            .get("token").asText();

        // Bearer 헤더로 보호 endpoint 호출
        mockMvc.perform(get("/api/v1/schedule/vc/capacity-overflow/queue?status=PENDING")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("validation regex 위반 (사번 7자리) — 400 Bad Request")
    void login_invalid_format_returns_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload("1234567", CORRECT_PIN)))
            .andExpect(status().isBadRequest());

        // 카운터 변경 없음 (validation 단계에서 차단)
        AppUser u = userRepository.findByEmployeeId(EMP_ID).orElseThrow();
        assertThat(u.getFailedAttempts()).isZero();
    }
}
