package com.scheduling.vc.capacity_overflow;

import com.scheduling.master.api.KdOrderLookup;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.MultiGauge.Row;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Sprint 8 EP-V13-Grafana — IT_OPS BR-V12·V13 Prometheus metric 노출.
 *
 * <p>30초마다 refresh — Prometheus scrape interval (15s) 의 2배. Grafana 대시 갱신 적정.
 *
 * <p>노출 metric:
 * <ul>
 *   <li>{@code scheduling_v13_kd_remaining_qty{hose=...}} — hose 별 KD 잔량 (OPEN+PARTIAL 합계)</li>
 *   <li>{@code scheduling_v12_pending_request_count{status="PENDING"}} — Sprint 8 PENDING 요청 누적</li>
 * </ul>
 *
 * <p>활성 조건 — DI-07/08 마스터 입력 후 비-zero. 미입력 시 빈 metric (그래프 0).
 */
@Component
@Profile("with-infra")
public class CapacityOverflowMetrics {

    private static final Logger log = LoggerFactory.getLogger(CapacityOverflowMetrics.class);

    private final KdOrderLookup kdLookup;
    private final CapacityOverflowRequestRepository requestRepo;
    private final MultiGauge kdRemainingGauge;
    private final MultiGauge pendingQueueGauge;

    public CapacityOverflowMetrics(KdOrderLookup kdLookup,
                                    CapacityOverflowRequestRepository requestRepo,
                                    MeterRegistry registry) {
        this.kdLookup = kdLookup;
        this.requestRepo = requestRepo;
        this.kdRemainingGauge = MultiGauge.builder("scheduling.v13.kd.remaining.qty")
            .description("BR-V13 hose 별 KD remaining_qty 합계 (OPEN+PARTIAL)")
            .baseUnit("units")
            .register(registry);
        this.pendingQueueGauge = MultiGauge.builder("scheduling.v12.pending.request.count")
            .description("BR-V12 추가 요청 큐 status 별 count (Sprint 8)")
            .baseUnit("requests")
            .register(registry);
    }

    /** 30초 refresh — 부팅 5초 후 첫 측정 (master 시드 import 여유). */
    @Scheduled(initialDelay = 5_000, fixedRate = 30_000)
    public void refresh() {
        try {
            refreshKdRemaining();
            refreshPendingQueue();
        } catch (Exception e) {
            log.warn("CapacityOverflowMetrics refresh 실패 — 다음 cycle 재시도: {}", e.getMessage());
        }
    }

    private void refreshKdRemaining() {
        Map<String, Long> remainingByHose = kdLookup.remainingByHose();
        List<Row<?>> rows = remainingByHose.entrySet().stream()
            .<Row<?>>map(e -> Row.of(Tags.of("hose", e.getKey()), e.getValue().doubleValue()))
            .toList();
        kdRemainingGauge.register(rows, true);   // overwrite — 사라진 hose 제거
    }

    private void refreshPendingQueue() {
        long pending = requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.PENDING).size();
        long accepted = requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.ACCEPTED).size();
        long rejected = requestRepo.findByStatusOrderByPriorityRankAscRequestedAtAsc(
            CapacityOverflowRequest.Status.REJECTED).size();
        pendingQueueGauge.register(List.<Row<?>>of(
            Row.of(Tags.of("status", "PENDING"), pending),
            Row.of(Tags.of("status", "ACCEPTED"), accepted),
            Row.of(Tags.of("status", "REJECTED"), rejected)
        ), true);
    }
}
