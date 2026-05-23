package com.scheduling.kpi;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 사업 KPI REST — TK-47-1~4 (EP-47, KPI-001~019).
 *
 * <p>{@code GET /api/v1/kpi/measurements?from=&to=&kpi=} — Grafana 직접 조회 / 운영 대시
 *
 * <p>{@code POST /api/v1/kpi/measurements/{kpiCode}} — 백오피스 수동 기록 / 배치 스케줄러
 */
@RestController
@RequestMapping("/api/v1/kpi")
@Profile("with-infra")
public class BusinessKpiController {

    private final BusinessKpiPersister persister;
    private final JdbcTemplate jdbc;

    public BusinessKpiController(BusinessKpiPersister persister, JdbcTemplate jdbc) {
        this.persister = persister;
        this.jdbc = jdbc;
    }

    public record RecordPayload(LocalDate measuredDate, BigDecimal metricValue) {}

    public record RecordResponse(String kpiCode, LocalDate measuredDate, boolean aboveTarget) {}

    @GetMapping("/measurements")
    @PreAuthorize("hasAnyRole('IT_OPS','READ_ONLY','PLANNER')")
    public List<Map<String, Object>> list(
        @RequestParam(required = false) String kpi,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String sql = "SELECT kpi_code, measured_date, metric_value, threshold, above_target, captured_at "
            + "FROM business_kpi.measurement "
            + "WHERE measured_date BETWEEN ? AND ? "
            + (kpi != null ? "AND kpi_code = ? " : "")
            + "ORDER BY measured_date DESC, kpi_code";
        Object[] args = kpi != null ? new Object[]{from, to, kpi} : new Object[]{from, to};
        return jdbc.queryForList(sql, args);
    }

    @PostMapping("/measurements/{kpiCode}")
    @PreAuthorize("hasRole('IT_OPS')")
    public RecordResponse record(@PathVariable String kpiCode, @RequestBody RecordPayload payload) {
        boolean ok = persister.record(kpiCode, payload.measuredDate(), payload.metricValue());
        return new RecordResponse(kpiCode, payload.measuredDate(), ok);
    }

    @GetMapping("/definitions")
    @PreAuthorize("hasAnyRole('IT_OPS','READ_ONLY','PLANNER')")
    public List<Map<String, Object>> definitions() {
        return jdbc.queryForList(
            "SELECT kpi_code, category, description, threshold, unit, target_dir "
                + "FROM business_kpi.definition ORDER BY category, kpi_code");
    }
}
