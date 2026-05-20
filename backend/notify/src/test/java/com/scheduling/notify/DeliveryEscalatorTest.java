package com.scheduling.notify;

import com.scheduling.common.enums.ChangeSeverity;
import com.scheduling.notify.api.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DeliveryEscalator 회귀 — TK-03-3-2 (REQ-NF-KPI-015).
 *
 * <p>1분 SLA 위반 Critical 알림 retry_count 증가 + Repository.saveAll 호출.
 */
class DeliveryEscalatorTest {

    private static final Instant NOW = Instant.parse("2026-05-20T10:01:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"));

    private NotificationRepository repo;
    private DeliveryEscalator escalator;

    @BeforeEach
    void setUp() {
        repo = mock(NotificationRepository.class);
        escalator = new DeliveryEscalator(repo, CLOCK);
    }

    private NotificationEntity dispatched(Instant at) {
        return new NotificationEntity(
            UUID.randomUUID(), UUID.randomUUID(),
            NotificationChannel.KAKAOTALK,
            ChangeSeverity.CRITICAL,
            "PLANNER", "29673-2F900",
            LocalDate.of(2026, 2, 15),
            "qty 변경", at);
    }

    @Test
    @DisplayName("overdue 0건 → saveAll 호출 X")
    void no_overdue_no_save() {
        when(repo.findCriticalOverdue(eq(ChangeSeverity.CRITICAL), any())).thenReturn(List.of());

        escalator.escalate();

        verify(repo, never()).saveAll(any());
    }

    @Test
    @DisplayName("overdue 3건 → 각 retry_count++ + saveAll 호출")
    void overdue_entries_incremented_and_saved() {
        NotificationEntity n1 = dispatched(NOW.minusSeconds(120));
        NotificationEntity n2 = dispatched(NOW.minusSeconds(90));
        NotificationEntity n3 = dispatched(NOW.minusSeconds(75));
        when(repo.findCriticalOverdue(eq(ChangeSeverity.CRITICAL), any()))
            .thenReturn(List.of(n1, n2, n3));

        escalator.escalate();

        assertThat(n1.getRetryCount()).isEqualTo(1);
        assertThat(n2.getRetryCount()).isEqualTo(1);
        assertThat(n3.getRetryCount()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationEntity>> cap = ArgumentCaptor.forClass(List.class);
        verify(repo, times(1)).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("threshold 계산 — NOW - 1분")
    void threshold_is_one_minute_before_now() {
        when(repo.findCriticalOverdue(eq(ChangeSeverity.CRITICAL), any())).thenReturn(List.of());

        escalator.escalate();

        ArgumentCaptor<Instant> cap = ArgumentCaptor.forClass(Instant.class);
        verify(repo).findCriticalOverdue(eq(ChangeSeverity.CRITICAL), cap.capture());
        assertThat(cap.getValue()).isEqualTo(NOW.minusSeconds(60));
    }
}
