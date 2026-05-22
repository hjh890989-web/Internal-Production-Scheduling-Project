package com.scheduling.vc.conflict;

import com.scheduling.vc.allocator.AllocationConflict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AlternativeGenerator 단위 — TK-VC15-1-2 (REQ-FUNC-VC-015).
 *
 * <p>SRS 명시: 모든 conflict 에 ≥ 3 distinct 대안.
 */
class AlternativeGeneratorTest {

    private final AlternativeGenerator generator = new AlternativeGenerator();

    private ClassifiedConflict classified(ConflictCategory category, String hose) {
        AllocationConflict c = new AllocationConflict(hose,
            AllocationConflict.Category.UNSCHEDULABLE, "dummy", 0, 0);
        return new ClassifiedConflict(c, category);
    }

    @Test
    @DisplayName("8 카테고리 — 모두 ≥ 3 distinct 대안")
    void all_categories_have_at_least_three_alternatives() {
        for (ConflictCategory c : ConflictCategory.values()) {
            List<Alternative> alts = generator.generate(classified(c, "X"));
            long distinct = alts.stream().map(Alternative::type).distinct().count();
            assertThat(distinct)
                .as("%s ≥ 3 distinct", c)
                .isGreaterThanOrEqualTo(AlternativeGenerator.MIN_ALTERNATIVES);
        }
    }

    @Test
    @DisplayName("SPEC_LT7 → IC_ROUTING 미포함 (룰 적용 불가)")
    void spec_lt7_excludes_ic_routing() {
        var alts = generator.generate(classified(ConflictCategory.SPEC_LT7, "28442-6T010"));
        assertThat(alts).extracting(Alternative::type)
            .doesNotContain(AlternativeType.IC_ROUTING);
    }

    @Test
    @DisplayName("MACHINE_PIN → IC_ROUTING 미포함")
    void machine_pin_excludes_ic_routing() {
        var alts = generator.generate(classified(ConflictCategory.MACHINE_PIN, "28422-08HA0"));
        assertThat(alts).extracting(Alternative::type)
            .doesNotContain(AlternativeType.IC_ROUTING);
    }

    @Test
    @DisplayName("LEFT_RIGHT → IC_ROUTING 미포함")
    void left_right_excludes_ic_routing() {
        var alts = generator.generate(classified(ConflictCategory.LEFT_RIGHT, "28421-2M800"));
        assertThat(alts).extracting(Alternative::type)
            .doesNotContain(AlternativeType.IC_ROUTING);
    }

    @Test
    @DisplayName("DEADLINE_D2 → DEADLINE_NEGOTIATE 포함")
    void deadline_includes_negotiate() {
        var alts = generator.generate(classified(ConflictCategory.DEADLINE_D2, "X"));
        assertThat(alts).extracting(Alternative::type)
            .contains(AlternativeType.DEADLINE_NEGOTIATE);
    }

    @Test
    @DisplayName("UNKNOWN → defaultPolicy fallback ≥ 3")
    void unknown_falls_back_to_default() {
        var alts = generator.generate(classified(ConflictCategory.UNKNOWN, "X"));
        long distinct = alts.stream().map(Alternative::type).distinct().count();
        assertThat(distinct).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Alternative.suggestedAction — hoseId 차별화")
    void suggested_action_includes_hose_id() {
        var alts = generator.generate(classified(ConflictCategory.DEADLINE_D2, "28442-6T010"));
        assertThat(alts).extracting(Alternative::suggestedAction)
            .allMatch(action -> action.contains("28442-6T010") || action.contains("주말"));
    }

    @Test
    @DisplayName("distinct 보장 — Set 기반 중복 제거")
    void distinct_alternatives() {
        for (ConflictCategory c : ConflictCategory.values()) {
            var alts = generator.generate(classified(c, "X"));
            long count = alts.size();
            long distinct = alts.stream().map(Alternative::type).distinct().count();
            assertThat(distinct).as("%s distinct == size", c).isEqualTo(count);
        }
    }
}
