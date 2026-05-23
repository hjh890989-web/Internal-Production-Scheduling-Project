package com.scheduling.master.kd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BR-V13 KdOrder 도메인 — Sprint 7 (consume + status 자동 전이).
 */
class KdOrderTest {

    private static final Instant T0 = Instant.parse("2026-06-01T00:00:00Z");
    private static final LocalDate D = LocalDate.of(2026, 5, 1);

    private KdOrder newOrder(int orderQty) {
        return new KdOrder(UUID.randomUUID(), "29673-2R060",
            orderQty, orderQty, D, "CUST-A", KdOrder.Status.OPEN, T0, "seed");
    }

    @Test
    @DisplayName("OPEN → consume 일부 → PARTIAL")
    void partial_consume() {
        KdOrder k = newOrder(100);
        int actual = k.consume(30, T0, "supplement-svc");
        assertThat(actual).isEqualTo(30);
        assertThat(k.getRemainingQty()).isEqualTo(70);
        assertThat(k.getStatus()).isEqualTo(KdOrder.Status.PARTIAL);
    }

    @Test
    @DisplayName("OPEN → consume 전량 → FILLED")
    void full_consume() {
        KdOrder k = newOrder(100);
        int actual = k.consume(100, T0, "supplement-svc");
        assertThat(actual).isEqualTo(100);
        assertThat(k.getRemainingQty()).isZero();
        assertThat(k.getStatus()).isEqualTo(KdOrder.Status.FILLED);
    }

    @Test
    @DisplayName("PARTIAL → consume 잔량 초과 → 가능량만 소진 (FILLED)")
    void over_consume_caps_at_remaining() {
        KdOrder k = newOrder(100);
        k.consume(60, T0, "x");
        int actual = k.consume(50, T0, "y");      // 잔량 40, 요청 50
        assertThat(actual).isEqualTo(40);
        assertThat(k.getRemainingQty()).isZero();
        assertThat(k.getStatus()).isEqualTo(KdOrder.Status.FILLED);
    }

    @Test
    @DisplayName("orderQty ≤ 0 → IllegalArgumentException")
    void invalid_order_qty() {
        assertThatThrownBy(() -> new KdOrder(UUID.randomUUID(), "X",
            0, 0, D, null, KdOrder.Status.OPEN, T0, "seed"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("remainingQty > orderQty → IllegalArgumentException")
    void remaining_over_order() {
        assertThatThrownBy(() -> new KdOrder(UUID.randomUUID(), "X",
            100, 150, D, null, KdOrder.Status.OPEN, T0, "seed"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("remainingQty");
    }

    @Test
    @DisplayName("consume qty ≤ 0 → IllegalArgumentException")
    void invalid_consume_qty() {
        KdOrder k = newOrder(100);
        assertThatThrownBy(() -> k.consume(0, T0, "x"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
