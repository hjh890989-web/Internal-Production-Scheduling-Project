package com.scheduling.master.vc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SlotCompatibilityMatrix.build 단위 회귀 — TK-04-1-2.
 *
 * <p>매트릭스 build 로직만 검증 (DB 없이). LISTEN/NOTIFY 통합은 IT 에서.
 */
class SlotCompatibilityMatrixTest {

    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    private VcConstraint vc(String hoseId,
                            boolean lpTop, boolean lpUpmid, boolean lpLowmid, boolean lpBot,
                            boolean icTop, boolean icMid, boolean icBot) {
        return new VcConstraint(
            hoseId, 45, (short) 1,
            (short) 1, (short) 20,
            lpTop, lpUpmid, lpLowmid, lpBot,
            (short) 1, (short) 20,
            icTop, icMid, icBot,
            T0, "test"
        );
    }

    @Test
    @DisplayName("build — 빈 입력 → version + 빈 매트릭스")
    void build_empty() {
        SlotCompatibilityMatrix m = SlotCompatibilityMatrix.build(1, T0, List.of());
        assertThat(m.version()).isEqualTo(1);
        assertThat(m.byHose()).isEmpty();
        assertThat(m.unschedulableHoseIds()).isEmpty();
        // bySlot 은 7 슬롯 모두 키 존재하지만 빈 Set
        for (SlotPosition s : SlotPosition.values()) {
            assertThat(m.eligibleHoseIdsFor(s)).isEmpty();
        }
    }

    @Test
    @DisplayName("build — 3 품번 × 7 슬롯 = 21 셀 정확 매핑")
    void build_three_hoses() {
        List<VcConstraint> input = List.of(
            vc("A001", true, false, false, false, false, false, false),
            vc("A002", false, true, true, false, true, true, false),
            vc("A003", false, false, false, true, false, false, true)
        );

        SlotCompatibilityMatrix m = SlotCompatibilityMatrix.build(5, T0, input);

        assertThat(m.version()).isEqualTo(5);
        assertThat(m.byHose()).hasSize(3);

        // isEligible 정확
        assertThat(m.isEligible("A001", SlotPosition.LP_TOP)).isTrue();
        assertThat(m.isEligible("A001", SlotPosition.LP_UPMID)).isFalse();
        assertThat(m.isEligible("A002", SlotPosition.LP_UPMID)).isTrue();
        assertThat(m.isEligible("A002", SlotPosition.IC_TOP)).isTrue();
        assertThat(m.isEligible("A003", SlotPosition.LP_BOT)).isTrue();
        assertThat(m.isEligible("A003", SlotPosition.IC_BOT)).isTrue();
        // 미존재 hose_id → false
        assertThat(m.isEligible("UNKNOWN", SlotPosition.LP_TOP)).isFalse();
    }

    @Test
    @DisplayName("eligibleHoseIdsFor — 슬롯 → 가능 품번 역인덱스")
    void by_slot_reverse_index() {
        SlotCompatibilityMatrix m = SlotCompatibilityMatrix.build(1, T0, List.of(
            vc("A001", true, false, false, false, false, false, false),
            vc("A002", true, true, false, false, false, false, false),
            vc("A003", false, true, false, false, false, false, false)
        ));

        assertThat(m.eligibleHoseIdsFor(SlotPosition.LP_TOP))
            .containsExactlyInAnyOrder("A001", "A002");
        assertThat(m.eligibleHoseIdsFor(SlotPosition.LP_UPMID))
            .containsExactlyInAnyOrder("A002", "A003");
        assertThat(m.eligibleHoseIdsFor(SlotPosition.LP_BOT)).isEmpty();
    }

    @Test
    @DisplayName("eligibleSlotsFor — 품번 → 가능 슬롯 집합")
    void eligible_slots_for_hose() {
        SlotCompatibilityMatrix m = SlotCompatibilityMatrix.build(1, T0, List.of(
            vc("A001", true, false, true, false, false, true, false)
        ));

        assertThat(m.eligibleSlotsFor("A001"))
            .containsExactlyInAnyOrder(SlotPosition.LP_TOP, SlotPosition.LP_LOWMID, SlotPosition.IC_MID);
        assertThat(m.eligibleSlotsFor("UNKNOWN")).isEmpty();
    }

    @Test
    @DisplayName("unschedulableHoseIds — BR-V11 모든 7 슬롯 X 인 품번만")
    void unschedulable_hose_ids() {
        SlotCompatibilityMatrix m = SlotCompatibilityMatrix.build(1, T0, List.of(
            vc("UNSCHED", false, false, false, false, false, false, false),
            vc("OK", true, false, false, false, false, false, false)
        ));

        assertThat(m.unschedulableHoseIds()).containsExactly("UNSCHED");
    }

    @Test
    @DisplayName("immutability — byHose / bySlot / unschedulable 모두 UnsupportedOperationException")
    void built_matrix_is_immutable() {
        SlotCompatibilityMatrix m = SlotCompatibilityMatrix.build(1, T0, List.of(
            vc("A001", true, false, false, false, false, false, false)
        ));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            m.byHose().put("X", java.util.Map.of()))
            .isInstanceOf(UnsupportedOperationException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            m.unschedulableHoseIds().add("X"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("build 성능 — 47품번 × 7 슬롯 빌드 ≤ 50ms (TC-VC-001 buffer)")
    void build_performance_47_hoses() {
        // 47품번 합성
        java.util.List<VcConstraint> input = new java.util.ArrayList<>(47);
        for (int i = 0; i < 47; i++) {
            input.add(vc(String.format("29673-%05d", i),
                i % 2 == 0, i % 3 == 0, i % 5 == 0, i % 7 == 0,
                i % 2 == 1, i % 3 == 1, i % 5 == 1));
        }

        long startNanos = System.nanoTime();
        SlotCompatibilityMatrix m = SlotCompatibilityMatrix.build(1, T0, input);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertThat(m.byHose()).hasSize(47);
        assertThat(elapsedMs).as("47품번 빌드 — SLA 1000ms 의 5% 미만").isLessThan(50L);
    }
}
