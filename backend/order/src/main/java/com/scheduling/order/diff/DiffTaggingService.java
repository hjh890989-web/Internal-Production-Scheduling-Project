package com.scheduling.order.diff;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.order.domain.OrderDraft;
import com.scheduling.order.events.OrderDiffPersistedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
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
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DiffTaggingService(
        DiffEngineService diffEngine,
        SeverityClassifier classifier,
        DiffPersistenceService persistence,
        OrderChangeRepository repository,
        ApplicationEventPublisher eventPublisher,
        Clock clock
    ) {
        this.diffEngine = diffEngine;
        this.classifier = classifier;
        this.persistence = persistence;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
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
        List<EventDraft> events = new ArrayList<>(persisted.size());
        int criticalCount = 0;
        Instant now = Instant.now(clock);
        for (OrderChangeEntity entity : persisted) {
            RowDiff matched = findRowDiff(diff, entity);
            Severity sev = classifier.classify(matched);
            entity.setSeverity(sev.name());
            updated.add(entity);
            events.add(new EventDraft(entity, matched, sev, now));
            if (sev == Severity.CRITICAL) criticalCount++;
        }
        repository.saveAll(updated);

        // notify 모듈로 이벤트 발행 — Critical/Normal 모두 발행 (Service 가 채널 라우팅).
        // @ApplicationModuleListener (AFTER_COMMIT + @Async) — 알림은 비동기.
        for (EventDraft d : events) {
            eventPublisher.publishEvent(d.toEvent(trackingId));
        }

        log.info("DiffTagging trackingId={} v{}→v{}: persisted={} critical={}",
            trackingId, previousVersion, previousVersion + 1, persisted.size(), criticalCount);
        return diff;
    }

    /** Critical/Normal 양쪽 이벤트 발행을 위한 내부 transfer. */
    private record EventDraft(OrderChangeEntity entity, RowDiff row, Severity severity, Instant at) {
        OrderDiffPersistedEvent toEvent(UUID trackingId) {
            return new OrderDiffPersistedEvent(
                entity.getChangeId(),
                trackingId,
                entity.getHoseId(),
                entity.getDeliveryDate(),
                entity.getDiffType().name(),
                severity == Severity.CRITICAL ? ChangeSeverity.CRITICAL : ChangeSeverity.NORMAL,
                summarize(row),
                at
            );
        }

        private static String summarize(RowDiff row) {
            if (row.fieldDiffs() == null || row.fieldDiffs().isEmpty()) {
                return row.type().name();
            }
            StringBuilder sb = new StringBuilder();
            for (FieldDiff fd : row.fieldDiffs()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(fd.fieldName()).append(": ")
                  .append(fd.before()).append(" → ").append(fd.after());
            }
            return sb.toString();
        }
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
