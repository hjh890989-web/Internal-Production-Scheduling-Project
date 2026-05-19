package com.scheduling.order.mapping;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.parser.ParsedRow;
import com.scheduling.order.parser.ParsedSheet;
import com.scheduling.order.parser.ParsedWorkbook;
import com.scheduling.order.parser.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema mapping engine — TK-01-2-1.
 *
 * <p>{@link ParsedWorkbook} + {@link SourceType} → {@link MappingResult}.
 * SourceType별 룰셋 ({@link MappingRule}) 적용 → 표준 {@link OrderDraft} 6 필드.
 *
 * <p>알고리즘:
 * <ol>
 *   <li>SourceType → MappingRule 로드 (캐시됨)</li>
 *   <li>각 sheet 의 headerRowCandidates 중 매칭되는 헤더 row 자동 감지</li>
 *   <li>헤더 셀 → 표준 필드명 매핑 (alias 기반)</li>
 *   <li>각 data row 매핑 시도 — 성공/실패 분리</li>
 *   <li>K-O03 메트릭 emit (Prometheus → Grafana 패널)</li>
 * </ol>
 *
 * <p>REQ-FUNC-OC-003 자동 매핑 ≥95% / REQ-FUNC-OC-004 보정 워크플로우 (실패 1%↑) / NFR-USA-002 사유 명시.
 */
@Service
public class SchemaMappingService {

    private static final Logger log = LoggerFactory.getLogger(SchemaMappingService.class);

    private final MappingRuleLoader ruleLoader;
    private final FieldNormalizer normalizer;
    private final SchedulingMetrics metrics;

    public SchemaMappingService(
        MappingRuleLoader ruleLoader,
        FieldNormalizer normalizer,
        SchedulingMetrics metrics
    ) {
        this.ruleLoader = ruleLoader;
        this.normalizer = normalizer;
        this.metrics = metrics;
    }

    public MappingResult map(ParsedWorkbook workbook, SourceType sourceType) {
        if (sourceType == SourceType.UNRECOGNIZED) {
            return new MappingResult(List.of(), List.of(
                new MappingFailure("(전체)", -1, List.of(), "HEADER",
                    "UNRECOGNIZED SourceType — 분류기 confidence 미달, 사용자 확인 필요")
            ), sourceType);
        }

        MappingRule rule = ruleLoader.loadRuleFor(sourceType);
        List<OrderDraft> successes = new ArrayList<>();
        List<MappingFailure> failures = new ArrayList<>();

        for (ParsedSheet sheet : workbook.sheets()) {
            int headerRowIdx;
            Map<String, Integer> columnIndex;
            try {
                headerRowIdx = detectHeaderRow(sheet, rule);
                columnIndex = buildColumnIndex(sheet.row(headerRowIdx), rule);
            } catch (MappingException e) {
                failures.add(new MappingFailure(
                    sheet.name(), -1, List.of(), e.getField(), e.getReason()));
                continue;
            }

            for (int rowIdx = headerRowIdx + 1; rowIdx < sheet.rowCount(); rowIdx++) {
                ParsedRow row = sheet.row(rowIdx);
                if (row.isEmpty()) {
                    continue;
                }
                try {
                    successes.add(mapRow(row, columnIndex, rule));
                } catch (MappingException e) {
                    failures.add(new MappingFailure(
                        sheet.name(), rowIdx, row.cells(), e.getField(), e.getReason()));
                }
            }
        }

        emitMetrics(sourceType, successes.size(), failures.size());

        int total = successes.size() + failures.size();
        log.info("Mapping for {}: {}/{} success ({:.1f}%)", sourceType, successes.size(), total,
            total == 0 ? 0.0 : 100.0 * successes.size() / total);

        return new MappingResult(successes, failures, sourceType);
    }

    private int detectHeaderRow(ParsedSheet sheet, MappingRule rule) {
        for (Integer candidate : rule.headerRowCandidates()) {
            if (candidate == null || candidate < 0 || candidate >= sheet.rowCount()) {
                continue;
            }
            ParsedRow row = sheet.row(candidate);
            for (String cell : row.cells()) {
                if (rule.resolveAliasToField(normalizer.canonical(cell)) != null) {
                    return candidate;
                }
            }
        }
        throw new MappingException("HEADER",
            "헤더 row 찾기 실패 (시트=%s, candidates=%s)".formatted(
                sheet.name(), rule.headerRowCandidates()));
    }

    private Map<String, Integer> buildColumnIndex(ParsedRow header, MappingRule rule) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String canonical = normalizer.canonical(header.cell(i));
            String standardField = rule.resolveAliasToField(canonical);
            if (standardField != null) {
                map.putIfAbsent(standardField, i);
            }
        }
        return map;
    }

    private OrderDraft mapRow(ParsedRow row, Map<String, Integer> idx, MappingRule rule) {
        FieldMapping hoseRule = rule.field("hose_id");
        FieldMapping dateRule = rule.field("delivery_date");
        FieldMapping qtyRule = rule.field("qty");
        FieldMapping custRule = rule.field("customer");

        String hoseId = normalizer.normalizeHoseId(
            cellAt(row, idx, "hose_id", true),
            hoseRule == null ? "" : hoseRule.regexStrip(),
            hoseRule == null || hoseRule.toUpperCase());

        LocalDate deliveryDate = normalizer.parseDate(
            cellAt(row, idx, "delivery_date", true),
            dateRule == null ? List.of("yyyy-MM-dd") : dateRule.dateFormatHints(),
            true);

        int qty = normalizer.parseInt(cellAt(row, idx, "qty", true), true);

        String customer = cellAt(row, idx, "customer", false);
        if (customer == null || customer.isBlank()) {
            customer = custRule != null && custRule.defaultValue() != null
                ? custRule.defaultValue()
                : rule.defaultCustomer();
        }

        return new OrderDraft(null, hoseId, deliveryDate, qty, rule.defaultOrderType(), customer);
    }

    private String cellAt(ParsedRow row, Map<String, Integer> idx, String field, boolean required) {
        Integer i = idx.get(field);
        if (i == null) {
            if (required) throw new MappingException(field, "표준 필드 컬럼 없음 (헤더에서 미발견)");
            return null;
        }
        return row.cell(i);
    }

    private void emitMetrics(SourceType sourceType, int success, int failed) {
        int total = success + failed;
        if (total == 0) return;
        // K-O03 KPI — success rate (gauge). Counter 는 누적 통계.
        metrics.increment("order_mapping", "total_" + sourceType.name().toLowerCase());
        for (int i = 0; i < success; i++) {
            metrics.increment("order_mapping", "success_" + sourceType.name().toLowerCase());
        }
        for (int i = 0; i < failed; i++) {
            metrics.increment("order_mapping", "failed_" + sourceType.name().toLowerCase());
        }
    }
}
