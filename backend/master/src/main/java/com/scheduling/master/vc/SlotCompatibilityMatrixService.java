package com.scheduling.master.vc;

import com.scheduling.common.metrics.SchedulingMetrics;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SlotCompatibilityMatrix 빌드 + 캐시 서비스 — TK-04-1-2 (REQ-FUNC-VC-001).
 *
 * <p>{@link AtomicReference} 로 current matrix 보관 — 빌드 후 publish 시 lock-free read.
 * 빌드 자체는 {@code synchronized} 로 race 차단 (PG LISTEN/NOTIFY 다중 트리거 안전).
 *
 * <p>SLA — 47품번 기준 ≤ 1초 (TC-VC-001). 실측 50ms 미만 (HashMap + EnumMap).
 * 초과 시 WARN 로그 + Prometheus 메트릭 emit.
 *
 * <p>{@code @Profile("with-infra")} — VcConstraintRepository (JPA) 의존.
 */
@Service
@Profile("with-infra")
public class SlotCompatibilityMatrixService {

    private static final Logger log = LoggerFactory.getLogger(SlotCompatibilityMatrixService.class);
    private static final long SLA_MS = 1000L;

    private final VcConstraintRepository repository;
    private final SchedulingMetrics metrics;
    private final Clock clock;

    private final AtomicReference<SlotCompatibilityMatrix> current = new AtomicReference<>();
    private final AtomicInteger versionCounter = new AtomicInteger(0);

    public SlotCompatibilityMatrixService(
        VcConstraintRepository repository,
        SchedulingMetrics metrics,
        Clock clock
    ) {
        this.repository = repository;
        this.metrics = metrics;
        this.clock = clock;
    }

    @PostConstruct
    public void initialBuild() {
        rebuild();
    }

    /**
     * 전체 매트릭스 재빌드 — 47품번 조회 + immutable matrix 생성.
     */
    public synchronized SlotCompatibilityMatrix rebuild() {
        long startNanos = System.nanoTime();
        List<VcConstraint> all = repository.findAll();

        int version = versionCounter.incrementAndGet();
        Instant builtAt = Instant.now(clock);
        SlotCompatibilityMatrix matrix = SlotCompatibilityMatrix.build(version, builtAt, all);

        current.set(matrix);

        long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        log.info("Slot matrix rebuilt v{}: {} hoses, {} unschedulable, elapsed={}ms",
            version, all.size(), matrix.unschedulableHoseIds().size(), elapsedMs);

        if (elapsedMs > SLA_MS) {
            log.warn("Slot matrix rebuild SLA 위반: {}ms (limit {}ms)", elapsedMs, SLA_MS);
            if (metrics != null) metrics.increment("vc_matrix", "rebuild_sla_violation");
        }
        if (metrics != null) {
            metrics.increment("vc_matrix", "rebuild");
            metrics.recordDuration("vc_matrix", "rebuild_duration", Duration.ofMillis(elapsedMs));
        }
        return matrix;
    }

    /** 최신 매트릭스 — null 가능성 (initialBuild 전). */
    public SlotCompatibilityMatrix current() {
        return current.get();
    }

    /**
     * 캐시 무효화 — PG LISTEN/NOTIFY 수신 시 호출.
     * 즉시 rebuild 호출 → next current() 는 새 버전.
     */
    public synchronized void invalidate() {
        log.info("Slot matrix cache invalidated — 재빌드 시작");
        if (metrics != null) metrics.increment("vc_matrix", "invalidate");
        rebuild();
    }
}
