package com.scheduling.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PrecedenceResolver 회귀 — TK-02-2-1 + TK-02-2-3 (REQ-FUNC-OC-006, BR-O01).
 *
 * <p>TC-OC-006 4종 표준 케이스 + 동률 + 3개 이상 후보 + 기존 마스터 케이스.
 */
class PrecedenceResolverTest {

    private static final LocalDate D = LocalDate.of(2026, 2, 15);
    private static final String HOSE = "29673-2F900";
    private final PrecedenceResolver resolver = new PrecedenceResolver();

    private OrderDraft draft(OrderType type) {
        return new OrderDraft(UUID.randomUUID(), HOSE, D, 100, type, "내수");
    }

    private Order existingMaster(OrderType type) {
        return new Order(UUID.randomUUID(), HOSE, D, 100, type, "내수",
            1, "ACTIVE", Instant.parse("2026-02-01T00:00:00Z"));
    }

    private DuplicateGroup group(List<OrderDraft> candidates, Order existing) {
        return new DuplicateGroup(new OrderKey(HOSE, D), candidates, existing);
    }

    // ---------- TC-OC-006 4 표준 케이스 ----------

    @Test
    @DisplayName("Case 1 — Forecast vs Weekly → Weekly 승 (NEW_WINS)")
    void case_1_forecast_vs_weekly() {
        OrderDraft f = draft(OrderType.FORECAST);
        OrderDraft w = draft(OrderType.WEEKLY);

        Resolution r = resolver.resolve(group(List.of(f, w), null));

        assertThat(r.winner()).isSameAs(w);
        assertThat(r.losers()).containsExactly(f);
        assertThat(r.decision()).isEqualTo(Resolution.Decision.NEW_WINS);
        assertThat(r.hasExisting()).isFalse();
    }

    @Test
    @DisplayName("Case 2 — KD vs Confirmed → Confirmed 승 (NEW_WINS)")
    void case_2_kd_vs_confirmed() {
        OrderDraft kd = draft(OrderType.KD);
        OrderDraft c = draft(OrderType.CONFIRMED);

        Resolution r = resolver.resolve(group(List.of(kd, c), null));

        assertThat(r.winner()).isSameAs(c);
        assertThat(r.losers()).containsExactly(kd);
        assertThat(r.decision()).isEqualTo(Resolution.Decision.NEW_WINS);
    }

    @Test
    @DisplayName("Case 3 — 기존 마스터 Confirmed vs 신규 Forecast → KEPT_EXISTING")
    void case_3_existing_confirmed_beats_new_forecast() {
        OrderDraft f = draft(OrderType.FORECAST);
        Order ex = existingMaster(OrderType.CONFIRMED);

        Resolution r = resolver.resolve(group(List.of(f), ex));

        assertThat(r.decision()).isEqualTo(Resolution.Decision.KEPT_EXISTING);
        assertThat(r.winner().orderType()).isEqualTo(OrderType.CONFIRMED);
        assertThat(r.losers()).containsExactly(f);
        assertThat(r.hasExisting()).isTrue();
    }

    @Test
    @DisplayName("Case 4 — 기존 마스터 Forecast vs 신규 Confirmed → REPLACED_EXISTING")
    void case_4_new_confirmed_replaces_existing_forecast() {
        OrderDraft c = draft(OrderType.CONFIRMED);
        Order ex = existingMaster(OrderType.FORECAST);

        Resolution r = resolver.resolve(group(List.of(c), ex));

        assertThat(r.decision()).isEqualTo(Resolution.Decision.REPLACED_EXISTING);
        assertThat(r.winner()).isSameAs(c);
        assertThat(r.losers()).isEmpty();
        assertThat(r.hasExisting()).isTrue();
    }

    // ---------- 동률 + 다중 후보 ----------

    @Test
    @DisplayName("동률 — 같은 OrderType 2개 → first-come (입력 첫 번째 winner)")
    void tie_breaks_first_come() {
        OrderDraft first = draft(OrderType.CONFIRMED);
        OrderDraft second = draft(OrderType.CONFIRMED);

        Resolution r = resolver.resolve(group(List.of(first, second), null));

        assertThat(r.winner()).isSameAs(first);
        assertThat(r.losers()).containsExactly(second);
    }

    @Test
    @DisplayName("4종 ordinal 모두 — Confirmed 승, 나머지 3개 losers")
    void all_four_types_confirmed_wins() {
        OrderDraft f = draft(OrderType.FORECAST);
        OrderDraft k = draft(OrderType.KD);
        OrderDraft w = draft(OrderType.WEEKLY);
        OrderDraft c = draft(OrderType.CONFIRMED);

        Resolution r = resolver.resolve(group(List.of(f, k, w, c), null));

        assertThat(r.winner()).isSameAs(c);
        assertThat(r.losers()).containsExactly(f, k, w);   // 입력 순서 보존
        assertThat(r.decision()).isEqualTo(Resolution.Decision.NEW_WINS);
    }

    // ---------- 기존 마스터 + 다중 후보 ----------

    @Test
    @DisplayName("기존 Weekly + 신규 Forecast·KD → KEPT_EXISTING (모두 losers)")
    void existing_weekly_beats_weaker_new_candidates() {
        Order ex = existingMaster(OrderType.WEEKLY);
        OrderDraft f = draft(OrderType.FORECAST);
        OrderDraft k = draft(OrderType.KD);

        Resolution r = resolver.resolve(group(List.of(f, k), ex));

        assertThat(r.decision()).isEqualTo(Resolution.Decision.KEPT_EXISTING);
        assertThat(r.winner().orderType()).isEqualTo(OrderType.WEEKLY);
        assertThat(r.losers()).containsExactly(f, k);
    }

    @Test
    @DisplayName("기존 Weekly + 신규 Confirmed + Forecast → REPLACED_EXISTING (Confirmed 승)")
    void existing_weekly_replaced_by_new_confirmed() {
        Order ex = existingMaster(OrderType.WEEKLY);
        OrderDraft c = draft(OrderType.CONFIRMED);
        OrderDraft f = draft(OrderType.FORECAST);

        Resolution r = resolver.resolve(group(List.of(c, f), ex));

        assertThat(r.decision()).isEqualTo(Resolution.Decision.REPLACED_EXISTING);
        assertThat(r.winner()).isSameAs(c);
        assertThat(r.losers()).containsExactly(f);
    }

    @Test
    @DisplayName("기존 마스터 == 신규 (동률) → KEPT_EXISTING (기존 유지)")
    void existing_equal_to_new_keeps_existing() {
        Order ex = existingMaster(OrderType.CONFIRMED);
        OrderDraft c = draft(OrderType.CONFIRMED);

        Resolution r = resolver.resolve(group(List.of(c), ex));

        // 동률 → 기존 유지 (isAtLeastAsStrongAs)
        assertThat(r.decision()).isEqualTo(Resolution.Decision.KEPT_EXISTING);
        assertThat(r.winner().orderType()).isEqualTo(OrderType.CONFIRMED);
    }

    @Test
    @DisplayName("null DuplicateGroup → NullPointerException")
    void null_group_rejected() {
        assertThatThrownBy(() -> resolver.resolve(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("candidates 1개 단독 — winner 자기 자신, losers 빈")
    void single_candidate_no_existing() {
        OrderDraft c = draft(OrderType.WEEKLY);
        Resolution r = resolver.resolve(group(List.of(c), null));
        assertThat(r.winner()).isSameAs(c);
        assertThat(r.losers()).isEmpty();
        assertThat(r.decision()).isEqualTo(Resolution.Decision.NEW_WINS);
    }
}
