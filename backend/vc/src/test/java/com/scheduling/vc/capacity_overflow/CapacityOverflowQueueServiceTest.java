package com.scheduling.vc.capacity_overflow;

import com.scheduling.master.api.ProductPriorityLookup;
import com.scheduling.master.api.ProductPrioritySummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BR-V12 capa 초과 큐 분리 — Sprint 7 단위 (REQ-FUNC-VC-022).
 */
class CapacityOverflowQueueServiceTest {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);

    private final ProductPriorityLookup lookup = mock(ProductPriorityLookup.class);
    private final CapacityOverflowQueueService service =
        new CapacityOverflowQueueService(lookup, CLOCK);

    @Test
    @DisplayName("capa 충분 — 모두 accepted, queue 0")
    void within_capa_all_accepted() {
        when(lookup.findEffectiveAt(java.time.LocalDate.of(2026, 6, 1)))
            .thenReturn(List.of());
        Map<String, Integer> required = Map.of("A", 30, "B", 40);

        CapacityOverflowQueueService.SplitResult r = service.split(required, 100);

        assertThat(r.totalAccepted()).isEqualTo(70);
        assertThat(r.totalQueued()).isZero();
        assertThat(r.requestQueue()).isEmpty();
    }

    @Test
    @DisplayName("capa 초과 — priority rank ASC 정렬 → 우선순위 hose 채택, 나머지 queue")
    void over_capa_priority_first() {
        when(lookup.findEffectiveAt(java.time.LocalDate.of(2026, 6, 1)))
            .thenReturn(List.of(
                new ProductPrioritySummary("A", (short) 1, "VIP", null, null),
                new ProductPrioritySummary("B", (short) 3, null,  null, null),
                new ProductPrioritySummary("C", (short) 2, "긴급", null, null)
            ));
        Map<String, Integer> required = new LinkedHashMap<>();
        required.put("A", 50);    // rank 1
        required.put("B", 50);    // rank 3
        required.put("C", 50);    // rank 2

        CapacityOverflowQueueService.SplitResult r = service.split(required, 100);

        // rank ASC — A(1)+C(2) = 100 accepted, B(3) 전체 queue
        assertThat(r.accepted()).containsExactly(
            Map.entry("A", 50), Map.entry("C", 50));
        assertThat(r.requestQueue()).containsExactly(Map.entry("B", 50));
        assertThat(r.totalAccepted()).isEqualTo(100);
        assertThat(r.totalQueued()).isEqualTo(50);
    }

    @Test
    @DisplayName("partial split — 1순위 일부 accepted, 일부 queue")
    void partial_split() {
        when(lookup.findEffectiveAt(java.time.LocalDate.of(2026, 6, 1)))
            .thenReturn(List.of(
                new ProductPrioritySummary("A", (short) 1, null, null, null)
            ));
        CapacityOverflowQueueService.SplitResult r =
            service.split(Map.of("A", 150), 100);

        assertThat(r.accepted().get("A")).isEqualTo(100);
        assertThat(r.requestQueue().get("A")).isEqualTo(50);
    }

    @Test
    @DisplayName("priority 미등록 hose → rank 99 fallback (후순위)")
    void unregistered_hose_fallback() {
        when(lookup.findEffectiveAt(java.time.LocalDate.of(2026, 6, 1)))
            .thenReturn(List.of(
                new ProductPrioritySummary("A", (short) 1, null, null, null)
            ));
        Map<String, Integer> required = new LinkedHashMap<>();
        required.put("A", 70);   // rank 1
        required.put("Z", 50);   // 미등록 → 99 (후순위)

        CapacityOverflowQueueService.SplitResult r = service.split(required, 100);

        assertThat(r.accepted().get("A")).isEqualTo(70);
        assertThat(r.accepted().get("Z")).isEqualTo(30);
        assertThat(r.requestQueue().get("Z")).isEqualTo(20);
    }

    @Test
    @DisplayName("required 빈 입력 → 빈 결과")
    void empty_input() {
        CapacityOverflowQueueService.SplitResult r = service.split(Map.of(), 100);
        assertThat(r.totalAccepted()).isZero();
        assertThat(r.totalQueued()).isZero();
    }
}
