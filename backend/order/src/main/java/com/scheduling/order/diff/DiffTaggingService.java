package com.scheduling.order.diff;

import com.scheduling.order.domain.OrderDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Diff 계산 + Severity 태깅 통합 — TK-03-2-1.
 *
 * <p>흐름:
 * <ol>
 *   <li>{@link DiffEngineService#compute} → DiffResult</li>
 *   <li>{@link SeverityClassifier#classify} 각 RowDiff 별 severity</li>
 *   <li>{@link DiffPersistenceService#persist} 후 severity UPDATE</li>
 *   <li>ST-03-3 알림이 severity=CRITICAL 만 우선 발송</li>
 * </ol>
 *
 * <p>본 서비스는 @Profile("with-infra") — DB 의존.
 */
@Service
@Profile("with-infra")
public class DiffTaggingService {

    private static final Logger log = LoggerFactory.getLogger(DiffTaggingService.class);

    private final DiffEngineService diffEngine;
    private final SeverityClassifier classifier;
    private final DiffPersistenceService persistence;
    private final OrderChangeRepository repository;

    public DiffTaggingService(
        DiffEngineService diffEngine,
        SeverityClassifier classifier,
        DiffPersistenceService persistence,
        OrderChangeRepository repository
    ) {
        this.diffEngine = diffEngine;
        this.classifier = classifier;
        this.persistence = persistence;
        this.repository = repository;
    }

    /**
     * Diff 계산 + 영속 + Severity 태깅.
     *
     * @return DiffResult (severity 정보는 OrderChangeEntity 에 부착)
     */
    @Transactional
    public DiffResult computeAndTag(UUID trackingId, List<OrderDraft> newRows, int previousVersion) {
        DiffResult diff = diffEngine.compute(trackingId, newRows, previousVersion);

        // 영속 (UNCHANGED 제외)
        List<OrderChangeEntity> persisted = persistence.persist(diff);

        // RowDiff 와 OrderChangeEntity 매칭 — (hose_id, delivery_date) 키
        // diff.rows() 순서와 persisted 순서가 다를 수 있어 매핑 필요
        List<OrderChangeEntity> updated = new ArrayList<>(persisted.size());
        int criticalCount = 0;
        for (OrderChangeEntity entity : persisted) {
            RowDiff matched = findRowDiff(diff, entity);
            Severity sev = classifier.classify(matched);
            entity.setSeverity(sev.name());
            updated.add(entity);
            if (sev == Severity.CRITICAL) criticalCount++;
        }
        repository.saveAll(updated);

        log.info("DiffTagging trackingId={} v{}→v{}: persisted={} critical={}",
            trackingId, previousVersion, previousVersion + 1, persisted.size(), criticalCount);
        return diff;
    }

    private RowDiff findRowDiff(DiffResult diff, OrderChangeEntity entity) {
        for (RowDiff row : diff.rows()) {
            if (row.key().hoseId().equals(entity.getHoseId())
                && row.key().deliveryDate().equals(entity.getDeliveryDate())) {
                return row;
            }
        }
        throw new IllegalStateException(
            "RowDiff 매칭 실패 — hose=" + entity.getHoseId() + " date=" + entity.getDeliveryDate());
    }
}
