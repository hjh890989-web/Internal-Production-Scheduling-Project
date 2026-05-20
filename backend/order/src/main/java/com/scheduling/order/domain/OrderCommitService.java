package com.scheduling.order.domain;

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
    private final Clock clock;

    public OrderCommitService(OrderRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Order commit(OrderDraft draft, int masterVersion) {
        UUID orderId = draft.orderId() != null ? draft.orderId() : UUID.randomUUID();
        Order entity = Order.fromDraft(
            new OrderDraft(orderId, draft.hoseId(), draft.deliveryDate(),
                draft.qty(), draft.orderType(), draft.customer()),
            masterVersion,
            Instant.now(clock)
        );
        try {
            return repository.save(entity);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                log.warn("Duplicate commit blocked by DB: hose={} delivery={} version={}",
                    draft.hoseId(), draft.deliveryDate(), masterVersion);
                throw new DuplicateOrderException(
                    draft.hoseId(), draft.deliveryDate(), masterVersion, e);
            }
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
