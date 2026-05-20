package com.scheduling.order.mapping;

import com.scheduling.order.domain.OrderType;
import com.scheduling.order.parser.SourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MappingRuleService 회귀 — TK-01-2-3.
 *
 * <p>주요 검증:
 *  - YAML 룰 조회 + 메모리 캐싱
 *  - PUT 갱신 — 필수 필드 검증·SourceType 일치
 *  - 잘못된 입력 거부 (IllegalArgumentException)
 *  - reloadFromYaml — 캐시 reset
 */
class MappingRuleServiceTest {

    private MappingRuleLoader loader;
    private MappingRuleService service;

    @BeforeEach
    void setUp() {
        loader = new MappingRuleLoader();
        loader.load();
        service = new MappingRuleService(loader);
    }

    @Test
    @DisplayName("getRule — YAML 룰 조회 + 캐시 (두 번째 호출 동일 instance)")
    void get_rule_caches() {
        MappingRule first = service.getRule(SourceType.MONTHLY_FORECAST);
        MappingRule second = service.getRule(SourceType.MONTHLY_FORECAST);
        assertThat(first).isSameAs(second);
        assertThat(first.sourceType()).isEqualTo(SourceType.MONTHLY_FORECAST);
        assertThat(first.defaultOrderType()).isEqualTo(OrderType.FORECAST);
    }

    @Test
    @DisplayName("updateRule — 필수 필드(hose_id/delivery_date/qty) 누락 거부")
    void update_rule_missing_required_field() {
        MappingRule incomplete = new MappingRule(
            SourceType.MONTHLY_FORECAST,
            OrderType.FORECAST,
            Map.of("hose_id", new FieldMapping("hose_id", List.of("품번"), List.of(), false, "", true, null, true)),
            List.of(0),
            "내수"
        );
        assertThatThrownBy(() -> service.updateRule(SourceType.MONTHLY_FORECAST, incomplete, "actor"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("필수 필드 누락");
    }

    @Test
    @DisplayName("updateRule — path / body SourceType 불일치 거부")
    void update_rule_source_type_mismatch() {
        MappingRule mismatched = new MappingRule(
            SourceType.WEEKLY_PLAN,
            OrderType.WEEKLY,
            allRequiredFields(),
            List.of(0),
            "내수"
        );
        assertThatThrownBy(() -> service.updateRule(SourceType.MONTHLY_FORECAST, mismatched, "actor"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SourceType 불일치");
    }

    @Test
    @DisplayName("updateRule — fields 비어있음 거부")
    void update_rule_empty_fields() {
        MappingRule empty = new MappingRule(
            SourceType.MONTHLY_FORECAST,
            OrderType.FORECAST,
            Map.of(),
            List.of(0),
            "내수"
        );
        assertThatThrownBy(() -> service.updateRule(SourceType.MONTHLY_FORECAST, empty, "actor"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fields 비어 있음");
    }

    @Test
    @DisplayName("updateRule — null 룰 거부")
    void update_rule_null() {
        assertThatThrownBy(() -> service.updateRule(SourceType.MONTHLY_FORECAST, null, "actor"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("null");
    }

    @Test
    @DisplayName("updateRule — 정상 갱신 + 즉시 반영 (다음 getRule 새 룰)")
    void update_rule_success_in_memory() {
        MappingRule newRule = new MappingRule(
            SourceType.MONTHLY_FORECAST,
            OrderType.FORECAST,
            allRequiredFields(),
            List.of(0, 1),
            "내수"
        );
        MappingRule returned = service.updateRule(SourceType.MONTHLY_FORECAST, newRule, "test-actor");
        assertThat(returned).isSameAs(newRule);

        // 다음 조회 → 새 룰 반환 (YAML 미 reload, 메모리 캐시 우선)
        MappingRule fetched = service.getRule(SourceType.MONTHLY_FORECAST);
        assertThat(fetched).isSameAs(newRule);
        assertThat(fetched.headerRowCandidates()).containsExactly(0, 1);
    }

    @Test
    @DisplayName("reloadFromYaml — 캐시 reset 후 YAML 재로드")
    void reload_from_yaml_clears_cache() {
        // 메모리 룰 우선 등록
        MappingRule custom = new MappingRule(
            SourceType.KD_ORDER,
            OrderType.KD,
            allRequiredFields(),
            List.of(2),
            "별도"
        );
        service.updateRule(SourceType.KD_ORDER, custom, "actor");
        assertThat(service.getRule(SourceType.KD_ORDER).defaultCustomer()).isEqualTo("별도");

        // reload — YAML 룰셋 (KD) 의 defaultCustomer = "KD" 로 복원
        service.reloadFromYaml();
        assertThat(service.getRule(SourceType.KD_ORDER).defaultCustomer()).isEqualTo("KD");
    }

    @Test
    @DisplayName("snapshot — 현재 활성 룰 map 반환")
    void snapshot_returns_active_rules() {
        service.getRule(SourceType.MONTHLY_FORECAST);
        service.getRule(SourceType.KD_ORDER);
        Map<SourceType, MappingRule> snap = service.snapshot();
        assertThat(snap).containsKeys(SourceType.MONTHLY_FORECAST, SourceType.KD_ORDER);
    }

    // ---------- helpers ----------
    private Map<String, FieldMapping> allRequiredFields() {
        return Map.of(
            "hose_id", new FieldMapping("hose_id", List.of("품번"), List.of(), false, "", true, null, true),
            "delivery_date", new FieldMapping("delivery_date", List.of("납기"),
                List.of("yyyy-MM-dd"), false, "", false, null, true),
            "qty", new FieldMapping("qty", List.of("수량"), List.of(), true, "", false, null, true)
        );
    }
}
