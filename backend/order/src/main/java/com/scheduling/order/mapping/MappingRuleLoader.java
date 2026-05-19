package com.scheduling.order.mapping;

import com.scheduling.order.domain.OrderType;
import com.scheduling.order.parser.SourceType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SourceType별 YAML 매핑 룰셋 로더 — TK-01-2-1.
 *
 * <p>{@code classpath:mapping/rules-<SourceType>.yaml} 4 파일 PostConstruct 로 로드.
 * SRS-RSK-007 (포맷 분화) 완화 — 영업 양식 변경 시 YAML 만 갱신, 코드 수정 없음.
 *
 * @see SchemaMappingService
 */
@Component
public class MappingRuleLoader {

    private static final Logger log = LoggerFactory.getLogger(MappingRuleLoader.class);
    private static final String RULES_PREFIX = "mapping/rules-";
    private static final String RULES_SUFFIX = ".yaml";

    private final Map<SourceType, MappingRule> rules = new EnumMap<>(SourceType.class);

    @PostConstruct
    public void load() {
        rules.clear();
        for (SourceType st : SourceType.values()) {
            if (st == SourceType.UNRECOGNIZED) {
                continue;
            }
            MappingRule rule = loadOne(st);
            if (rule != null) {
                rules.put(st, rule);
            }
        }
        log.info("Loaded {} mapping rules: {}", rules.size(), rules.keySet());
    }

    public MappingRule loadRuleFor(SourceType sourceType) {
        MappingRule rule = rules.get(sourceType);
        if (rule == null) {
            throw new IllegalStateException("No mapping rule for SourceType: " + sourceType);
        }
        return rule;
    }

    /** 운영자 YAML 변경 후 호출 — REST endpoint 노출은 Sprint 1+ 별도 Task. */
    public void reload() {
        load();
    }

    private MappingRule loadOne(SourceType sourceType) {
        Resource resource = new ClassPathResource(RULES_PREFIX + sourceType.name() + RULES_SUFFIX);
        if (!resource.exists()) {
            log.warn("Mapping rule file missing for {}: {}", sourceType, resource.getDescription());
            return null;
        }
        try (InputStream is = resource.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(is);
            return parseRule(sourceType, root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load mapping rule: " + resource.getDescription(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private MappingRule parseRule(SourceType sourceType, Map<String, Object> root) {
        OrderType defaultOrderType = OrderType.valueOf(asString(root.get("defaultOrderType"), "CONFIRMED"));
        String defaultCustomer = asString(root.get("defaultCustomer"), "내수");

        List<Integer> headerRowCandidates = (List<Integer>) root.getOrDefault(
            "headerRowCandidates", List.of(0, 1, 2));

        Map<String, Object> fieldsRaw = (Map<String, Object>) root.getOrDefault("fields", Map.of());
        Map<String, FieldMapping> fields = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : fieldsRaw.entrySet()) {
            String standardField = entry.getKey();
            Map<String, Object> fieldData = (Map<String, Object>) entry.getValue();
            fields.put(standardField, parseField(standardField, fieldData));
        }

        return new MappingRule(sourceType, defaultOrderType, fields, headerRowCandidates, defaultCustomer);
    }

    @SuppressWarnings("unchecked")
    private FieldMapping parseField(String name, Map<String, Object> data) {
        List<String> aliases = (List<String>) data.getOrDefault("aliases", List.of());
        List<String> dateFormatHints = data.containsKey("dateFormatHints")
            ? new ArrayList<>((List<String>) data.get("dateFormatHints"))
            : List.of();
        boolean coerceInteger = data.get("coerceInteger") instanceof Boolean b && b;
        String regexStrip = asString(data.get("regexStrip"), "");
        boolean toUpperCase = data.get("toUpperCase") instanceof Boolean b2 && b2;
        String defaultValue = asString(data.get("defaultValue"), null);
        boolean required = !(data.get("required") instanceof Boolean req) || req;

        return new FieldMapping(name, aliases, dateFormatHints, coerceInteger, regexStrip,
            toUpperCase, defaultValue, required);
    }

    private String asString(Object obj, String fallback) {
        return obj == null ? fallback : obj.toString();
    }

    Map<SourceType, MappingRule> rules() {
        return rules;
    }
}
