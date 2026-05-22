package com.scheduling.vc.conflict;

import com.scheduling.vc.allocator.AllocationConflict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConflictCategorizer 단위 — TK-VC15-1-1 (REQ-FUNC-VC-015).
 */
class ConflictCategorizerTest {

    private final ConflictCategorizer categorizer = new ConflictCategorizer();

    @Test
    @DisplayName("UNSCHEDULABLE Allocator Category → UNSCHEDULABLE")
    void classifies_unschedulable() {
        AllocationConflict c = AllocationConflict.unschedulable("X", 10);
        ClassifiedConflict cc = categorizer.classify(c);
        assertThat(cc.category()).isEqualTo(ConflictCategory.UNSCHEDULABLE);
    }

    @Test
    @DisplayName("INSUFFICIENT_CAPACITY → DAILY_CAPA")
    void classifies_insufficient_capacity() {
        AllocationConflict c = AllocationConflict.insufficientCapacity("X", 10, 5, 5);
        ClassifiedConflict cc = categorizer.classify(c);
        assertThat(cc.category()).isEqualTo(ConflictCategory.DAILY_CAPA);
    }

    @Test
    @DisplayName("DEADLINE_EXCEEDED → DEADLINE_D2")
    void classifies_deadline() {
        AllocationConflict c = AllocationConflict.deadlineExceeded("X", 10, 3, LocalDate.of(2026, 3, 5));
        ClassifiedConflict cc = categorizer.classify(c);
        assertThat(cc.category()).isEqualTo(ConflictCategory.DEADLINE_D2);
    }

    @Test
    @DisplayName("LEFT_RIGHT_VIOLATION → LEFT_RIGHT")
    void classifies_left_right() {
        AllocationConflict c = AllocationConflict.leftRightViolation("X", 10, 5);
        ClassifiedConflict cc = categorizer.classify(c);
        assertThat(cc.category()).isEqualTo(ConflictCategory.LEFT_RIGHT);
    }

    @Test
    @DisplayName("MACHINE_PIN_VIOLATION → MACHINE_PIN")
    void classifies_machine_pin() {
        AllocationConflict c = AllocationConflict.machinePinViolation("X", 10, 0, "28422-08HA0 LP-01 pin");
        ClassifiedConflict cc = categorizer.classify(c);
        assertThat(cc.category()).isEqualTo(ConflictCategory.MACHINE_PIN);
    }

    @Test
    @DisplayName("HOSE_CAP_VIOLATION → HOSE_CAP")
    void classifies_hose_cap() {
        AllocationConflict c = AllocationConflict.hoseCapViolation("X", 10, 5, 2);
        ClassifiedConflict cc = categorizer.classify(c);
        assertThat(cc.category()).isEqualTo(ConflictCategory.HOSE_CAP);
    }

    @Test
    @DisplayName("ANGLE_VIOLATION → ANGLE_CAPA")
    void classifies_angle() {
        AllocationConflict c = new AllocationConflict("X",
            AllocationConflict.Category.ANGLE_VIOLATION,
            "BR-V06 앵글 capa 초과", 10, 4);
        assertThat(categorizer.classify(c).category()).isEqualTo(ConflictCategory.ANGLE_CAPA);
    }

    @Test
    @DisplayName("countByCategory — 100건 분류 합계 = 100")
    void count_by_category_sums_to_total() {
        List<AllocationConflict> conflicts = List.of(
            AllocationConflict.unschedulable("A", 1),
            AllocationConflict.unschedulable("B", 1),
            AllocationConflict.insufficientCapacity("C", 5, 2, 2),
            AllocationConflict.leftRightViolation("D", 5, 1),
            AllocationConflict.deadlineExceeded("E", 5, 0, LocalDate.now())
        );

        var summary = categorizer.countByCategory(conflicts);
        long total = summary.values().stream().mapToLong(Long::longValue).sum();
        assertThat(total).isEqualTo(5);
        assertThat(summary).containsKey(ConflictCategory.UNSCHEDULABLE);
        assertThat(summary.get(ConflictCategory.UNSCHEDULABLE)).isEqualTo(2L);
    }

    @Test
    @DisplayName("classifyAll — order 보존")
    void classify_all_preserves_order() {
        List<AllocationConflict> conflicts = List.of(
            AllocationConflict.unschedulable("Z", 1),
            AllocationConflict.unschedulable("A", 1),
            AllocationConflict.unschedulable("M", 1)
        );

        List<ClassifiedConflict> classified = categorizer.classifyAll(conflicts);
        assertThat(classified.stream().map(ClassifiedConflict::hoseId).toList())
            .containsExactly("Z", "A", "M");
    }
}
