package com.scheduling.order.domain;

import java.util.List;

/**
 * 중복 그룹 우선순위 해소 결과 — TK-02-2-1 (REQ-FUNC-OC-006).
 *
 * @param key             복합 키
 * @param winner          정본 (DB persist 대상 또는 유지 결정)
 * @param losers          신규 후보 중 패자 (archive 후보)
 * @param existingMaster  기존 ACTIVE 마스터 (있으면, 없으면 null)
 * @param decision        해소 의사결정
 */
public record Resolution(
    OrderKey key,
    OrderDraft winner,
    List<OrderDraft> losers,
    Order existingMaster,
    Decision decision
) {
    public Resolution {
        losers = losers == null ? List.of() : List.copyOf(losers);
    }

    public boolean hasExisting() {
        return existingMaster != null;
    }

    /**
     * 우선순위 해소 의사결정 — BR-O01.
     */
    public enum Decision {
        /** 기존 마스터가 신규보다 강해 유지 (winner = existing). 신규는 모두 archive. */
        KEPT_EXISTING,
        /** 신규가 기존 마스터보다 강해 교체 (winner = 신규 중 가장 강함). */
        REPLACED_EXISTING,
        /** 기존 마스터 없음 — 신규 중 가장 강한 후보가 winner. */
        NEW_WINS
    }
}
