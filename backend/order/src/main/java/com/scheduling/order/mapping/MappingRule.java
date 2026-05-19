package com.scheduling.order.mapping;

import com.scheduling.order.domain.OrderType;
import com.scheduling.order.parser.SourceType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SourceType별 매핑 룰셋 — TK-01-2-1.
 *
 * <p>{@code rules-<SourceType>.yaml} 파일에서 로드. {@link MappingRuleLoader} 가 생성.
 *
 * @param sourceType            적용 대상 SourceType (MONTHLY_FORECAST 등)
 * @param defaultOrderType      OrderType 기본값 (qty 매핑 시 자동 적용)
 * @param fields                필드명 → FieldMapping 인덱스
 * @param headerRowCandidates   헤더 행 후보 0-based row index (예: [0, 1, 2])
 * @param defaultCustomer       customer 필드 기본값
 */
public record MappingRule(
    SourceType sourceType,
    OrderType defaultOrderType,
    Map<String, FieldMapping> fields,
    List<Integer> headerRowCandidates,
    String defaultCustomer
) {
    public MappingRule {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        headerRowCandidates = headerRowCandidates == null ? List.of(0, 1, 2) : List.copyOf(headerRowCandidates);
        if (defaultCustomer == null || defaultCustomer.isBlank()) {
            defaultCustomer = "내수";
        }
    }

    public FieldMapping field(String standardField) {
        return fields.get(standardField);
    }

    /**
     * 헤더 row 의 셀 값 (canonical) → 표준 필드명.
     * 매치 없으면 null. 대소문자·공백·언더스코어 무시 (양쪽 모두 canonical 처리).
     */
    public String resolveAliasToField(String canonicalCell) {
        if (canonicalCell == null) return null;
        for (FieldMapping fm : fields.values()) {
            for (String alias : fm.aliases()) {
                if (canonicalizeAlias(alias).equals(canonicalCell)) {
                    return fm.standardField();
                }
            }
        }
        return null;
    }

    /** alias 도 헤더 셀과 같은 방식으로 canonicalize — 공백·언더스코어 제거 + 대문자. */
    private static String canonicalizeAlias(String alias) {
        if (alias == null) return "";
        return alias.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s_]+", "");
    }
}
