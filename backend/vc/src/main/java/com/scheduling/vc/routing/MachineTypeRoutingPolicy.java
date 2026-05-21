package com.scheduling.vc.routing;

import java.util.List;

/**
 * 머신 유형 우선순위 결정 정책 — TK-05-4-1 (BR-V08 / ADR-006).
 *
 * <p>{@code com.scheduling.vc.allocator.GreedyRotationAllocator} 가 정책 인터페이스를 통해
 * LP/IC 우선순위 결정 — 코드 결합 분리. application.yaml {@code scheduling.routing.active-policy}
 * 로 정책 변경 가능.
 *
 * <p>3 정책 후보:
 * <ul>
 *   <li>{@code LP_FIRST} — BR-V08 기본 (저압 우선)</li>
 *   <li>{@code IC_FIRST} — IC 우선 (특수 운영 시)</li>
 *   <li>{@code BALANCED} — 가동률 기반 라운드로빈 (Phase 2+)</li>
 * </ul>
 */
public interface MachineTypeRoutingPolicy {

    /**
     * 주어진 hose_id 의 머신 유형 우선순위.
     * 첫 번째 원소부터 시도 → yield 없거나 슬롯 포화 시 다음 시도.
     */
    List<MachineType> prioritize(String hoseId, RoutingContext ctx);

    /** 정책 식별자 — application.yaml 값 + audit policy_id. */
    String policyId();
}
