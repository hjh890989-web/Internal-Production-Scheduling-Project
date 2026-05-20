package com.scheduling.order.domain;

import java.util.List;

/**
 * 중복 후보 그룹 — TK-02-1-3.
 *
 * <p>{@link DuplicateDetectionService} 가 batch 입력 내 또는 기존 마스터와 충돌하는 후보 묶음 산출.
 *
 * @param key             복합 키 (hose_id, delivery_date)
 * @param candidates      신규 batch 내 후보 (1+ 개)
 * @param existingMaster  기존 ACTIVE 마스터 row (없으면 null)
 */
public record DuplicateGroup(
    OrderKey key,
    List<OrderDraft> candidates,
    Order existingMaster
) {
    public DuplicateGroup {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public boolean hasExisting() {
        return existingMaster != null;
    }

    public int candidateCount() {
        return candidates.size();
    }

    /** batch 내 중복 (≥2 후보) 또는 기존 마스터 충돌 시 true. */
    public boolean isDuplicate() {
        return candidates.size() > 1 || existingMaster != null;
    }
}
