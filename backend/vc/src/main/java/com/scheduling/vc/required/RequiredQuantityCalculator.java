package com.scheduling.vc.required;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REQ-FUNC-VC-009 Q_required 계산 — TK-05-3-1.
 *
 * <p>공식: <b>Q_required = max(0, Q_net + target_stock − current_stock)</b>.
 *
 * <p>{@link OrderInput} 리스트 + 호라이즌 + hose_id → {@link StockInfo} 매핑 입력.
 * 결과 {@link QRequiredResult} 는 {@code needsProduction() = qRequired > 0} 만 출력.
 *
 * <p>본 calculator 는 순수 함수형 — DB 의존 X. caller 가 stockInfo 를 외부 ERP / Phase 2+
 * master.product 에서 조회하여 주입. 단위 테스트 100% 결정성.
 */
@Component
public class RequiredQuantityCalculator {

    private static final Logger log = LoggerFactory.getLogger(RequiredQuantityCalculator.class);

    /**
     * 호라이즌 내 수주 → Q_required 결과.
     *
     * @param orders        호라이즌 후보 수주 (EP-02 중복 해소 + EP-03 Diff 거친 정본)
     * @param stockInfo     hose_id → 재고 정보 (caller 가 ERP 또는 마스터 조회 후 주입)
     * @param horizonFrom   호라이즌 시작일 (inclusive)
     * @param horizonTo     호라이즌 종료일 (inclusive)
     * @return hose_id → QRequiredResult (qRequired = 0 제외)
     * @throws NoSuchElementException 입력 수주의 hose_id 가 stockInfo 에 없음
     */
    public Map<String, QRequiredResult> calculate(
        Collection<OrderInput> orders,
        Map<String, StockInfo> stockInfo,
        LocalDate horizonFrom,
        LocalDate horizonTo
    ) {
        if (orders == null || orders.isEmpty()) {
            return Map.of();
        }
        if (horizonTo.isBefore(horizonFrom)) {
            throw new IllegalArgumentException("horizonTo >= horizonFrom 필수");
        }
        Map<String, StockInfo> stockMap = stockInfo == null ? Map.of() : stockInfo;

        // 1. 호라이즌 내 수주만 — qty 음수 검증 + hose_id 그룹핑 → Q_net
        Map<String, Integer> qNetByHose = new HashMap<>();
        for (OrderInput o : orders) {
            if (o.deliveryDate().isBefore(horizonFrom) || o.deliveryDate().isAfter(horizonTo)) {
                continue;
            }
            if (o.qty() < 0) {
                throw new IllegalArgumentException(
                    "OrderInput qty 음수 불가: hose=%s qty=%d".formatted(o.hoseId(), o.qty()));
            }
            qNetByHose.merge(o.hoseId(), o.qty(), Integer::sum);
        }

        // 2. Q_required = max(0, Q_net + target − current)
        Map<String, QRequiredResult> result = new HashMap<>();
        for (Map.Entry<String, Integer> e : qNetByHose.entrySet()) {
            String hose = e.getKey();
            int qNet = e.getValue();
            StockInfo si = stockMap.get(hose);
            if (si == null) {
                throw new NoSuchElementException(
                    "StockInfo 누락 — hose_id=%s. caller 가 ERP 조회 결과 주입 필요.".formatted(hose));
            }
            int qRequired = Math.max(0, qNet + si.targetStock() - si.currentStock());
            QRequiredResult row = new QRequiredResult(
                hose, qNet, si.targetStock(), si.currentStock(), qRequired);
            if (row.needsProduction()) {
                result.put(hose, row);
            }
        }
        log.debug("Q_required 계산 — input orders={} horizon={}~{} result={}",
            orders.size(), horizonFrom, horizonTo, result.size());
        return Map.copyOf(result);
    }

    /** 단순 형식 — caller 가 qRequired 값만 필요할 때. */
    public Map<String, Integer> calculateQuantities(
        Collection<OrderInput> orders,
        Map<String, StockInfo> stockInfo,
        LocalDate horizonFrom,
        LocalDate horizonTo
    ) {
        Map<String, Integer> simple = new HashMap<>();
        calculate(orders, stockInfo, horizonFrom, horizonTo)
            .forEach((hose, r) -> simple.put(hose, r.qRequired()));
        return Map.copyOf(simple);
    }

    /** caller 보조 — orders → hose_id 별 OrderInput 그룹 (greedy allocator linkedOrderIds 추적). */
    public Map<String, List<OrderInput>> groupByHose(Collection<OrderInput> orders) {
        if (orders == null) return Map.of();
        Map<String, List<OrderInput>> grouped = new HashMap<>();
        for (OrderInput o : orders) {
            grouped.computeIfAbsent(o.hoseId(), k -> new java.util.ArrayList<>()).add(o);
        }
        return Map.copyOf(grouped);
    }
}
