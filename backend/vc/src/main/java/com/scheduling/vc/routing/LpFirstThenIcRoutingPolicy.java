package com.scheduling.vc.routing;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LP 우선 라우팅 정책 — TK-05-4-1 (BR-V08 / ADR-006 성형판 기본).
 *
 * <p>{@code prioritize()} 항상 [LP, IC] 반환. GreedyRotationAllocator 가 LP 슬롯 시도 후
 * yield 없거나 포화 시 IC 폴백.
 */
@Component
public class LpFirstThenIcRoutingPolicy implements MachineTypeRoutingPolicy {

    public static final String POLICY_ID = "LP_FIRST";

    @Override
    public List<MachineType> prioritize(String hoseId, RoutingContext ctx) {
        return List.of(MachineType.LP, MachineType.IC);
    }

    @Override
    public String policyId() {
        return POLICY_ID;
    }
}
