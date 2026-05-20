package com.scheduling.order.domain;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * BR-O01 우선순위 해소 엔진 — TK-02-2-1 (REQ-FUNC-OC-006).
 *
 * <p>알고리즘:
 * <ol>
 *   <li>{@link DuplicateGroup#candidates()} 중 ordinal 최대 → 잠정 winner</li>
 *   <li>{@link DuplicateGroup#existingMaster()} 가 있으면 winner 와 비교</li>
 *   <li>기존이 더 강함 → {@link Resolution.Decision#KEPT_EXISTING} (winner = existing)</li>
 *   <li>신규가 더 강함 → {@link Resolution.Decision#REPLACED_EXISTING}</li>
 *   <li>기존 없음 → {@link Resolution.Decision#NEW_WINS}</li>
 *   <li>동률 시 입력 순서 first-come (Sprint 1+ tiebreak 정책 별도 정의)</li>
 * </ol>
 *
 * <p>본 컴포넌트는 순수 함수 — Profile 게이팅 불필요 (테스트 직접 호출 가능).
 *
 * @see Resolution
 * @see OrderType#precedenceRank
 */
@Component
public class PrecedenceResolver {

    public Resolution resolve(DuplicateGroup group) {
        Objects.requireNonNull(group, "DuplicateGroup null");

        OrderDraft strongestCandidate = strongest(group.candidates());

        if (!group.hasExisting()) {
            // 기존 마스터 없음 — 신규 winner
            List<OrderDraft> losers = othersOf(group.candidates(), strongestCandidate);
            return new Resolution(
                group.key(),
                strongestCandidate,
                losers,
                null,
                Resolution.Decision.NEW_WINS
            );
        }

        OrderType existingType = group.existingMaster().getOrderType();
        if (strongestCandidate == null
            || existingType.isAtLeastAsStrongAs(strongestCandidate.orderType())) {
            // 기존 마스터 유지 — 신규는 모두 losers
            return new Resolution(
                group.key(),
                OrderDraft.fromExisting(group.existingMaster()),
                group.candidates(),
                group.existingMaster(),
                Resolution.Decision.KEPT_EXISTING
            );
        }

        // 신규가 기존보다 강함 — 교체
        List<OrderDraft> losers = othersOf(group.candidates(), strongestCandidate);
        return new Resolution(
            group.key(),
            strongestCandidate,
            losers,
            group.existingMaster(),
            Resolution.Decision.REPLACED_EXISTING
        );
    }

    /** candidates 중 ordinal 최대 — 동률 시 first-come (입력 순서 첫 등장). */
    private OrderDraft strongest(List<OrderDraft> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        OrderDraft winner = candidates.get(0);
        for (int i = 1; i < candidates.size(); i++) {
            OrderDraft d = candidates.get(i);
            if (d.orderType().isStrongerThan(winner.orderType())) {
                winner = d;
            }
        }
        return winner;
    }

    /** winner 를 제외한 나머지 (identity 비교 — record equals 회피). */
    private List<OrderDraft> othersOf(List<OrderDraft> candidates, OrderDraft winner) {
        List<OrderDraft> losers = new ArrayList<>(candidates.size());
        boolean winnerSkipped = false;
        for (OrderDraft d : candidates) {
            if (!winnerSkipped && d == winner) {
                winnerSkipped = true;
                continue;
            }
            losers.add(d);
        }
        return losers;
    }
}
