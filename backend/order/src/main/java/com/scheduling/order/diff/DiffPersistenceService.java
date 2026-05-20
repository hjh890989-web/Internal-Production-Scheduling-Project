package com.scheduling.order.diff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DiffResult → app.order_change 영속 — TK-03-1-3.
 *
 * <p>한 import 추적 ID 당 다수 row INSERT (RowDiff 1건 ↔ OrderChangeEntity 1건).
 * field_diffs 는 Jackson JSON 직렬화 → JSONB column.
 */
@Service
@Profile("with-infra")
public class DiffPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(DiffPersistenceService.class);

    private final OrderChangeRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DiffPersistenceService(
        OrderChangeRepository repository,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public List<OrderChangeEntity> persist(DiffResult result) {
        Instant at = Instant.now(clock);
        List<OrderChangeEntity> entities = new ArrayList<>(result.rows().size());

        for (RowDiff row : result.rows()) {
            if (row.type() == DiffType.UNCHANGED) {
                // UNCHANGED 는 audit 정합용 (DiffResult 보존) — DB persist 는 skip (저장 비용 ↓)
                continue;
            }
            String fieldDiffsJson = serialize(row.fieldDiffs());
            UUID newOrderId = row.newRow() != null ? row.newRow().orderId() : null;
            UUID oldOrderId = row.oldRow() != null ? row.oldRow().getOrderId() : null;

            OrderChangeEntity entity = new OrderChangeEntity(
                UUID.randomUUID(),
                result.trackingId(),
                row.type(),
                row.key().hoseId(),
                row.key().deliveryDate(),
                newOrderId,
                oldOrderId,
                fieldDiffsJson,
                result.previousVersion(),
                result.newVersion(),
                null,            // severity — ST-03-2 에서 채움
                at
            );
            entities.add(entity);
        }

        List<OrderChangeEntity> saved = repository.saveAll(entities);
        log.info("Persisted {} order_change rows for trackingId={}", saved.size(), result.trackingId());
        return saved;
    }

    private String serialize(List<FieldDiff> fieldDiffs) {
        if (fieldDiffs.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(fieldDiffs);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("FieldDiff 직렬화 실패", e);
        }
    }
}
