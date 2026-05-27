package com.scheduling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduling.security.auth.AppUser;
import com.scheduling.security.auth.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 12 EP-MASTER-UI UserAdmin REST IT — 5 cases (TK-MASTER-2-5).
 *
 * <p>RBAC: IT_OPS only. STK_USER/PLANNER → 403. audit_log.actor 검증 (BR-X02).
 */
@SpringBootTest
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserAdminIT {

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
    @Autowired private JdbcTemplate jdbc;

    private MockMvc mockMvc;

    private static final String BASE = "/api/v1/master/user";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // V037 시드 8명 + 본 test 격리 위해 cleanup 후 baseline 1명만 시드
        userRepository.deleteAll();
        userRepository.save(new AppUser("00000007", passwordEncoder.encode("0007"), AppUser.Role.IT_OPS));
    }

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("IT_OPS list — 200 + 시드 IT_OPS 1명 반환")
    void list_it_ops_returns_users() throws Exception {
        mockMvc.perform(get(BASE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].employeeId").value("00000007"))
            .andExpect(jsonPath("$[0].role").value("IT_OPS"))
            // pin_hash 노출 안 함 검증 (NFR-SEC-005)
            .andExpect(jsonPath("$[0].pinHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("IT_OPS create — 201 + 사용자 추가 + 중복 409")
    void create_user_then_conflict() throws Exception {
        String payload = "{\"employeeId\":\"00000099\",\"pin\":\"1234\",\"role\":\"PLANNER\"}";

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employeeId").value("00000099"))
            .andExpect(jsonPath("$.role").value("PLANNER"));

        // 중복 → 409
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("IT_OPS resetPin — 200 + 새 PIN 으로 로그인 가능 (audit_log actor=00000007)")
    void reset_pin_then_audit() throws Exception {
        // 시드 사용자 추가
        userRepository.save(new AppUser("00000099", passwordEncoder.encode("1234"), AppUser.Role.PLANNER));

        mockMvc.perform(post(BASE + "/00000099/reset-pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPin\":\"5678\"}"))
            .andExpect(status().isOk());

        // 새 PIN 으로 hash 변경 확인
        AppUser u = userRepository.findByEmployeeId("00000099").orElseThrow();
        assertThat(passwordEncoder.matches("5678", u.getPinHash())).isTrue();
        assertThat(passwordEncoder.matches("1234", u.getPinHash())).isFalse();

        // BR-X02 audit_log.actor = 00000007 (IT_OPS) 검증 + pin_hash 마스킹 (NFR-SEC-005)
        String actor = jdbc.queryForObject(
            "SELECT actor FROM audit.schedule_audit_log "
                + "WHERE table_name='user_account' AND row_pk='00000099' AND action='UPDATE' "
                + "ORDER BY occurred_at DESC LIMIT 1",
            String.class);
        assertThat(actor).isEqualTo("00000007");
    }

    @Test
    @WithMockUser(roles = "IT_OPS", username = "00000007")
    @DisplayName("IT_OPS unlock — locked_until 미래 사용자 → 잠금 해제 + failed_attempts reset")
    void unlock_locked_user() throws Exception {
        AppUser locked = new AppUser("00000099", passwordEncoder.encode("1234"), AppUser.Role.PLANNER);
        locked.recordFailure();
        locked.recordFailure();
        locked.lock(Instant.now().plusSeconds(600));
        userRepository.save(locked);

        mockMvc.perform(post(BASE + "/00000099/unlock"))
            .andExpect(status().isOk());

        AppUser unlocked = userRepository.findByEmployeeId("00000099").orElseThrow();
        assertThat(unlocked.getFailedAttempts()).isZero();
        assertThat(unlocked.getLockedUntil()).isNull();
    }

    @Test
    @WithMockUser(roles = "STK_USER")
    @DisplayName("RBAC — STK_USER 403 (4 endpoint 모두 IT_OPS only)")
    void stk_user_forbidden_all() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeId\":\"00000088\",\"pin\":\"1111\",\"role\":\"PLANNER\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE + "/00000007/reset-pin").contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPin\":\"9999\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE + "/00000007"))
            .andExpect(status().isForbidden());
    }
}
