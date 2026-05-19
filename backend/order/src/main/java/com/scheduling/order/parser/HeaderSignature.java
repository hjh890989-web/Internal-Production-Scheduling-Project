package com.scheduling.order.parser;

import java.util.List;

/**
 * SourceType 식별 시그니처 — TK-01-1-2.
 *
 * <p>{@code requiredAny} 키워드 중 1개 이상 매칭 → score 가산.
 * {@code excluded} 키워드 1개라도 매칭 → score 0 (강제 제외).
 *
 * @param requiredAny 매칭 키워드 (대소문자 무시·부분 일치)
 * @param excluded    제외 키워드 (cross-source 오분류 방지)
 * @param weight      매칭 1건당 가중치 (기본 1.0, KD 같은 강한 시그니처는 1.5)
 */
public record HeaderSignature(
    List<String> requiredAny,
    List<String> excluded,
    double weight
) {
    public HeaderSignature {
        requiredAny = requiredAny == null ? List.of() : List.copyOf(requiredAny);
        excluded    = excluded    == null ? List.of() : List.copyOf(excluded);
        if (weight <= 0) weight = 1.0;
    }
}
