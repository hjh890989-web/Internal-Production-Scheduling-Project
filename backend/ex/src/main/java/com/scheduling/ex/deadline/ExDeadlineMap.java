package com.scheduling.ex.deadline;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * 품번별 압출 D-1 deadline — TK-07-1-2 (REQ-FUNC-EX-001 / BR-E01).
 *
 * <p>같은 hose_id 다중 vc_date 있을 시 가장 이른 vc_date 기준 deadline (hard 제약).
 * 불변 — {@link BackwardExtrusionCalculator} 가 {@code Map.copyOf} 로 생성.
 */
public record ExDeadlineMap(Map<String, LocalDate> map) {

    public ExDeadlineMap {
        map = map == null ? Map.of() : Map.copyOf(map);
    }

    public Optional<LocalDate> get(String hoseId) {
        return Optional.ofNullable(map.get(hoseId));
    }

    /**
     * production_date 가 본 hose 의 deadline 이내인가.
     * deadline 미등록 hose → true (검증 미적용).
     */
    public boolean isWithinDeadline(String hoseId, LocalDate exProductionDate) {
        return get(hoseId).map(d -> !exProductionDate.isAfter(d)).orElse(true);
    }
}
