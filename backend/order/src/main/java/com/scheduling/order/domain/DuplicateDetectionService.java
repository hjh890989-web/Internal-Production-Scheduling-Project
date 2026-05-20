package com.scheduling.order.domain;

import com.scheduling.common.metrics.SchedulingMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ORM 레벨 사전 중복 감지 — TK-02-1-3 (REQ-FUNC-OC-005).
 *
 * <p>책임:
 * <ul>
 *   <li>batch 내 동일 {@link OrderKey} 중복 사전 검출 — DB hit 없이 차단</li>
 *   <li>기존 마스터 (ACTIVE) 와 충돌 검출 — batch query 1회 ({@link OrderRepository#findActiveByHoseDeliveryPairs})</li>
 *   <li>{@link DuplicateGroup} 산출 — ST-02-2 우선순위 해소 (TK-02-2-1) 가 소비</li>
 *   <li>K-O03 부속 KPI — {@code order_duplicate.detected} 카운터 emit</li>
 * </ul>
 *
 * <p>성능: N+1 회피를 위해 hose_ids·delivery_dates 다중 IN 단일 query.
 *
 * <p>{@code @Profile("with-infra")} — JPA Repository 의존, DB 활성 환경에서만 bean 생성.
 * Sprint 1 baseline DEV (no DB) 에서는 unit test (mock repository) 로 검증.
 */
@Service
@Profile("with-infra")
public class DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionService.class);

    private final OrderRepository repository;
    private final SchedulingMetrics metrics;

    public DuplicateDetectionService(OrderRepository repository, SchedulingMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    /**
     * 입력 batch 의 중복 그룹 산출.
     *
     * <p>알고리즘:
     * <ol>
     *   <li>batch 를 {@link OrderKey} 로 grouping</li>
     *   <li>모든 유니크 키의 ACTIVE 마스터 한 번에 조회 (batch query)</li>
     *   <li>각 그룹에 대해 candidates·existing 매핑 → {@link DuplicateGroup}</li>
     *   <li>{@link DuplicateGroup#isDuplicate()} = true 만 필터링 + 메트릭 emit</li>
     * </ol>
     *
     * @param batch 신규 OrderDraft 입력 (빈 리스트 허용)
     * @return 중복 그룹 — 비어 있으면 모두 unique
     */
    public List<DuplicateGroup> detect(List<OrderDraft> batch) {
        if (batch == null || batch.isEmpty()) {
            return List.of();
        }

        Map<OrderKey, List<OrderDraft>> grouped = batch.stream()
            .collect(Collectors.groupingBy(OrderKey::of, LinkedHashMap::new, Collectors.toList()));

        Set<OrderKey> uniqueKeys = grouped.keySet();
        Map<OrderKey, Order> existingMap = loadExisting(uniqueKeys);

        List<DuplicateGroup> duplicates = grouped.entrySet().stream()
            .map(entry -> new DuplicateGroup(entry.getKey(), entry.getValue(), existingMap.get(entry.getKey())))
            .filter(DuplicateGroup::isDuplicate)
            .toList();

        if (!duplicates.isEmpty() && metrics != null) {
            metrics.increment("order_duplicate", "detected_batch");
            for (DuplicateGroup g : duplicates) {
                if (g.hasExisting()) {
                    metrics.increment("order_duplicate", "vs_master");
                }
                if (g.candidateCount() > 1) {
                    metrics.increment("order_duplicate", "within_batch");
                }
            }
        }

        log.info("Duplicate detection: batch={} unique={} dup_groups={}",
            batch.size(), uniqueKeys.size(), duplicates.size());

        return duplicates;
    }

    /** 중복 키만 추출 — 단순 사용 (DuplicateGroup 불필요 시). */
    public Set<OrderKey> findDuplicateKeys(List<OrderDraft> batch) {
        return detect(batch).stream()
            .map(DuplicateGroup::key)
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Map<OrderKey, Order> loadExisting(Set<OrderKey> uniqueKeys) {
        if (uniqueKeys.isEmpty() || repository == null) {
            return Map.of();
        }
        List<String> hoseIds = uniqueKeys.stream().map(OrderKey::hoseId).distinct().toList();
        List<java.time.LocalDate> dates = uniqueKeys.stream().map(OrderKey::deliveryDate).distinct().toList();

        List<Order> candidates = repository.findActiveByHoseDeliveryPairs(hoseIds, dates);
        // IN 조합 (hoseIds × dates) 결과는 cross join 포함 — uniqueKeys 와 매칭만 추림.
        Map<OrderKey, Order> result = new HashMap<>();
        for (Order o : candidates) {
            OrderKey key = OrderKey.of(o);
            if (uniqueKeys.contains(key)) {
                result.put(key, o);
            }
        }
        return result;
    }
}
