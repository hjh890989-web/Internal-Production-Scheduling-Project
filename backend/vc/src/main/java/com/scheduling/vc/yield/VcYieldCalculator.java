package com.scheduling.vc.yield;

import com.scheduling.common.metrics.SchedulingMetrics;
import com.scheduling.master.api.VcConstraintLookup;
import com.scheduling.master.api.VcConstraintSummary;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BR-V03 yield/회전 계산 + immutable 매트릭스 캐시 — TK-05-2-1 (REQ-FUNC-VC-006).
 *
 * <p>{@link YieldMatrix} 47품번 × {LP, IC} 사전 계산. AtomicReference 로 lock-free read.
 * {@link #rebuild()} 시 master.api.VcConstraintLookup facade 로 일괄 조회 → 새 매트릭스 publish.
 *
 * <p>ST-04-1·2 의 SlotCompatibilityMatrix LISTEN/NOTIFY 와 별도 — VcYieldCalculator
 * 는 PG NOTIFY 직접 구독 X (yield 는 마스터 변경 빈도 낮음). EP-21 Sprint 2 후속 BR-V14
 * 변경 시 사용자 명시 rebuild API 또는 1h scheduled refresh 추가 검토.
 *
 * <p>{@code @Profile("with-infra")} — VcConstraintLookup (JPA) 의존.
 *
 * <p>Prometheus 메트릭 — vc_yield.rebuild + duration timer.
 */
@Component
@Profile("with-infra")
public class VcYieldCalculator {

    private static final Logger log = LoggerFactory.getLogger(VcYieldCalculator.class);

    private final VcConstraintLookup lookup;
    private final SchedulingMetrics metrics;
    private final Clock clock;

    private final AtomicReference<YieldMatrix> current = new AtomicReference<>();
    private final AtomicInteger versionCounter = new AtomicInteger(0);

    public VcYieldCalculator(VcConstraintLookup lookup, SchedulingMetrics metrics, Clock clock) {
        this.lookup = lookup;
        this.metrics = metrics;
        this.clock = clock;
    }

    @PostConstruct
    public void initialBuild() {
        rebuild();
    }

    public synchronized YieldMatrix rebuild() {
        long startNanos = System.nanoTime();
        List<VcConstraintSummary> all = lookup.findAll();

        Map<String, Integer> lpYields = new HashMap<>();
        Map<String, Integer> icYields = new HashMap<>();
        Set<String> unschedulable = new HashSet<>();

        for (VcConstraintSummary c : all) {
            int lpY = c.lpYieldPerRotation();
            int icY = c.icYieldPerRotation();
            if (lpY > 0) lpYields.put(c.hoseId(), lpY);
            if (icY > 0) icYields.put(c.hoseId(), icY);
            if (lpY == 0 && icY == 0) unschedulable.add(c.hoseId());
        }

        int version = versionCounter.incrementAndGet();
        YieldMatrix matrix = new YieldMatrix(
            version, Instant.now(clock), lpYields, icYields, unschedulable);
        current.set(matrix);

        long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        log.info("YieldMatrix rebuilt v{}: lp={} ic={} unschedulable={} elapsed={}ms",
            version, lpYields.size(), icYields.size(), unschedulable.size(), elapsedMs);
        if (metrics != null) {
            metrics.increment("vc_yield", "rebuild");
            metrics.recordDuration("vc_yield", "rebuild_duration", Duration.ofMillis(elapsedMs));
        }
        return matrix;
    }

    /** 회전당 yield — null 가능 (양쪽 가능 / LP 만 / IC 만). */
    public Optional<Integer> yieldPerRotation(String hoseId, String machineType) {
        YieldMatrix matrix = current.get();
        if (matrix == null) {
            matrix = rebuild();
        }
        return matrix.lookup(hoseId, machineType);
    }

    /** 직접 매트릭스 접근 (TK-05-3 알고리즘 batch 조회용). */
    public YieldMatrix currentMatrix() {
        YieldMatrix matrix = current.get();
        return matrix != null ? matrix : rebuild();
    }
}
