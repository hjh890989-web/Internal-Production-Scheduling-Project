package com.scheduling.notify;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@link DeliveryEscalator} {@code @Scheduled} 활성용 — TK-03-3-2.
 *
 * <p>{@code @Profile("with-infra")} — DEV 컨텍스트 미활성화 (Repository 부재로 인한 schedule fail 방지).
 */
@Configuration
@Profile("with-infra")
@EnableScheduling
class SchedulingEnabledConfig {
}
