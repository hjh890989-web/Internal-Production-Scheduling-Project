package com.scheduling.master.vc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VcMachine 도메인 단위 — TK-05-1-3 (BR-V05 capa 진리표).
 */
class VcMachineTest {

    private static final Instant T0 = Instant.parse("2026-05-21T00:00:00Z");

    @Test
    @DisplayName("LP 가류기 — totalRotationsPerDay=18, dailyCapacity=144 (BR-V05)")
    void lp_capacity_is_144() {
        VcMachine lp = new VcMachine("LP-01", MachineType.LP,
            (short) 8, (short) 8, (short) 10, true, T0, "test");
        assertThat(lp.totalRotationsPerDay()).isEqualTo(18);
        assertThat(lp.dailyCapacity()).isEqualTo(144);
    }

    @Test
    @DisplayName("IC 가류기 — totalRotationsPerDay=18, dailyCapacity=108 (BR-V05)")
    void ic_capacity_is_108() {
        VcMachine ic = new VcMachine("IC-01", MachineType.IC,
            (short) 6, (short) 8, (short) 10, true, T0, "test");
        assertThat(ic.totalRotationsPerDay()).isEqualTo(18);
        assertThat(ic.dailyCapacity()).isEqualTo(108);
    }

    @Test
    @DisplayName("LP totalSlots ≠ 8 → IllegalArgumentException (BR-V05 CHECK 정합)")
    void lp_must_have_8_slots() {
        assertThatThrownBy(() -> new VcMachine("LP-X", MachineType.LP,
            (short) 6, (short) 8, (short) 10, true, T0, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LP");
    }

    @Test
    @DisplayName("IC totalSlots ≠ 6 → IllegalArgumentException")
    void ic_must_have_6_slots() {
        assertThatThrownBy(() -> new VcMachine("IC-X", MachineType.IC,
            (short) 8, (short) 8, (short) 10, true, T0, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("IC");
    }

    @Test
    @DisplayName("updatedAt null → IllegalArgumentException (BR-X04 Clock)")
    void null_updated_at_rejected() {
        assertThatThrownBy(() -> new VcMachine("LP-01", MachineType.LP,
            (short) 8, (short) 8, (short) 10, true, null, "test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("updatedAt");
    }

    @Test
    @DisplayName("주간 12 + 야간 12 = 24 회전 (가상 24h 가동) — capa 변경 즉시 반영")
    void custom_rotations_extension() {
        VcMachine custom = new VcMachine("LP-01", MachineType.LP,
            (short) 8, (short) 12, (short) 12, true, T0, "test");
        assertThat(custom.totalRotationsPerDay()).isEqualTo(24);
        assertThat(custom.dailyCapacity()).isEqualTo(192);
    }
}
