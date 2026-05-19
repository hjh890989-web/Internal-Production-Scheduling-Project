package com.scheduling.order.mapping;

import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.parser.SourceType;

import java.util.List;

/**
 * Schema mapping 결과 — TK-01-2-1.
 *
 * @param successes  성공 매핑 row 의 OrderDraft list
 * @param failures   실패 row 의 MappingFailure list (TK-01-2-2 보정 UI 입력)
 * @param sourceType 입력 워크북의 SourceType (룰셋 trace)
 */
public record MappingResult(
    List<OrderDraft> successes,
    List<MappingFailure> failures,
    SourceType sourceType
) {
    public MappingResult {
        successes = successes == null ? List.of() : List.copyOf(successes);
        failures = failures == null ? List.of() : List.copyOf(failures);
    }

    public int totalRows() {
        return successes.size() + failures.size();
    }

    /** REQ-FUNC-OC-003 자동 매핑 성공률 (K-O03 KPI). */
    public double successRate() {
        int total = totalRows();
        return total == 0 ? 0.0 : (double) successes.size() / total;
    }

    /** REQ-FUNC-OC-004 임계 — 매핑 실패율 ≥1% 시 보정 모달 필요. */
    public boolean requiresReviewModal() {
        int total = totalRows();
        if (total == 0) return false;
        return (double) failures.size() / total >= 0.01;
    }
}
