package com.scheduling.master.vc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VcConstraint 도메인 로직 단위 회귀 — TK-04-1-1.
 *
 * <p>도메인 메서드만 검증 (DB 없이). JPA mapping + LISTEN/NOTIFY 트리거 검증은
 * Testcontainers IT 에서.
 */
class VcConstraintTest {

    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    private VcConstraint vc(boolean lpTop, boolean lpUpmid, boolean lpLowmid, boolean lpBot,
                            boolean icTop, boolean icMid, boolean icBot,
                            short compositeCount, Short lpMolds, Short icMolds) {
        return new VcConstraint(
            "29673-2F900", 45, compositeCount,
            lpMolds, (short) 20,
            lpTop, lpUpmid, lpLowmid, lpBot,
            icMolds, (short) 20,
            icTop, icMid, icBot,
            T0, "test"
        );
    }

    // ---------- isEligibleFor ----------

    @Test
    @DisplayName("isEligibleFor — 7 슬롯 위치 정확 매핑")
    void isEligibleFor_maps_each_slot() {
        VcConstraint v = vc(true, false, true, false, true, true, false, (short) 1, (short) 1, (short) 1);
        assertThat(v.isEligibleFor(SlotPosition.LP_TOP)).isTrue();
        assertThat(v.isEligibleFor(SlotPosition.LP_UPMID)).isFalse();
        assertThat(v.isEligibleFor(SlotPosition.LP_LOWMID)).isTrue();
        assertThat(v.isEligibleFor(SlotPosition.LP_BOT)).isFalse();
        assertThat(v.isEligibleFor(SlotPosition.IC_TOP)).isTrue();
        assertThat(v.isEligibleFor(SlotPosition.IC_MID)).isTrue();
        assertThat(v.isEligibleFor(SlotPosition.IC_BOT)).isFalse();
    }

    // ---------- isUnschedulable (BR-V11) ----------

    @Test
    @DisplayName("isUnschedulable — 7 슬롯 모두 X → true")
    void all_slots_false_is_unschedulable() {
        VcConstraint v = vc(false, false, false, false, false, false, false, (short) 1, (short) 1, (short) 1);
        assertThat(v.isUnschedulable()).isTrue();
    }

    @Test
    @DisplayName("isUnschedulable — 슬롯 1개라도 O → false")
    void any_slot_true_is_schedulable() {
        VcConstraint onlyLpTop = vc(true, false, false, false, false, false, false, (short) 1, (short) 1, (short) 1);
        VcConstraint onlyIcBot = vc(false, false, false, false, false, false, true, (short) 1, (short) 1, (short) 1);
        assertThat(onlyLpTop.isUnschedulable()).isFalse();
        assertThat(onlyIcBot.isUnschedulable()).isFalse();
    }

    // ---------- yieldPerRotation ----------

    @Test
    @DisplayName("yieldPerRotation — LP = compositeCount × lpMoldsPerAngle")
    void yield_lp() {
        VcConstraint v = vc(true, true, false, false, true, true, false, (short) 2, (short) 5, (short) 3);
        assertThat(v.yieldPerRotation(MachineType.LP)).isEqualTo(10);   // 2 × 5
    }

    @Test
    @DisplayName("yieldPerRotation — IC = compositeCount × icMoldsPerAngle")
    void yield_ic() {
        VcConstraint v = vc(true, true, false, false, true, true, false, (short) 3, (short) 5, (short) 4);
        assertThat(v.yieldPerRotation(MachineType.IC)).isEqualTo(12);   // 3 × 4
    }

    @Test
    @DisplayName("yieldPerRotation — moldsPerAngle null → 0 (적용 불가 머신)")
    void yield_null_molds_returns_zero() {
        VcConstraint v = vc(true, true, false, false, false, false, false, (short) 1, (short) 5, null);
        assertThat(v.yieldPerRotation(MachineType.IC)).isZero();
    }

    // ---------- compositeCount 검증 (REF-11 도메인 1·2·3·6) ----------

    @Test
    @DisplayName("compositeCount = 1·2·3·6 모두 허용")
    void composite_count_valid_values() {
        for (short ok : new short[]{1, 2, 3, 6}) {
            VcConstraint v = vc(true, false, false, false, false, false, false, ok, (short) 1, (short) 1);
            assertThat(v.getCompositeCount()).isEqualTo(ok);
        }
    }

    @Test
    @DisplayName("compositeCount 4·5 → IllegalArgumentException (REF-11 도메인 외)")
    void composite_count_invalid_rejected() {
        for (short bad : new short[]{0, 4, 5, 7, 10}) {
            assertThatThrownBy(() ->
                vc(true, false, false, false, false, false, false, bad, (short) 1, (short) 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("compositeCount");
        }
    }

    // ---------- BR-X04 ----------

    @Test
    @DisplayName("updatedAt null → IllegalArgumentException (BR-X04 Clock 강제)")
    void null_updated_at_rejected() {
        assertThatThrownBy(() -> new VcConstraint(
            "X", 1, (short) 1, null, null,
            true, false, false, false, null, null, false, false, false,
            null, "test"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("updatedAt");
    }

    // ---------- SlotPosition.of / machineType ----------

    @Test
    @DisplayName("SlotPosition.of(LP) — 4 슬롯 / of(IC) — 3 슬롯")
    void slot_position_of_machine_type() {
        assertThat(SlotPosition.of(MachineType.LP)).containsExactly(
            SlotPosition.LP_TOP, SlotPosition.LP_UPMID,
            SlotPosition.LP_LOWMID, SlotPosition.LP_BOT);
        assertThat(SlotPosition.of(MachineType.IC)).containsExactly(
            SlotPosition.IC_TOP, SlotPosition.IC_MID, SlotPosition.IC_BOT);
    }

    @Test
    @DisplayName("SlotPosition.machineType — LP_*/IC_* 접두 매핑")
    void slot_position_machine_type() {
        assertThat(SlotPosition.LP_TOP.machineType()).isEqualTo(MachineType.LP);
        assertThat(SlotPosition.LP_BOT.machineType()).isEqualTo(MachineType.LP);
        assertThat(SlotPosition.IC_TOP.machineType()).isEqualTo(MachineType.IC);
        assertThat(SlotPosition.IC_BOT.machineType()).isEqualTo(MachineType.IC);
    }
}
