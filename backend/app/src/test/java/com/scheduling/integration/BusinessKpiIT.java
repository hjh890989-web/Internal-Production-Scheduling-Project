package com.scheduling.integration;

import com.scheduling.kpi.BusinessKpiPersister;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * EP-47 IT — BusinessKpiPersister + definition 정합 (KPI-001~019).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BusinessKpiIT {

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

    @Autowired private BusinessKpiPersister persister;
    @Autowired private JdbcTemplate jdbc;

    private static final LocalDate D = LocalDate.of(2026, 5, 23);

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM business_kpi.measurement");
    }

    @Test
    @DisplayName("V032 definition seed — 9 KPI 미리 등록")
    void definition_seed_9_kpis() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM business_kpi.definition", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(9);
    }

    @Test
    @DisplayName("NS-S04 (도달률) 95% — higher dir, threshold 95.00, above_target=true")
    void record_ns_s04_above_target() {
        boolean ok = persister.record("NS-S04", D, new BigDecimal("96.50"));
        assertThat(ok).isTrue();

        Boolean stored = jdbc.queryForObject(
            "SELECT above_target FROM business_kpi.measurement WHERE kpi_code = ? AND measured_date = ?",
            Boolean.class, "NS-S04", D);
        assertThat(stored).isTrue();
    }

    @Test
    @DisplayName("NS-S09 (신규 라인) 85% — threshold 90.00 미달, above_target=false")
    void record_ns_s09_below_target() {
        boolean ok = persister.record("NS-S09", D, new BigDecimal("85.00"));
        assertThat(ok).isFalse();
    }

    @Test
    @DisplayName("K-V04 (BR-V07 위반) 2건 — lower dir, threshold 0.00, above_target=false")
    void record_k_v04_lower_dir() {
        boolean ok = persister.record("K-V04", D, new BigDecimal("2.0000"));
        assertThat(ok).isFalse();   // lower dir + 2 > 0 → 임계 초과
    }

    @Test
    @DisplayName("K-V04 위반 0건 — lower dir, threshold 0.00, above_target=true")
    void record_k_v04_zero_violations() {
        boolean ok = persister.record("K-V04", D, new BigDecimal("0.0000"));
        assertThat(ok).isTrue();
    }

    @Test
    @DisplayName("UPSERT — 같은 (kpi_code, date) 재기록 시 갱신 (덮어쓰기)")
    void upsert_same_date_overwrites() {
        persister.record("NS-S04", D, new BigDecimal("80.00"));
        persister.record("NS-S04", D, new BigDecimal("97.00"));

        BigDecimal latest = jdbc.queryForObject(
            "SELECT metric_value FROM business_kpi.measurement WHERE kpi_code = ? AND measured_date = ?",
            BigDecimal.class, "NS-S04", D);
        assertThat(latest).isEqualByComparingTo("97.00");
    }

    @Test
    @DisplayName("KPI 미등록 → IllegalArgumentException")
    void unknown_kpi_rejected() {
        assertThatThrownBy(() -> persister.record("UNKNOWN-X", D, BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("미존재");
    }
}
