package com.scheduling.vc.capacity_overflow;

import com.scheduling.master.api.ProductPriorityLookup;
import com.scheduling.master.api.ProductPrioritySummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * BR-V12 capa 초과 큐 분리 — Sprint 7 (REQ-FUNC-VC-022, deferred 활성).
 *
 * <p>{@code Σ Q_required > daily_capa} 시 hose 별 요청량을 PRODUCT_PRIORITY rank ASC
 * 정렬 → (a) capa 내 자동 채택분 + (b) 추가 요청 큐 분리.
 *
 * <p>활성 조건 — 수주통합 (Sprint 1 EP-01) 안정화 후 + DI-07 PRODUCT_PRIORITY 마스터 입력.
 */
@Service
@Profile("with-infra")
public class CapacityOverflowQueueService {

    private static final Logger log = LoggerFactory.getLogger(CapacityOverflowQueueService.class);

    private final ProductPriorityLookup priorityLookup;
    private final Clock clock;

    public CapacityOverflowQueueService(ProductPriorityLookup priorityLookup, Clock clock) {
        this.priorityLookup = priorityLookup;
        this.clock = clock;
    }

    public record SplitResult(
        Map<String, Integer> accepted,        // hose_id → 채택 qty
        Map<String, Integer> requestQueue,    // hose_id → 추가 요청 qty (Planner 승인 대기)
        int totalAccepted,
        int totalQueued
    ) {}

    /**
     * @param required   hose_id → 요구 qty (전체 일별)
     * @param dailyCapa  daily capacity (BR-V05 — LP 72 + IC 18 = 90 기본)
     * @return SplitResult — accepted (capa 내) + requestQueue (Planner 승인 대기)
     */
    public SplitResult split(Map<String, Integer> required, int dailyCapa) {
        if (required == null || required.isEmpty()) {
            return new SplitResult(Map.of(), Map.of(), 0, 0);
        }

        // PRODUCT_PRIORITY 조회 — 본 일자 유효
        LocalDate today = LocalDate.now(clock);
        List<ProductPrioritySummary> priorities = priorityLookup.findEffectiveAt(today);
        Map<String, Short> rankByHose = priorities.stream()
            .collect(java.util.stream.Collectors.toMap(
                ProductPrioritySummary::hoseId,
                ProductPrioritySummary::priorityRank,
                (a, b) -> a));

        // priority rank ASC (1=최우선) → 정렬, 미등록 hose 는 99 fallback (후순위)
        List<Map.Entry<String, Integer>> sorted = required.entrySet().stream()
            .sorted(Comparator.comparingInt(e ->
                rankByHose.getOrDefault(e.getKey(), (short) 99)))
            .toList();

        Map<String, Integer> accepted = new java.util.LinkedHashMap<>();
        Map<String, Integer> queue = new java.util.LinkedHashMap<>();
        int remainingCapa = dailyCapa;

        for (Map.Entry<String, Integer> entry : sorted) {
            int qty = entry.getValue();
            if (remainingCapa >= qty) {
                accepted.put(entry.getKey(), qty);
                remainingCapa -= qty;
            } else if (remainingCapa > 0) {
                accepted.put(entry.getKey(), remainingCapa);
                queue.put(entry.getKey(), qty - remainingCapa);
                remainingCapa = 0;
            } else {
                queue.put(entry.getKey(), qty);
            }
        }

        int totalAccepted = accepted.values().stream().mapToInt(Integer::intValue).sum();
        int totalQueued = queue.values().stream().mapToInt(Integer::intValue).sum();
        log.info("BR-V12 capa split — accepted={} queued={} (capa={}, requestedTotal={})",
            totalAccepted, totalQueued, dailyCapa, totalAccepted + totalQueued);
        return new SplitResult(accepted, queue, totalAccepted, totalQueued);
    }
}
