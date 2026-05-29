package com.scheduling.audit.maintenance;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@link PartitionMaintenanceScheduler} {@code @Scheduled} 활성용 — Sprint 22 ST-SEC-3.
 *
 * <p>{@code @Profile("with-infra")} — DEV 컨텍스트 미활성화 (DataSource 부재 시 schedule fail 방지).
 * 다른 모듈의 SchedulingEnabledConfig 와 동일 패턴 (notify/order/vc).
 */
@Configuration
@Profile("with-infra")
@EnableScheduling
class AuditSchedulingEnabledConfig {
}
