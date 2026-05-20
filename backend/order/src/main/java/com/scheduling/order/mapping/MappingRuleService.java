package com.scheduling.order.mapping;

import com.scheduling.order.parser.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 매핑 룰 운영 서비스 — TK-01-2-3.
 *
 * <p>{@link MappingRuleLoader} 가 부팅 시 YAML 파일을 로드하면, 본 서비스가 메모리 캐시로 보존.
 * 사용자(@Role PLANNER) 가 PUT 으로 룰 변경 시 즉시 반영 (재시작 X).
 * 변경은 BR-X02 audit log 남김 — Sprint 1+ EP-11 audit 모듈 활성 후 실제 DB persist.
 *
 * <p>본 Sprint 1 baseline 의 audit 는 SLF4J 로 로그 — Sprint 1+ AuditPublisher 연동.
 */
@Service
public class MappingRuleService {

    private static final Logger log = LoggerFactory.getLogger(MappingRuleService.class);
    private static final List<String> REQUIRED_FIELDS = List.of("hose_id", "delivery_date", "qty");

    private final MappingRuleLoader loader;
    private final Map<SourceType, MappingRule> liveCache = new ConcurrentHashMap<>();

    public MappingRuleService(MappingRuleLoader loader) {
        this.loader = loader;
    }

    public MappingRule getRule(SourceType sourceType) {
        return liveCache.computeIfAbsent(sourceType, loader::loadRuleFor);
    }

    /**
     * 룰 변경 — 메모리 캐시 갱신 + audit 기록.
     *
     * @param sourceType 변경 대상
     * @param updated    새 룰
     * @param actor      변경자 (Sprint 1+ JWT subject)
     * @return 갱신된 룰
     * @throws IllegalArgumentException 필수 필드 누락 또는 SourceType 불일치
     */
    public MappingRule updateRule(SourceType sourceType, MappingRule updated, String actor) {
        validate(sourceType, updated);
        MappingRule before = liveCache.get(sourceType);
        liveCache.put(sourceType, updated);
        auditRuleChange(sourceType, before, updated, actor);
        return updated;
    }

    /** 모든 SourceType 룰을 YAML 에서 다시 로드 (메모리 캐시 reset). */
    public void reloadFromYaml() {
        loader.reload();
        liveCache.clear();
        log.info("MappingRule 캐시 reset — 다음 호출 시 YAML 재로드");
    }

    /** 테스트·진단용 — 현재 활성 룰 snapshot. */
    public Map<SourceType, MappingRule> snapshot() {
        Map<SourceType, MappingRule> snap = new EnumMap<>(SourceType.class);
        snap.putAll(liveCache);
        return snap;
    }

    private void validate(SourceType sourceType, MappingRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("룰셋 null");
        }
        if (rule.sourceType() != sourceType) {
            throw new IllegalArgumentException(
                "SourceType 불일치: path=%s, body=%s".formatted(sourceType, rule.sourceType()));
        }
        if (rule.fields() == null || rule.fields().isEmpty()) {
            throw new IllegalArgumentException("fields 비어 있음");
        }
        for (String required : REQUIRED_FIELDS) {
            if (!rule.fields().containsKey(required)) {
                throw new IllegalArgumentException("필수 필드 누락: " + required);
            }
        }
    }

    private void auditRuleChange(SourceType sourceType, MappingRule before, MappingRule after, String actor) {
        // BR-X02 — 모든 mutation 강제 audit. Sprint 1+ EP-11 audit 모듈 활성 후 DB persist.
        // 본 Sprint 1 baseline 은 SLF4J 로 audit 트레일 유지 (Loki 90일 보존).
        log.info("AUDIT mapping_rule {} updated by {} — beforeSize={}, afterSize={}",
            sourceType, actor,
            before == null ? 0 : before.fields().size(),
            after.fields().size());
    }
}
