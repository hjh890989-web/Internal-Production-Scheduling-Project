package com.scheduling.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 도메인 모듈 공통 metric facade — Sprint 1+ VC·EX·Order·Audit 가 재사용.
 *
 * <p>Micrometer Timer/Counter 등록을 표준화 — 모든 metric 은 {@code scheduling.*}
 * prefix + (module, operation) tag. Grafana 17 KPI 대시보드의 root metric.
 *
 * <p>예시:
 * <pre>{@code
 *   metrics.recordDuration("vc", "allocate", duration);   // → scheduling.duration{module=vc,operation=allocate}
 *   metrics.increment("audit", "br_violation");           // → scheduling.events{module=audit,operation=br_violation}
 * }</pre>
 */
@Component
public class SchedulingMetrics {

    private static final String DURATION_METRIC = "scheduling.duration";
    private static final String EVENT_METRIC = "scheduling.events";

    private final MeterRegistry registry;
    private final ConcurrentMap<String, Timer> timerCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> counterCache = new ConcurrentHashMap<>();

    public SchedulingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordDuration(String module, String operation, Duration duration) {
        timerCache.computeIfAbsent(module + "." + operation, k ->
            Timer.builder(DURATION_METRIC)
                .tag("module", module)
                .tag("operation", operation)
                .publishPercentileHistogram(true)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
        ).record(duration);
    }

    public void increment(String module, String event) {
        counterCache.computeIfAbsent(module + "." + event, k ->
            Counter.builder(EVENT_METRIC)
                .tag("module", module)
                .tag("operation", event)
                .register(registry)
        ).increment();
    }
}
