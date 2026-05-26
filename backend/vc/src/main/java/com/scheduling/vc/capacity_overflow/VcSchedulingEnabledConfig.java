package com.scheduling.vc.capacity_overflow;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@link CapacityOverflowMetrics} {@code @Scheduled refresh()} 활성용 — Sprint 8 EP-V13-Grafana.
 *
 * <p>{@code @Profile("with-infra")} — DEV 컨텍스트 미활성화 (Repository 부재 회피).
 * notify/order 모듈의 SchedulingEnabledConfig 동일 패턴.
 */
@Configuration
@Profile("with-infra")
@EnableScheduling
class VcSchedulingEnabledConfig {
}
