package com.scheduling.order.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OrderCommitService 회귀 — TK-02-1-1.
 *
 * <p>5 케이스:
 *   1. 정상 commit → Order entity
 *   2. UNIQUE 제약 (constraint name 매칭) → DuplicateOrderException
 *   3. UNIQUE 제약 (root cause 메시지) → DuplicateOrderException
 *   4. 기타 DataIntegrityViolation (constraint name 매칭 X) → 원본 예외 전파
 *   5. orderId null 입력 → UUID 자동 발급
 */
class OrderCommitServiceTest {

    private OrderRepository repository;
    private com.scheduling.common.metrics.SchedulingMetrics metrics;
    private OrderCommitService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        metrics = mock(com.scheduling.common.metrics.SchedulingMetrics.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-20T05:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new OrderCommitService(repository, metrics, clock);
    }

    private OrderDraft draft() {
        return new OrderDraft(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "29673-2F900",
            LocalDate.of(2026, 2, 15),
            100,
            OrderType.FORECAST,
            "내수"
        );
    }

    @Test
    @DisplayName("정상 commit — repository.save 호출 + Order 반환")
    void normal_commit() {
        when(repository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.commit(draft(), 1);

        assertThat(result.getHoseId()).isEqualTo("29673-2F900");
        assertThat(result.getMasterVersion()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2026-05-20T05:00:00Z"));
    }

    @Test
    @DisplayName("UNIQUE 제약 위반 (constraint name 매칭) → DuplicateOrderException")
    void unique_violation_constraint_name() {
        when(repository.saveAndFlush(any(Order.class))).thenThrow(
            new DataIntegrityViolationException(
                "could not execute statement [duplicate key value violates unique constraint \"uq_order_hose_delivery_version\"]"));

        assertThatThrownBy(() -> service.commit(draft(), 1))
            .isInstanceOf(DuplicateOrderException.class)
            .satisfies(e -> {
                DuplicateOrderException de = (DuplicateOrderException) e;
                assertThat(de.getHoseId()).isEqualTo("29673-2F900");
                assertThat(de.getDeliveryDate()).isEqualTo(LocalDate.of(2026, 2, 15));
                assertThat(de.getMasterVersion()).isEqualTo(1);
                assertThat(de.getMessage()).contains("중복 수주");
            });
    }

    @Test
    @DisplayName("UNIQUE 제약 (root cause 메시지) → DuplicateOrderException")
    void unique_violation_root_cause() {
        Throwable rootCause = new SQLException("ERROR: duplicate key value violates unique constraint \"uq_order_hose_delivery_version\"");
        when(repository.saveAndFlush(any(Order.class))).thenThrow(
            new DataIntegrityViolationException("wrapper message", rootCause));

        assertThatThrownBy(() -> service.commit(draft(), 1))
            .isInstanceOf(DuplicateOrderException.class);
    }

    @Test
    @DisplayName("기타 DataIntegrityViolation — 원본 예외 전파")
    void other_data_integrity_violation_rethrown() {
        when(repository.saveAndFlush(any(Order.class))).thenThrow(
            new DataIntegrityViolationException("null value in column foo"));

        assertThatThrownBy(() -> service.commit(draft(), 1))
            .isInstanceOf(DataIntegrityViolationException.class)
            .isNotInstanceOf(DuplicateOrderException.class);
    }

    @Test
    @DisplayName("orderId null — UUID 자동 발급")
    void null_order_id_generates_uuid() {
        OrderDraft d = new OrderDraft(null, "X", LocalDate.of(2026, 2, 15), 1,
            OrderType.WEEKLY, "내수");
        when(repository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.commit(d, 2);
        assertThat(result.getOrderId()).isNotNull();
    }
}
