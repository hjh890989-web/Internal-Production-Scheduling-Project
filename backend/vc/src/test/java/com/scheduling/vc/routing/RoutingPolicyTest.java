package com.scheduling.vc.routing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 라우팅 정책 회귀 — TK-05-4-1 (BR-V08).
 */
class RoutingPolicyTest {

    private static final RoutingContext CTX = RoutingContext.initial(LocalDate.of(2026, 2, 16));

    @Test
    @DisplayName("LpFirstThenIcRoutingPolicy — [LP, IC] 우선순위 (BR-V08)")
    void lp_first_policy() {
        LpFirstThenIcRoutingPolicy policy = new LpFirstThenIcRoutingPolicy();
        assertThat(policy.prioritize("A", CTX)).containsExactly(MachineType.LP, MachineType.IC);
        assertThat(policy.policyId()).isEqualTo("LP_FIRST");
    }

    @Test
    @DisplayName("IcFirstRoutingPolicy — [IC, LP] 우선순위")
    void ic_first_policy() {
        IcFirstRoutingPolicy policy = new IcFirstRoutingPolicy();
        assertThat(policy.prioritize("A", CTX)).containsExactly(MachineType.IC, MachineType.LP);
        assertThat(policy.policyId()).isEqualTo("IC_FIRST");
    }

    @Test
    @DisplayName("RoutingPolicyResolver — active-policy 매칭 (LP_FIRST default)")
    void resolver_picks_active_policy() {
        LpFirstThenIcRoutingPolicy lp = new LpFirstThenIcRoutingPolicy();
        IcFirstRoutingPolicy ic = new IcFirstRoutingPolicy();
        RoutingPolicyConfig config = new RoutingPolicyConfig();
        config.setActivePolicy("IC_FIRST");

        RoutingPolicyResolver resolver = new RoutingPolicyResolver(List.of(lp, ic), config, lp);
        assertThat(resolver.resolve().policyId()).isEqualTo("IC_FIRST");
    }

    @Test
    @DisplayName("RoutingPolicyResolver — 미존재 정책 → default LpFirst fallback")
    void resolver_unknown_policy_falls_back() {
        LpFirstThenIcRoutingPolicy lp = new LpFirstThenIcRoutingPolicy();
        RoutingPolicyConfig config = new RoutingPolicyConfig();
        config.setActivePolicy("UNKNOWN_X");

        RoutingPolicyResolver resolver = new RoutingPolicyResolver(List.of(lp), config, lp);
        assertThat(resolver.resolve().policyId()).isEqualTo("LP_FIRST");
    }

    @Test
    @DisplayName("RoutingPolicyConfig — 기본값 LP_FIRST (BR-V08)")
    void config_default_is_lp_first() {
        assertThat(new RoutingPolicyConfig().getActivePolicy()).isEqualTo("LP_FIRST");
    }

    @Test
    @DisplayName("RoutingContext.initial — 가동률 0 default")
    void context_initial_zero_utilization() {
        RoutingContext ctx = RoutingContext.initial(LocalDate.of(2026, 2, 16));
        assertThat(ctx.currentLpUtilizationPct()).isZero();
        assertThat(ctx.currentIcUtilizationPct()).isZero();
    }
}
