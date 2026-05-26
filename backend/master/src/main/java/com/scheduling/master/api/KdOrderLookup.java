package com.scheduling.master.api;

import java.util.List;
import java.util.UUID;

/**
 * BR-V13 KD_ORDER facade — Sprint 7 (REQ-FUNC-VC-023).
 *
 * <p>vc 모듈 {@code KdSupplementService} 가 본 인터페이스로 잔량 조회 + 차감.
 * <ol>
 *   <li>{@link #findOpenByHose} — 동일 hose 1차 우선순위</li>
 *   <li>{@link #findOpenByHoseIn} — 동일 셋팅 그룹 hose 들 2차 우선순위</li>
 *   <li>{@link #consume} — atomic 차감 + status auto-update + audit reason 영속</li>
 * </ol>
 */
public interface KdOrderLookup {

    List<KdOrderSummary> findOpenByHose(String hoseId);

    List<KdOrderSummary> findOpenByHoseIn(List<String> hoseIds);

    /**
     * @return 실 차감량 (remaining 부족 시 가능량만 — caller 가 잔여 부족 처리)
     */
    int consume(UUID kdOrderId, int qty, String actor);

    /**
     * Sprint 8 EP-V13-Grafana — hose 별 OPEN+PARTIAL remaining_qty 합계.
     *
     * @return hose_id → total remaining qty (잔량 0 hose 는 결과에서 제외)
     */
    java.util.Map<String, Long> remainingByHose();
}
