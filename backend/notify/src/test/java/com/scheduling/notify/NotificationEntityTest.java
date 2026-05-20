package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.api.NotificationChannel;
import com.scheduling.notify.api.NotificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NotificationEntity 상태머신 — TK-03-3-2.
 *
 * <p>DISPATCHED → SENT → DELIVERED → ACKNOWLEDGED|FAILED 전이 + idempotent ack.
 */
class NotificationEntityTest {

    private static final Instant T0 = Instant.parse("2026-05-20T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(10);
    private static final Instant T2 = T0.plusSeconds(30);

    private NotificationEntity create() {
        return new NotificationEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            NotificationChannel.IN_APP,
            ChangeSeverity.CRITICAL,
            "PLANNER",
            "29673-2F900",
            LocalDate.of(2026, 2, 15),
            "qty 변경",
            T0
        );
    }

    @Test
    @DisplayName("초기 상태 — DISPATCHED + retryCount=0")
    void initial_state_dispatched() {
        NotificationEntity n = create();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.DISPATCHED);
        assertThat(n.getRetryCount()).isZero();
        assertThat(n.getSentAt()).isNull();
        assertThat(n.getAcknowledgedAt()).isNull();
    }

    @Test
    @DisplayName("markSent → SENT 상태 + sentAt 기록")
    void mark_sent_transitions() {
        NotificationEntity n = create();
        n.markSent(T1);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isEqualTo(T1);
    }

    @Test
    @DisplayName("acknowledge — 최초 호출 changed=true, ack 시각 기록")
    void acknowledge_first_call() {
        NotificationEntity n = create();
        assertThat(n.acknowledge(T1)).isTrue();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.ACKNOWLEDGED);
        assertThat(n.getAcknowledgedAt()).isEqualTo(T1);
    }

    @Test
    @DisplayName("acknowledge 재호출 → idempotent (changed=false, 시각 불변)")
    void acknowledge_idempotent() {
        NotificationEntity n = create();
        n.acknowledge(T1);

        assertThat(n.acknowledge(T2)).isFalse();
        assertThat(n.getAcknowledgedAt()).isEqualTo(T1);   // 최초 시각 유지
    }

    @Test
    @DisplayName("markFailed → FAILED + errorMessage 기록")
    void mark_failed() {
        NotificationEntity n = create();
        n.markFailed(T1, "Kakao timeout");
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.getFailedAt()).isEqualTo(T1);
        assertThat(n.getErrorMessage()).isEqualTo("Kakao timeout");
    }

    @Test
    @DisplayName("incrementRetry — retryCount 증가")
    void increment_retry() {
        NotificationEntity n = create();
        n.incrementRetry();
        n.incrementRetry();
        assertThat(n.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("dispatchedAt null → IllegalArgumentException (BR-X04 Clock 강제)")
    void null_dispatched_at_rejected() {
        assertThatThrownBy(() -> new NotificationEntity(
            UUID.randomUUID(), UUID.randomUUID(),
            NotificationChannel.IN_APP, ChangeSeverity.CRITICAL,
            "PLANNER", "29673-2F900",
            LocalDate.of(2026, 2, 15),
            "qty", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dispatchedAt");
    }
}
