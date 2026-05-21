package com.scheduling.vc.yield;

import com.scheduling.master.api.VcConstraintLookup;
import com.scheduling.master.api.VcConstraintSummary;
import com.scheduling.vc.domain.RotationSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AngleCapacityValidatorTest {

    private static final LocalDate D = LocalDate.of(2026, 2, 16);

    private VcConstraintLookup lookup;
    private AngleCapacityValidator validator;

    @BeforeEach
    void setUp() {
        lookup = mock(VcConstraintLookup.class);
        validator = new AngleCapacityValidator(lookup);
    }

    private VcConstraintSummary cs(String hose, Short lpAngle, Short icAngle) {
        return new VcConstraintSummary(hose, (short) 1, (short) 1, lpAngle, (short) 1, icAngle);
    }

    @Test
    @DisplayName("동시 점유 ≤ 앵글 → 위반 0")
    void within_capacity_no_violations() {
        when(lookup.findById("A001")).thenReturn(Optional.of(cs("A001", (short) 4, (short) 3)));

        List<AngleCapacityViolation> v = validator.validate(Map.of(
            "A001", List.of(
                new RotationSlot(D, "LP-01", 5, 1),
                new RotationSlot(D, "LP-01", 5, 2),
                new RotationSlot(D, "LP-01", 5, 3),
                new RotationSlot(D, "LP-01", 5, 4)
            )));

        assertThat(v).isEmpty();
    }

    @Test
    @DisplayName("앵글 1 + 같은 회전·다른 슬롯 2 → 위반 1")
    void over_capacity_same_rotation() {
        when(lookup.findById("A001")).thenReturn(Optional.of(cs("A001", (short) 1, (short) 1)));

        List<AngleCapacityViolation> v = validator.validate(Map.of(
            "A001", List.of(
                new RotationSlot(D, "LP-01", 5, 1),
                new RotationSlot(D, "LP-01", 5, 2)
            )));

        assertThat(v).hasSize(1);
        AngleCapacityViolation viol = v.get(0);
        assertThat(viol.hoseId()).isEqualTo("A001");
        assertThat(viol.machineType()).isEqualTo("LP");
        assertThat(viol.rotationNo()).isEqualTo(5);
        assertThat(viol.actualSlotsUsed()).isEqualTo(2);
        assertThat(viol.allowedAngles()).isEqualTo(1);
        assertThat(viol.userMessage()).contains("앵글 과초과").contains("A001").contains("BR-V06");
    }

    @Test
    @DisplayName("다른 회전 시점 — 앵글 재사용, 위반 X")
    void different_rotation_reuses_angle() {
        when(lookup.findById("A001")).thenReturn(Optional.of(cs("A001", (short) 1, (short) 1)));

        List<AngleCapacityViolation> v = validator.validate(Map.of(
            "A001", List.of(
                new RotationSlot(D, "LP-01", 5, 1),
                new RotationSlot(D, "LP-01", 6, 1),   // 다른 회전 — 앵글 재셋업
                new RotationSlot(D, "LP-01", 7, 1)
            )));

        assertThat(v).isEmpty();
    }

    @Test
    @DisplayName("다른 머신 (LP-01 vs LP-02) — 별도 앵글, 같은 hose/회전 도 위반 X")
    void different_machine_separate_angle() {
        when(lookup.findById("A001")).thenReturn(Optional.of(cs("A001", (short) 1, (short) 1)));

        List<AngleCapacityViolation> v = validator.validate(Map.of(
            "A001", List.of(
                new RotationSlot(D, "LP-01", 5, 1),
                new RotationSlot(D, "LP-02", 5, 1)    // 다른 머신
            )));

        assertThat(v).isEmpty();
    }

    @Test
    @DisplayName("다른 hose_id — 독립 그룹")
    void different_hose_independent() {
        when(lookup.findById("A001")).thenReturn(Optional.of(cs("A001", (short) 1, (short) 1)));
        when(lookup.findById("B002")).thenReturn(Optional.of(cs("B002", (short) 1, (short) 1)));

        List<AngleCapacityViolation> v = validator.validate(Map.of(
            "A001", List.of(new RotationSlot(D, "LP-01", 5, 1)),
            "B002", List.of(new RotationSlot(D, "LP-01", 5, 2))
        ));

        assertThat(v).isEmpty();
    }

    @Test
    @DisplayName("IC 머신 — ic_angle_qty 사용")
    void ic_machine_uses_ic_angle_qty() {
        when(lookup.findById("A001")).thenReturn(Optional.of(cs("A001", (short) 1, (short) 1)));

        List<AngleCapacityViolation> v = validator.validate(Map.of(
            "A001", List.of(
                new RotationSlot(D, "IC-01", 3, 1),
                new RotationSlot(D, "IC-01", 3, 2)
            )));

        assertThat(v).hasSize(1);
        assertThat(v.get(0).machineType()).isEqualTo("IC");
    }

    @Test
    @DisplayName("isWithinCapacity — quick check")
    void is_within_capacity_quick() {
        when(lookup.findById("A001")).thenReturn(Optional.of(cs("A001", (short) 4, (short) 3)));

        assertThat(validator.isWithinCapacity("A001", "LP", 4)).isTrue();
        assertThat(validator.isWithinCapacity("A001", "LP", 5)).isFalse();
        assertThat(validator.isWithinCapacity("A001", "IC", 3)).isTrue();
        assertThat(validator.isWithinCapacity("A001", "IC", 4)).isFalse();
    }

    @Test
    @DisplayName("미존재 hose_id → angle 0, 어떤 점유도 위반")
    void unknown_hose_id_zero_capacity() {
        when(lookup.findById("UNKNOWN")).thenReturn(Optional.empty());

        List<AngleCapacityViolation> v = validator.validate(Map.of(
            "UNKNOWN", List.of(new RotationSlot(D, "LP-01", 1, 1))
        ));

        assertThat(v).hasSize(1);
        assertThat(v.get(0).allowedAngles()).isZero();
    }

    @Test
    @DisplayName("null lp_angle_qty → 0 으로 처리")
    void null_lp_angle_qty_zero() {
        when(lookup.findById("A001")).thenReturn(Optional.of(cs("A001", null, (short) 1)));

        List<AngleCapacityViolation> v = validator.validate(Map.of(
            "A001", List.of(new RotationSlot(D, "LP-01", 1, 1))
        ));

        assertThat(v).hasSize(1);
        assertThat(v.get(0).allowedAngles()).isZero();
    }

    @Test
    @DisplayName("빈 / null 입력 → 빈 결과 (defensive)")
    void empty_input_no_violations() {
        assertThat(validator.validate(Map.of())).isEmpty();
        assertThat(validator.validate(null)).isEmpty();
    }
}
