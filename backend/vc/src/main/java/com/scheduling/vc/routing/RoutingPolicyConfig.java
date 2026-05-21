package com.scheduling.vc.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 라우팅 정책 설정 — TK-05-4-1.
 *
 * <p>application.yaml:
 * <pre>
 * scheduling:
 *   routing:
 *     active-policy: LP_FIRST     # BR-V08 (default), or IC_FIRST
 * </pre>
 */
@ConfigurationProperties(prefix = "scheduling.routing")
public class RoutingPolicyConfig {

    private String activePolicy = LpFirstThenIcRoutingPolicy.POLICY_ID;

    public String getActivePolicy() { return activePolicy; }
    public void setActivePolicy(String activePolicy) { this.activePolicy = activePolicy; }
}
