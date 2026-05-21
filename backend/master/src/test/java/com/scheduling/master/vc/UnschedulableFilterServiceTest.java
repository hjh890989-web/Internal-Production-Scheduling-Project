package com.scheduling.master.vc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UnschedulableFilterService 회귀 — TK-04-2-1 (BR-V11).
 */
class UnschedulableFilterServiceTest {

    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    private SlotCompatibilityMatrixService matrixService;
    private UnschedulableFilterService service;

    @BeforeEach
    void setUp() {
        matrixService = mock(SlotCompatibilityMatrixService.class);
        service = new UnschedulableFilterService(matrixService);
    }

    private SlotCompatibilityMatrix matrix(int version, Set<String> unsched) {
        return new SlotCompatibilityMatrix(
            version, T0,
            Map.of(),
            Map.of(),
            unsched
        );
    }

    @Test
    @DisplayName("schedulable + unschedulable 정확 분리 + matrixVersion 동기화")
    void separates_schedulable_and_unschedulable() {
        when(matrixService.current()).thenReturn(
            matrix(7, Set.of("ZERO-001", "ZERO-002")));

        FilterResult result = service.separate(List.of(
            "OK-001", "ZERO-001", "OK-002", "ZERO-002", "OK-003"));

        assertThat(result.schedulable()).containsExactly("OK-001", "OK-002", "OK-003");
        assertThat(result.unschedulable()).containsExactly("ZERO-001", "ZERO-002");
        assertThat(result.matrixVersion()).isEqualTo(7);
        assertThat(result.hasUnschedulable()).isTrue();
        assertThat(result.unschedulableCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("모든 hose_id schedulable → unschedulable empty")
    void all_schedulable() {
        when(matrixService.current()).thenReturn(matrix(1, Set.of("ZERO-X")));

        FilterResult result = service.separate(List.of("A", "B", "C"));

        assertThat(result.schedulable()).hasSize(3);
        assertThat(result.unschedulable()).isEmpty();
        assertThat(result.hasUnschedulable()).isFalse();
    }

    @Test
    @DisplayName("모든 hose_id unschedulable → schedulable empty")
    void all_unschedulable() {
        when(matrixService.current()).thenReturn(matrix(2, Set.of("A", "B", "C")));

        FilterResult result = service.separate(List.of("A", "B", "C"));

        assertThat(result.schedulable()).isEmpty();
        assertThat(result.unschedulable()).containsExactly("A", "B", "C");
        assertThat(result.unschedulableCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("빈 입력 → 빈 결과 + matrixVersion")
    void empty_input() {
        when(matrixService.current()).thenReturn(matrix(3, Set.of()));

        FilterResult result = service.separate(List.of());

        assertThat(result.schedulable()).isEmpty();
        assertThat(result.unschedulable()).isEmpty();
        assertThat(result.matrixVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("null 입력 → 빈 결과 (defensive)")
    void null_input() {
        when(matrixService.current()).thenReturn(matrix(4, Set.of("X")));

        FilterResult result = service.separate(null);

        assertThat(result.schedulable()).isEmpty();
        assertThat(result.unschedulable()).isEmpty();
    }

    @Test
    @DisplayName("matrix 미초기화 (null) → 모든 hose_id schedulable fallback")
    void matrix_null_falls_back_schedulable() {
        when(matrixService.current()).thenReturn(null);

        FilterResult result = service.separate(List.of("A", "B"));

        assertThat(result.schedulable()).containsExactly("A", "B");
        assertThat(result.unschedulable()).isEmpty();
        assertThat(result.matrixVersion()).isZero();
    }

    @Test
    @DisplayName("중복 hose_id — 결과에 순서·중복 보존")
    void duplicate_hose_ids_preserved() {
        when(matrixService.current()).thenReturn(matrix(5, Set.of("ZERO")));

        FilterResult result = service.separate(List.of("A", "ZERO", "A", "ZERO", "B"));

        assertThat(result.schedulable()).containsExactly("A", "A", "B");
        assertThat(result.unschedulable()).containsExactly("ZERO", "ZERO");
    }

    @Test
    @DisplayName("결과 immutability — UnsupportedOperationException")
    void result_lists_are_immutable() {
        when(matrixService.current()).thenReturn(matrix(1, Set.of("X")));

        FilterResult result = service.separate(List.of("OK", "X"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            result.schedulable().add("Y")).isInstanceOf(UnsupportedOperationException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            result.unschedulable().add("Z")).isInstanceOf(UnsupportedOperationException.class);
    }
}
