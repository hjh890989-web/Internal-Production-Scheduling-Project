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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 19 EP-BETA-LAUNCH ST-BETA-1 IT — V045 cleanup function (TK-BETA-1-2).
 *
 * <p>검증:
 * <ul>
 *   <li>{@code app.cleanup_99999_samples()} 함수 정의 + 호출 가능</li>
 *   <li>99999-SAMPLE-* row 삭제 + deleted_count 반환 (vc_schedule + ex_schedule_candidate)</li>
 *   <li>운영 데이터 (29673-2R060 등 실 hose_id) 보존</li>
 *   <li>idempotent — 두 번째 호출 deleted_count=0</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SampleCleanupIT {

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

    @Autowired private VcScheduleRepository repository;
    @Autowired private JdbcTemplate jdbc;

    private static final Instant T0 = Instant.parse("2026-05-28T00:00:00Z");

    @BeforeEach
    void clean() {
        // 본 IT 만의 stub 추가 — V039 sample 이 이미 들어있음 (Flyway baseline). 운영 row 도 추가.
        jdbc.update("DELETE FROM app.vc_schedule WHERE hose_id IN ('99999-SAMPLE-IT-1', '99999-SAMPLE-IT-2', '29673-2R060-IT')");

        // sample 2건 + 운영 1건 INSERT (D-2 hard trigger 우회 — direct JDBC 사용)
        jdbc.update("ALTER TABLE app.vc_schedule DISABLE TRIGGER trg_vc_schedule_d2_hard");
        repository.saveAndFlush(buildSchedule("99999-SAMPLE-IT-1", LocalDate.now().plusDays(5), (short) 1));
        repository.saveAndFlush(buildSchedule("99999-SAMPLE-IT-2", LocalDate.now().plusDays(5), (short) 2));
        repository.saveAndFlush(buildSchedule("29673-2R060-IT", LocalDate.now().plusDays(5), (short) 3));
        jdbc.update("ALTER TABLE app.vc_schedule ENABLE TRIGGER trg_vc_schedule_d2_hard");
    }

    private VcSchedule buildSchedule(String hoseId, LocalDate prod, short slot) {
        return new VcSchedule(UUID.randomUUID(), hoseId, "LP-04", slot, prod, (short) 5,
            "ANG-CLEANUP", 100, VcScheduleStatus.CANDIDATE, "", T0, T0);
    }

    @Test
    @DisplayName("V045 — cleanup_99999_samples() 함수 정의 확인 (information_schema)")
    void function_defined() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.routines "
                + "WHERE routine_schema='app' AND routine_name='cleanup_99999_samples'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("V045 — cleanup 호출 시 99999-SAMPLE 만 삭제 + 운영 row 보존")
    void cleanup_removes_only_samples() {
        // 호출 전 — sample 2 + 운영 1 + V039 seed (15)
        List<Map<String, Object>> result = jdbc.queryForList(
            "SELECT * FROM app.cleanup_99999_samples()");
        assertThat(result).hasSize(2);
        // 두 개의 row — vc_schedule + ex_schedule_candidate
        Map<String, Object> vcRow = result.stream()
            .filter(r -> "vc_schedule".equals(r.get("table_name"))).findFirst().orElseThrow();
        assertThat((Integer) vcRow.get("deleted_count")).isGreaterThanOrEqualTo(2);   // 본 IT 가 추가한 2건 이상 (+ V039 seed)

        // 운영 row 보존 검증
        Integer operationalCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app.vc_schedule WHERE hose_id = '29673-2R060-IT'",
            Integer.class);
        assertThat(operationalCount).isEqualTo(1);

        // 99999-SAMPLE row 0건 검증
        Integer remainingSamples = jdbc.queryForObject(
            "SELECT COUNT(*) FROM app.vc_schedule WHERE hose_id LIKE '99999-SAMPLE-%'",
            Integer.class);
        assertThat(remainingSamples).isZero();
    }

    @Test
    @DisplayName("V045 — idempotent: 두 번째 호출 deleted_count=0")
    void cleanup_idempotent() {
        jdbc.queryForList("SELECT * FROM app.cleanup_99999_samples()");

        List<Map<String, Object>> second = jdbc.queryForList(
            "SELECT * FROM app.cleanup_99999_samples()");
        assertThat(second).hasSize(2);
        second.forEach(r ->
            assertThat((Integer) r.get("deleted_count")).isZero());
    }
}
