package com.scheduling.order.parser;

import java.util.Map;

/**
 * 워크북 분류 결과 — TK-01-1-2.
 *
 * @param sourceType  분류 결과 (UNRECOGNIZED 포함)
 * @param confidence  매칭 점수 0.0~1.0 — 0.5 미만 = UNRECOGNIZED
 * @param allScores   디버깅·근거 추적용 모든 SourceType 점수
 */
public record ClassificationResult(
    SourceType sourceType,
    double confidence,
    Map<SourceType, Double> allScores
) {
    public ClassificationResult {
        allScores = allScores == null ? Map.of() : Map.copyOf(allScores);
    }
}
