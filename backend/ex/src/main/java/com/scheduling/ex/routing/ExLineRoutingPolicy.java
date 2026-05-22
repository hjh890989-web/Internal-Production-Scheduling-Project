package com.scheduling.ex.routing;

import com.scheduling.master.api.LineRoutingLookup;
import com.scheduling.master.api.LineTypeSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 압출 라인 라우팅 정책 — TK-14-1-1·2·3 (EP-14 ST-14-1, BR-E08).
 *
 * <p><b>알고리즘</b>:
 * <ol>
 *   <li>포드 전용 품번 → FORD 라인만 (신규 시도 0건)</li>
 *   <li>일반 품번 → NEW priority ASC 우선, FORD fallback</li>
 *   <li>비활성 라인 자동 skip</li>
 * </ol>
 *
 * <p><b>NS-S09 KPI</b>: 1주 호라이즌 신규 라인 사용률 ≥ 90%.
 */
@Component
@Profile("with-infra")
public class ExLineRoutingPolicy {

    private static final Logger log = LoggerFactory.getLogger(ExLineRoutingPolicy.class);

    private final LineRoutingLookup lookup;

    public ExLineRoutingPolicy(LineRoutingLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * 후보 라인 우선순위 결정.
     *
     * @param hoseId 품번
     * @return 우선순위 정렬된 line IDs (NEW 먼저, FORD 나중)
     */
    public List<String> prioritize(String hoseId) {
        List<LineTypeSummary> active = lookup.findAllActive();
        boolean fordOnly = lookup.isFordOnly(hoseId);

        if (fordOnly) {
            // 포드 전용 — FORD 라인만
            List<String> fordLines = active.stream()
                .filter(LineTypeSummary::isFord)
                .map(LineTypeSummary::lineId).toList();
            log.debug("hose={} ford_only=true → FORD 라인 {} (신규 시도 0)", hoseId, fordLines);
            return fordLines;
        }

        // 일반 — NEW priority ASC 먼저, FORD nachträglich
        List<String> ordered = active.stream()
            .sorted((a, b) -> {
                if (a.isNew() && b.isFord()) return -1;
                if (a.isFord() && b.isNew()) return 1;
                return Short.compare(a.priority(), b.priority());
            })
            .map(LineTypeSummary::lineId).toList();
        return ordered;
    }

    /**
     * 본 hose 의 ford_only 여부.
     */
    public boolean isFordOnly(String hoseId) {
        return lookup.isFordOnly(hoseId);
    }
}
