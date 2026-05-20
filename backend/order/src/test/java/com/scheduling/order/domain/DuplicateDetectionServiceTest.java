package com.scheduling.order.domain;

import com.scheduling.common.metrics.SchedulingMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DuplicateDetectionService 회귀 — TK-02-1-3.
 *
 * <p>8 케이스 (Story DoD):
 *   1. batch 내 중복
 *   2. 기존 마스터와 중복
 *   3. 양쪽 모두 중복
 *   4. 중복 없음 (빈 결과)
 *   5. 빈 batch
 *   6. 다중 hose × 다중 date (cross-join 정확성)
 *   7. findDuplicateKeys 헬퍼
 *   8. metrics emit
 */
class DuplicateDetectionServiceTest {

    private OrderRepository repository;
    private SchedulingMetrics metrics;
    private DuplicateDetectionService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        metrics = mock(SchedulingMetrics.class);
        service = new DuplicateDetectionService(repository, metrics);
        when(repository.findActiveByHoseDeliveryPairs(any(), any())).thenReturn(List.of());
    }

    private OrderDraft draft(String hoseId, LocalDate date, int qty) {
        return new OrderDraft(UUID.randomUUID(), hoseId, date, qty, OrderType.FORECAST, "내수");
    }

    private Order existing(String hoseId, LocalDate date) {
        return new Order(UUID.randomUUID(), hoseId, date, 100,
            OrderType.CONFIRMED, "내수", 1, "ACTIVE", Instant.now());
    }

    @Test
    @DisplayName("batch 내 중복 — 같은 OrderKey 2개 → 1 그룹 (candidates=2)")
    void duplicates_within_batch() {
        LocalDate d = LocalDate.of(2026, 2, 15);
        List<OrderDraft> batch = List.of(
            draft("29673-2F900", d, 100),
            draft("29673-2F900", d, 200),
            draft("28422-2M100", d, 50)
        );

        List<DuplicateGroup> result = service.detect(batch);

        assertThat(result).hasSize(1);
        DuplicateGroup g = result.get(0);
        assertThat(g.key()).isEqualTo(new OrderKey("29673-2F900", d));
        assertThat(g.candidateCount()).isEqualTo(2);
        assertThat(g.hasExisting()).isFalse();
    }

    @Test
    @DisplayName("기존 마스터와 중복 — batch 1 + DB 1 → 1 그룹 (existing != null)")
    void duplicate_with_existing_master() {
        LocalDate d = LocalDate.of(2026, 2, 15);
        OrderDraft newDraft = draft("29673-2F900", d, 100);
        Order existingOrder = existing("29673-2F900", d);

        when(repository.findActiveByHoseDeliveryPairs(any(), any()))
            .thenReturn(List.of(existingOrder));

        List<DuplicateGroup> result = service.detect(List.of(newDraft));

        assertThat(result).hasSize(1);
        DuplicateGroup g = result.get(0);
        assertThat(g.candidateCount()).isEqualTo(1);
        assertThat(g.hasExisting()).isTrue();
        assertThat(g.existingMaster()).isEqualTo(existingOrder);
    }

    @Test
    @DisplayName("양쪽 중복 — batch 2 + DB 1 → 1 그룹 (candidates=2, existing != null)")
    void duplicate_within_batch_and_master() {
        LocalDate d = LocalDate.of(2026, 2, 15);
        List<OrderDraft> batch = List.of(
            draft("29673-2F900", d, 100),
            draft("29673-2F900", d, 200)
        );
        Order existingOrder = existing("29673-2F900", d);
        when(repository.findActiveByHoseDeliveryPairs(any(), any()))
            .thenReturn(List.of(existingOrder));

        List<DuplicateGroup> result = service.detect(batch);

        assertThat(result).hasSize(1);
        DuplicateGroup g = result.get(0);
        assertThat(g.candidateCount()).isEqualTo(2);
        assertThat(g.hasExisting()).isTrue();
    }

    @Test
    @DisplayName("중복 없음 — 모두 unique → 빈 리스트")
    void no_duplicates() {
        List<OrderDraft> batch = List.of(
            draft("29673-2F900", LocalDate.of(2026, 2, 15), 100),
            draft("28422-2M100", LocalDate.of(2026, 2, 16), 200),
            draft("25450-P7200", LocalDate.of(2026, 2, 17), 50)
        );
        List<DuplicateGroup> result = service.detect(batch);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("빈 batch — 빈 리스트 (예외 X, repository 호출 X)")
    void empty_batch() {
        assertThat(service.detect(List.of())).isEmpty();
        assertThat(service.detect(null)).isEmpty();
    }

    @Test
    @DisplayName("다중 hose × 다중 date — cross-join 결과에서 정확한 매칭만")
    void multi_hose_multi_date_cross_join_safe() {
        LocalDate d1 = LocalDate.of(2026, 2, 15);
        LocalDate d2 = LocalDate.of(2026, 2, 16);
        List<OrderDraft> batch = List.of(
            draft("29673-2F900", d1, 100),
            draft("28422-2M100", d2, 200)
        );
        // Repository returns cross-join (4건) 모두 → 정확한 키만 매칭되어야 함
        when(repository.findActiveByHoseDeliveryPairs(any(), any()))
            .thenReturn(List.of(
                existing("29673-2F900", d1),    // ✓ 매칭
                existing("29673-2F900", d2),    // ✗ batch 에 없음
                existing("28422-2M100", d1),    // ✗ batch 에 없음
                existing("28422-2M100", d2)     // ✓ 매칭
            ));

        List<DuplicateGroup> result = service.detect(batch);
        // 두 batch row 모두 기존 마스터와 충돌 → 2 그룹
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(DuplicateGroup::hasExisting);
    }

    @Test
    @DisplayName("findDuplicateKeys — Set<OrderKey> 산출")
    void find_duplicate_keys_helper() {
        LocalDate d = LocalDate.of(2026, 2, 15);
        List<OrderDraft> batch = new ArrayList<>(List.of(
            draft("A", d, 1),
            draft("A", d, 2),
            draft("B", d, 3)
        ));

        var keys = service.findDuplicateKeys(batch);
        assertThat(keys).containsExactly(new OrderKey("A", d));
    }

    @Test
    @DisplayName("metrics — 중복 발견 시 카운터 emit (detected_batch + within_batch + vs_master)")
    void metrics_emitted_on_duplicate() {
        LocalDate d = LocalDate.of(2026, 2, 15);
        when(repository.findActiveByHoseDeliveryPairs(any(), any()))
            .thenReturn(List.of(existing("A", d)));

        service.detect(List.of(draft("A", d, 1), draft("A", d, 2)));

        org.mockito.Mockito.verify(metrics).increment("order_duplicate", "detected_batch");
        org.mockito.Mockito.verify(metrics).increment("order_duplicate", "vs_master");
        org.mockito.Mockito.verify(metrics).increment("order_duplicate", "within_batch");
    }
}
