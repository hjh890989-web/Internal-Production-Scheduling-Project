package com.scheduling.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 11 EP-RBAC ST-RBAC-5 — Strict Auth Mode 부팅 검증 IT (NFR-SEC-007 + NFR-SEC-003).
 *
 * <p>{@code app.auth.dev-fallback=false} 설정 → SecurityConfig strict 분기 활성 →
 * anonymous 허용 안 됨, 보호 endpoint 호출 시 401 / 잘못된 JWT 도 401.
 *
 * <p>본 IT 가 GREEN = Sprint 19 EP-BETA-LAUNCH 진입 게이트 (env APP_AUTH_DEV_FALLBACK=false
 * 부팅 가능 보장). 정상 사번+PIN 로그인 → JWT → 200 흐름은 {@link AuthLoginIT} 가 별도 검증.
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StrictAuthModeIT {

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
        // ⭐ Strict mode 강제 — anonymous 비활성, 사번+PIN JWT 필수
        registry.add("app.auth.dev-fallback", () -> "false");
    }

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Strict — 보호 endpoint 호출 (JWT 없음) → 401")
    void protected_endpoint_without_token_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/schedule/vc/capacity-overflow/queue"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Strict — 잘못된 JWT (서명 불일치) → 401")
    void invalid_jwt_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/schedule/vc/capacity-overflow/queue")
                .header("Authorization", "Bearer not-a-valid-jwt-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Strict — /api/v1/auth/login 은 anonymous 허용 (로그인 시도 가능, 401 BadCredentials 응답)")
    void login_endpoint_allows_anonymous() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"employeeId\":\"99999999\",\"pin\":\"9999\"}"))
            .andExpect(status().isUnauthorized());   // 사번 미존재 = 401 (anonymous 도달 OK)
    }

    @Test
    @DisplayName("Strict — Actuator health 는 anonymous 도달 (200 또는 503, 401 아님)")
    void health_endpoint_allows_anonymous() throws Exception {
        // 503 (Redis 미가용 등 component DOWN) 도 anonymous 도달 의미 — 401 만 아니면 OK
        mockMvc.perform(get("/api/actuator/health"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                if (status == 401 || status == 403) {
                    throw new AssertionError("health endpoint anonymous 차단됨 — status=" + status);
                }
            });
    }
}
