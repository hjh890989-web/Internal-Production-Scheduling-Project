package com.scheduling.order.domain;

import com.scheduling.common.metrics.SchedulingMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Order DB commit 서비스 — TK-02-1-1.
 *
 * <p>{@link OrderDraft} → {@link Order} entity 변환 후 {@link OrderRepository#save}.
 * UNIQUE 제약 위반 시 {@link DuplicateOrderException} 변환 (Spring DataIntegrityViolationException 캡처).
 *
 * <p>{@code @Profile("with-infra")} — JPA/DataSource 활성 환경에서만 bean 생성.
 * Sprint 1 baseline DEV (no DB) 에서는 unit test 로 검증.
 *
 * <p>BR-X02 audit — Sprint 1+ EP-11 audit 모듈 활성 후 audit row INSERT.
 */
@Service
@Profile("with-infra")
public class OrderCommitService {

    private static final Logger log = LoggerFactory.getLogger(OrderCommitService.class);
    private static final String UNIQUE_CONSTRAINT_NAME = "uq_order_hose_delivery_version";

    private final OrderRepository repository;
    private final SchedulingMetrics metrics;
    private final Clock clock;

    public OrderCommitService(OrderRepository repository, SchedulingMetrics metrics, Clock clock) {
        this.repository = repository;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Order commit(OrderDraft draft, int masterVersion) {
        // commit = 항상 신규 row INSERT 시도 — draft.orderId() 는 in-memory 추적용 placeholder.
        // 동일 entity id 로 save() 가 MERGE 처리되어 UNIQUE 제약을 우회하는 것 차단.
        // 동일 (hose, delivery, version) 재시도는 UNIQUE 제약이 거부 → DuplicateOrderException.
        UUID orderId = UUID.randomUUID();
        Order entity = Order.fromDraft(
            new OrderDraft(orderId, draft.hoseId(), draft.deliveryDate(),
                draft.qty(), draft.orderType(), draft.customer()),
            masterVersion,
            Instant.now(clock)
        );
        try {
            // saveAndFlush — Hibernate 가 INSERT 를 즉시 실행 → UNIQUE 위반을 try/catch 가 포착.
            // save() 는 INSERT 를 transaction commit 까지 지연 → catch block 우회.
            Order saved = repository.saveAndFlush(entity);
            if (metrics != null) metrics.increment("order_commit", "success");
            return saved;
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                log.warn("Duplicate commit blocked by DB: hose={} delivery={} version={}",
                    draft.hoseId(), draft.deliveryDate(), masterVersion);
                // K-O03 — TK-02-1-2 DB UNIQUE 위반 카운터 (Grafana / Alertmanager 핫스팟)
                if (metrics != null) metrics.increment("order_commit", "unique_violation");
                throw new DuplicateOrderException(
                    draft.hoseId(), draft.deliveryDate(), masterVersion, e);
            }
            if (metrics != null) metrics.increment("order_commit", "error");
            throw e;
        }
    }

    private boolean isUniqueViolation(DataIntegrityViolationException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains(UNIQUE_CONSTRAINT_NAME)) {
            return true;
        }
        // PostgreSQL SQLState 23505 = unique_violation
        Throwable root = rootCause(e);
        return root != null && root.getMessage() != null
            && root.getMessage().contains(UNIQUE_CONSTRAINT_NAME);
    }

    private Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
}
