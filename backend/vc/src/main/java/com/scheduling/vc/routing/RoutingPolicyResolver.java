package com.scheduling.vc.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 활성 라우팅 정책 resolver — TK-05-4-1.
 *
 * <p>{@link RoutingPolicyConfig#getActivePolicy()} 값으로 등록된 정책 중 매칭 선택.
 * 매칭 실패 시 default {@link LpFirstThenIcRoutingPolicy}.
 */
@Component
@EnableConfigurationProperties(RoutingPolicyConfig.class)
public class RoutingPolicyResolver {

    private static final Logger log = LoggerFactory.getLogger(RoutingPolicyResolver.class);

    private final List<MachineTypeRoutingPolicy> policies;
    private final RoutingPolicyConfig config;
    private final LpFirstThenIcRoutingPolicy defaultPolicy;

    public RoutingPolicyResolver(
        List<MachineTypeRoutingPolicy> policies,
        RoutingPolicyConfig config,
        LpFirstThenIcRoutingPolicy defaultPolicy
    ) {
        this.policies = policies;
        this.config = config;
        this.defaultPolicy = defaultPolicy;
    }

    public MachineTypeRoutingPolicy resolve() {
        String active = config.getActivePolicy();
        for (MachineTypeRoutingPolicy p : policies) {
            if (p.policyId().equals(active)) {
                return p;
            }
        }
        log.warn("Active policy '{}' 미존재 — default {} 사용", active, defaultPolicy.policyId());
        return defaultPolicy;
    }
}
