package com.scheduling.order.diff;

import com.scheduling.order.domain.Order;
import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.domain.OrderKey;
import com.scheduling.order.domain.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 마스터 버전 간 row-level diff 엔진 — TK-03-1-1 (REQ-FUNC-OC-007).
 *
 * <p>알고리즘 (outer join semantic):
 * <ol>
 *   <li>이전 마스터 버전의 ACTIVE row 조회 → {@code Map<OrderKey, Order>}</li>
 *   <li>신규 winner 셋 → {@code Map<OrderKey, OrderDraft>}</li>
 *   <li>합집합 키 순회 → {@link #classify} 로 RowDiff 생성:
 *     <ul>
 *       <li>이전만 → DELETED</li>
 *       <li>신규만 → NEW</li>
 *       <li>양쪽 + 필드 차이 → MODIFIED (필드별 before/after)</li>
 *       <li>양쪽 + 동일 → UNCHANGED</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p>입력 보장: 신규 셋은 EP-02 ST-02-2 {@link com.scheduling.order.domain.ResolutionService}
 * 결과 winner 목록 (중복 없음). 이전 마스터는 status='ACTIVE' 만.
 */
@Service
@Profile("with-infra")
public class DiffEngineService {

    private static final Logger log = LoggerFactory.getLogger(DiffEngineService.class);

    private final OrderRepository repository;
    private final Clock clock;

    public DiffEngineService(OrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * 신규 winner 셋 vs 이전 master_version outer join.
     *
     * @param trackingId       import 추적 ID (TK-01-1-3)
     * @param newRows          신규 winner OrderDraft 셋 (Resolution.winner 모음)
     * @param previousVersion  이전 마스터 버전. 0 이면 모든 row 가 NEW
     */
    public DiffResult compute(UUID trackingId, List<OrderDraft> newRows, int previousVersion) {
        List<Order> oldRows = previousVersion <= 0
            ? List.of()
            : repository.findByMasterVersion(previousVersion);

        Map<OrderKey, Order> oldByKey = new HashMap<>(oldRows.size());
        for (Order o : oldRows) oldByKey.put(OrderKey.of(o), o);

        Map<OrderKey, OrderDraft> newByKey = new LinkedHashMap<>(newRows.size());
        for (OrderDraft d : newRows) newByKey.put(OrderKey.of(d), d);

        Set<OrderKey> allKeys = new HashSet<>(oldByKey.size() + newByKey.size());
        allKeys.addAll(oldByKey.keySet());
        allKeys.addAll(newByKey.keySet());

        List<RowDiff> diffs = new ArrayList<>(allKeys.size());
        for (OrderKey key : allKeys) {
            diffs.add(classify(key, oldByKey.get(key), newByKey.get(key)));
        }

        DiffResult result = new DiffResult(
            trackingId,
            previousVersion,
            previousVersion + 1,
            Instant.now(clock),
            diffs
        );
        log.info("Diff computed for tracking={} v{}→v{}: NEW={} MOD={} DEL={} UNC={}",
            trackingId, previousVersion, previousVersion + 1,
            result.countByType(DiffType.NEW),
            result.countByType(DiffType.MODIFIED),
            result.countByType(DiffType.DELETED),
            result.countByType(DiffType.UNCHANGED));
        return result;
    }

    static RowDiff classify(OrderKey key, Order old, OrderDraft fresh) {
        if (old == null && fresh != null) {
            return new RowDiff(key, DiffType.NEW, fresh, null, List.of());
        }
        if (old != null && fresh == null) {
            return new RowDiff(key, DiffType.DELETED, null, old, List.of());
        }
        // 양쪽 존재 — 필드 비교
        List<FieldDiff> fieldDiffs = compareFields(old, fresh);
        DiffType type = fieldDiffs.isEmpty() ? DiffType.UNCHANGED : DiffType.MODIFIED;
        return new RowDiff(key, type, fresh, old, fieldDiffs);
    }

    static List<FieldDiff> compareFields(Order old, OrderDraft fresh) {
        List<FieldDiff> diffs = new ArrayList<>(5);
        addIfDiff("hose_id",       old.getHoseId(),       fresh.hoseId(),       diffs);
        addIfDiff("delivery_date", old.getDeliveryDate(), fresh.deliveryDate(), diffs);
        addIfDiff("qty",           old.getQty(),          fresh.qty(),          diffs);
        addIfDiff("customer",      normalize(old.getCustomer()), normalize(fresh.customer()), diffs);
        addIfDiff("order_type",    old.getOrderType(),    fresh.orderType(),    diffs);
        return diffs;
    }

    private static void addIfDiff(String field, Object before, Object after, List<FieldDiff> diffs) {
        if (!Objects.equals(before, after)) {
            diffs.add(new FieldDiff(field, before, after));
        }
    }

    /** customer null/blank → "내수" 기본값 (OrderDraft canonical constructor 와 정합). */
    private static String normalize(String value) {
        return value == null || value.isBlank() ? "내수" : value;
    }
}
