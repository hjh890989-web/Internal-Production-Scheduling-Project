package com.scheduling.vc.deadline;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * 품번별 D-2 deadline — TK-06-1-2 (REQ-FUNC-VC-008 / BR-X07).
 *
 * <p>같은 hose_id 에 여러 수주가 있으면 가장 이른 납기 기준 deadline (hard 제약).
 * 불변 — {@link BackwardDeadlineCalculator} 가 {@code Map.copyOf} 로 생성.
 */
public record DeadlineMap(Map<String, LocalDate> map) {

    public DeadlineMap {
        map = map == null ? Map.of() : Map.copyOf(map);
    }

    public Optional<LocalDate> get(String hoseId) {
        return Optional.ofNullable(map.get(hoseId));
    }

    /**
     * production_date 가 본 hose 의 deadline 이내인가.
     * deadline 미등록 hose → true (검증 미적용).
     */
    public boolean isWithinDeadline(String hoseId, LocalDate productionDate) {
        return get(hoseId).map(d -> !productionDate.isAfter(d)).orElse(true);
    }
}
