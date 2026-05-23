package com.scheduling.kpi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 사업 KPI 일별 영속 — TK-47-1·2·3·4 (EP-47, KPI-001~019).
 *
 * <p>스케줄러 또는 운영 백오피스에서 호출. {@code business_kpi.measurement} UPSERT.
 * Grafana 대시 (business-kpi.json) 가 직접 조회.
 *
 * <p>{@code above_target} 자동 계산 — {@code business_kpi.definition} 의 target_dir 참조:
 * <ul>
 *   <li>{@code higher} — metric_value &gt;= threshold → above_target = true</li>
 *   <li>{@code lower}  — metric_value &lt;= threshold → above_target = true (적을수록 좋음)</li>
 * </ul>
 */
@Component
@Profile("with-infra")
public class BusinessKpiPersister {

    private static final Logger log = LoggerFactory.getLogger(BusinessKpiPersister.class);

    private final JdbcTemplate jdbc;

    public BusinessKpiPersister(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 단일 KPI 일별 측정값 UPSERT.
     *
     * @return above_target (Slack alert 입력 — false 면 임계값 미달)
     */
    @Transactional
    public boolean record(String kpiCode, LocalDate measuredDate, BigDecimal metricValue) {
        // definition lookup
        DefinitionRow def;
        try {
            def = jdbc.queryForObject(
                "SELECT threshold, target_dir FROM business_kpi.definition WHERE kpi_code = ?",
                (rs, n) -> new DefinitionRow(
                    rs.getBigDecimal("threshold"),
                    rs.getString("target_dir")),
                kpiCode);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("KPI definition 미존재: " + kpiCode, ex);
        }
        if (def == null) {
            throw new IllegalArgumentException("KPI definition 미존재: " + kpiCode);
        }

        boolean aboveTarget = evaluate(def, metricValue);

        jdbc.update(
            "INSERT INTO business_kpi.measurement "
                + "(kpi_code, measured_date, metric_value, threshold, above_target, source) "
                + "VALUES (?, ?, ?, ?, ?, 'scheduled') "
                + "ON CONFLICT (kpi_code, measured_date) DO UPDATE SET "
                + "    metric_value = EXCLUDED.metric_value, "
                + "    threshold    = EXCLUDED.threshold, "
                + "    above_target = EXCLUDED.above_target, "
                + "    captured_at  = now()",
            kpiCode, measuredDate, metricValue, def.threshold(), aboveTarget);

        if (!aboveTarget) {
            log.warn("KPI {} BELOW target on {} — value={}, threshold={}, dir={}",
                kpiCode, measuredDate, metricValue, def.threshold(), def.targetDir());
        }
        return aboveTarget;
    }

    private boolean evaluate(DefinitionRow def, BigDecimal value) {
        if (def.threshold() == null) return true;     // 단순 추적
        return "higher".equals(def.targetDir())
            ? value.compareTo(def.threshold()) >= 0
            : value.compareTo(def.threshold()) <= 0;
    }

    private record DefinitionRow(BigDecimal threshold, String targetDir) {}
}
