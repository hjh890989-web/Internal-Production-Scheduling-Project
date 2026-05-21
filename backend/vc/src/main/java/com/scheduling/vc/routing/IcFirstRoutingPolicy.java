package com.scheduling.vc.routing;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * IC 우선 라우팅 정책 — TK-05-4-1 (대안, 특수 운영 시).
 *
 * <p>{@code scheduling.routing.active-policy=IC_FIRST} 설정 시 활성.
 */
@Component
public class IcFirstRoutingPolicy implements MachineTypeRoutingPolicy {

    public static final String POLICY_ID = "IC_FIRST";

    @Override
    public List<MachineType> prioritize(String hoseId, RoutingContext ctx) {
        return List.of(MachineType.IC, MachineType.LP);
    }

    @Override
    public String policyId() {
        return POLICY_ID;
    }
}
