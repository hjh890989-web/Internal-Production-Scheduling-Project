package com.scheduling.vc.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RotationSlot 진리표 — TK-05-1-3 (BR-V04 18 회전).
 */
class RotationSlotTest {

    private static final LocalDate D = LocalDate.of(2026, 2, 16);

    @ParameterizedTest(name = "rotation_no={0} → IllegalArgumentException")
    @ValueSource(ints = {0, -1, 19, 100})
    @DisplayName("BR-V04 위반 rotationNo (0/-1/19/100) → IllegalArgumentException")
    void invalid_rotation_no_throws(int n) {
        assertThatThrownBy(() -> new RotationSlot(D, "LP-01", n, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rotationNo");
    }

    @ParameterizedTest(name = "rotation_no={0} 정상 생성")
    @ValueSource(ints = {1, 2, 7, 8, 9, 10, 17, 18})
    @DisplayName("BR-V04 정상 rotationNo (1·2·7·8·9·10·17·18) — record 생성")
    void valid_rotation_no_creates(int n) {
        RotationSlot slot = new RotationSlot(D, "LP-01", n, 1);
        assertThat(slot.rotationNo()).isEqualTo(n);
    }

    @Test
    @DisplayName("rotation 1~8 → isDayRotation true (BR-V04 주간)")
    void rotation_1_to_8_is_day() {
        for (int r = 1; r <= 8; r++) {
            RotationSlot slot = new RotationSlot(D, "LP-01", r, 1);
            assertThat(slot.isDayRotation()).as("rotation %d day", r).isTrue();
            assertThat(slot.isNightRotation()).as("rotation %d not night", r).isFalse();
        }
    }

    @Test
    @DisplayName("rotation 9~18 → isNightRotation true (BR-V04 야간)")
    void rotation_9_to_18_is_night() {
        for (int r = 9; r <= 18; r++) {
            RotationSlot slot = new RotationSlot(D, "LP-01", r, 1);
            assertThat(slot.isNightRotation()).as("rotation %d night", r).isTrue();
            assertThat(slot.isDayRotation()).as("rotation %d not day", r).isFalse();
        }
    }

    @Test
    @DisplayName("date null / machineId null/blank / slotPosition <1 → IllegalArgumentException")
    void invalid_other_fields_rejected() {
        assertThatThrownBy(() -> new RotationSlot(null, "LP-01", 1, 1))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("date");
        assertThatThrownBy(() -> new RotationSlot(D, null, 1, 1))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("machineId");
        assertThatThrownBy(() -> new RotationSlot(D, "  ", 1, 1))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("machineId");
        assertThatThrownBy(() -> new RotationSlot(D, "LP-01", 1, 0))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("slotPosition");
    }

    @Test
    @DisplayName("record equality — 같은 4-tuple 동일 객체")
    void record_equality() {
        RotationSlot a = new RotationSlot(D, "LP-01", 5, 3);
        RotationSlot b = new RotationSlot(D, "LP-01", 5, 3);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
