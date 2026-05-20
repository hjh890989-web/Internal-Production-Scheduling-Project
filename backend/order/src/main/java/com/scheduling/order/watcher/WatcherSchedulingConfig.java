package com.scheduling.order.watcher;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@link ScheduledFolderPoller#poll} {@code @Scheduled} 활성용 — TK-01-3-1.
 *
 * <p>order 모듈의 첫 @Scheduled 도입. Spring 의 TaskScheduler 는 application context
 * 단위로 singleton — 다른 모듈의 @EnableScheduling 와 idempotent.
 */
@Configuration
@EnableScheduling
class WatcherSchedulingConfig {
}
