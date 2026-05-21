package com.scheduling.vc.yield;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YieldMatrixTest {

    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    @Test
    @DisplayName("lookup — LP 정확, IC 미정의 → empty")
    void lookup_present_and_missing() {
        YieldMatrix m = new YieldMatrix(1, T0,
            Map.of("A001", 5),
            Map.of(),
            Set.of());
        assertThat(m.lookup("A001", "LP")).contains(5);
        assertThat(m.lookup("A001", "IC")).isEmpty();
        assertThat(m.lookup("UNKNOWN", "LP")).isEmpty();
    }

    @Test
    @DisplayName("isUnschedulable — 양쪽 0 인 품번 식별")
    void unschedulable_lookup() {
        YieldMatrix m = new YieldMatrix(1, T0,
            Map.of("OK", 1),
            Map.of(),
            Set.of("ZERO-001", "ZERO-002"));
        assertThat(m.isUnschedulable("ZERO-001")).isTrue();
        assertThat(m.isUnschedulable("OK")).isFalse();
    }

    @Test
    @DisplayName("immutability — Map / Set 변경 시도 → UnsupportedOperationException")
    void map_set_immutable() {
        YieldMatrix m = new YieldMatrix(1, T0,
            Map.of("A", 1), Map.of("A", 2), Set.of("X"));
        assertThatThrownBy(() -> m.lpYields().put("B", 1))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> m.unschedulableYields().add("Y"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("null lpYields/icYields/unschedulable → 빈 collection (defensive)")
    void null_collections_defensive() {
        YieldMatrix m = new YieldMatrix(1, T0, null, null, null);
        assertThat(m.lpYields()).isEmpty();
        assertThat(m.icYields()).isEmpty();
        assertThat(m.unschedulableYields()).isEmpty();
        assertThat(m.lookup("X", "LP")).isEmpty();
    }
}
