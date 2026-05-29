package com.scheduling.integration;

import com.scheduling.audit.maintenance.PartitionMaintenanceScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprint 22 ST-SEC-3 IT — audit_log partition rolling-window 유지 (NFR-SEC-004).
 *
 * <p>검증:
 * <ul>
 *   <li>{@code audit.ensure_month_partition} (V053) — 미존재 월 생성 + 재호출 idempotent (EXISTS)</li>
 *   <li>신규 partition 라우팅 — 해당 월 occurred_at INSERT → DEFAULT 아닌 전용 partition 적재</li>
 *   <li>불변 트리거 상속 — 신규 partition row UPDATE → V030 트리거 reject (변조 금지)</li>
 *   <li>{@link PartitionMaintenanceScheduler} — 향후 12개월 확보 + 기존 partition 보존</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuditPartitionMaintenanceIT {

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

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PartitionMaintenanceScheduler scheduler;

    private String ensure(String date) {
        return jdbc.queryForObject("SELECT audit.ensure_month_partition(?::date)", String.class, date);
    }

    private boolean partitionExists(String name) {
        Integer c = jdbc.queryForObject(
            "SELECT COUNT(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE n.nspname = 'audit' AND c.relname = ?", Integer.class, name);
        return c != null && c == 1;
    }

    @Test
    @DisplayName("ensure_month_partition — 미존재 월 CREATED + 재호출 EXISTS (idempotent)")
    void creates_then_idempotent() {
        // 2030-03 은 V030 (2028m12 까지) 범위 밖 → 최초 생성
        assertThat(partitionExists("schedule_audit_log_y2030m03")).isFalse();

        assertThat(ensure("2030-03-15")).isEqualTo("CREATED: schedule_audit_log_y2030m03");
        assertThat(partitionExists("schedule_audit_log_y2030m03")).isTrue();

        // 재호출 — 이미 존재 → skip
        assertThat(ensure("2030-03-01")).isEqualTo("EXISTS: schedule_audit_log_y2030m03");
    }

    @Test
    @DisplayName("신규 partition 라우팅 + 불변 트리거 상속 — INSERT 전용 적재 + UPDATE reject")
    void new_partition_routes_and_is_immutable() {
        ensure("2030-06-10");
        assertThat(partitionExists("schedule_audit_log_y2030m06")).isTrue();

        jdbc.update(
            "INSERT INTO audit.schedule_audit_log (table_name, row_pk, action, actor, occurred_at) "
                + "VALUES (?, ?, ?, ?, ?)",
            "vc_machine", "PART-TEST-2030m06", "INSERT", "system",
            Timestamp.from(Instant.parse("2030-06-15T00:00:00Z")));

        // 전용 partition 으로 라우팅 (DEFAULT 아님)
        String partition = jdbc.queryForObject(
            "SELECT tableoid::regclass::text FROM audit.schedule_audit_log WHERE row_pk = ?",
            String.class, "PART-TEST-2030m06");
        assertThat(partition).endsWith("schedule_audit_log_y2030m06");

        // V030 BEFORE UPDATE 트리거가 신규 partition 에 상속 → 변조 차단
        assertThatThrownBy(() -> jdbc.update(
            "UPDATE audit.schedule_audit_log SET reason = '변조' WHERE row_pk = ?",
            "PART-TEST-2030m06"))
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("변조 금지");
    }

    @Test
    @DisplayName("PartitionMaintenanceScheduler — 향후 12개월 확보 + 기존 partition 보존")
    void scheduler_ensures_lookahead_and_preserves_existing() {
        // V030 사전 생성분 보존 (기준점)
        assertThat(partitionExists("schedule_audit_log_y2026m05")).isTrue();

        String summary = scheduler.ensureUpcomingPartitions();
        assertThat(summary).contains("partition maintenance 완료").contains("lookahead=12개월");

        // 기존 partition 그대로 보존
        assertThat(partitionExists("schedule_audit_log_y2026m05")).isTrue();
        assertThat(partitionExists("schedule_audit_log_default")).isTrue();
    }
}
