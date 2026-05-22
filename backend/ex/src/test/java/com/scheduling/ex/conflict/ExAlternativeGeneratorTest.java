package com.scheduling.ex.conflict;

import com.scheduling.ex.gate.ExGateViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExAlternativeGenerator 단위 — TK-EX12-1-1 (REQ-FUNC-EX-012).
 */
class ExAlternativeGeneratorTest {

    private final ExAlternativeGenerator generator = new ExAlternativeGenerator();

    private ExClassifiedConflict classified(ExConflictCategory category, String hose) {
        ExGateViolation v = ExGateViolation.yieldShort(100, 50);
        return new ExClassifiedConflict(UUID.randomUUID(), hose, category, v);
    }

    @Test
    @DisplayName("5 카테고리 — 모두 ≥ 3 distinct 대안")
    void all_categories_have_at_least_three_alternatives() {
        for (ExConflictCategory c : ExConflictCategory.values()) {
            List<ExAlternative> alts = generator.generate(classified(c, "X"));
            long distinct = alts.stream().map(ExAlternative::type).distinct().count();
            assertThat(distinct).as("%s ≥ 3 distinct", c)
                .isGreaterThanOrEqualTo(ExAlternativeGenerator.MIN_ALTERNATIVES);
        }
    }

    @Test
    @DisplayName("CUMULATIVE_YIELD_SHORT → EARLIER_START 포함 (조기 시작)")
    void yield_short_includes_earlier_start() {
        var alts = generator.generate(classified(ExConflictCategory.CUMULATIVE_YIELD_SHORT, "X"));
        assertThat(alts).extracting(ExAlternative::type)
            .contains(ExAlternativeType.EARLIER_START);
    }

    @Test
    @DisplayName("SHIFT_CAPACITY_EXCEEDED → EARLIER_START 미포함 (시간 부족 → 시작 변경 무의미)")
    void shift_capacity_excludes_earlier_start() {
        var alts = generator.generate(classified(ExConflictCategory.SHIFT_CAPACITY_EXCEEDED, "X"));
        assertThat(alts).extracting(ExAlternative::type)
            .doesNotContain(ExAlternativeType.EARLIER_START);
    }

    @Test
    @DisplayName("DEADLINE_D1_VIOLATION → VC_DATE_NEGOTIATE 포함")
    void deadline_includes_vc_negotiate() {
        var alts = generator.generate(classified(ExConflictCategory.DEADLINE_D1_VIOLATION, "X"));
        assertThat(alts).extracting(ExAlternative::type)
            .contains(ExAlternativeType.VC_DATE_NEGOTIATE);
    }

    @Test
    @DisplayName("UNKNOWN → defaultPolicy fallback ≥ 3")
    void unknown_falls_back_to_default() {
        var alts = generator.generate(classified(ExConflictCategory.UNKNOWN, "X"));
        assertThat(alts.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("ExAlternative.suggestedAction — hoseId 차별화")
    void suggested_action_includes_hose_id() {
        var alts = generator.generate(classified(ExConflictCategory.CUMULATIVE_YIELD_SHORT, "29673-2R060"));
        assertThat(alts).extracting(ExAlternative::suggestedAction)
            .allMatch(action -> action.contains("29673-2R060"));
    }

    @Test
    @DisplayName("distinct 보장 — Set 기반 중복 제거")
    void distinct_alternatives() {
        for (ExConflictCategory c : ExConflictCategory.values()) {
            var alts = generator.generate(classified(c, "X"));
            long count = alts.size();
            long distinct = alts.stream().map(ExAlternative::type).distinct().count();
            assertThat(distinct).as("%s distinct == size", c).isEqualTo(count);
        }
    }
}
