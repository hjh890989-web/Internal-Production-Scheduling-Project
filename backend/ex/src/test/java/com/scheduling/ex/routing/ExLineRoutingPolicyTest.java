package com.scheduling.ex.routing;

import com.scheduling.master.api.LineRoutingLookup;
import com.scheduling.master.api.LineTypeSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ExLineRoutingPolicy 단위 테스트 — TK-14-1-2 (BR-E08).
 *
 * <p>mock {@link LineRoutingLookup} 으로 정책 분기 검증:
 * <ul>
 *   <li>포드 전용 hose → FORD 라인만 (신규 시도 0)</li>
 *   <li>일반 hose → NEW priority ASC 먼저, FORD nachträglich</li>
 *   <li>비활성 라인은 lookup 단계에서 제외 (findAllActive)</li>
 * </ul>
 */
class ExLineRoutingPolicyTest {

    private final LineRoutingLookup lookup = mock(LineRoutingLookup.class);
    private final ExLineRoutingPolicy policy = new ExLineRoutingPolicy(lookup);

    private static LineTypeSummary newLine(String id, short priority) {
        return new LineTypeSummary(id, "NEW", priority, true, id + " 신규");
    }

    private static LineTypeSummary fordLine(String id, short priority) {
        return new LineTypeSummary(id, "FORD", priority, true, id + " 포드");
    }

    @Test
    @DisplayName("일반 hose — NEW priority ASC 먼저, FORD 나중 (BR-E08)")
    void normal_hose_new_first_ford_last() {
        when(lookup.findAllActive()).thenReturn(List.of(
            fordLine("L-FORD", (short) 90),
            newLine("L2", (short) 2),
            newLine("L1", (short) 1),
            newLine("L3", (short) 3)
        ));
        when(lookup.isFordOnly("29673-2R060")).thenReturn(false);

        List<String> ordered = policy.prioritize("29673-2R060");

        assertThat(ordered).containsExactly("L1", "L2", "L3", "L-FORD");
    }

    @Test
    @DisplayName("포드 전용 hose — FORD 라인만, 신규 시도 0")
    void ford_only_hose_returns_only_ford_lines() {
        when(lookup.findAllActive()).thenReturn(List.of(
            fordLine("L-FORD", (short) 90),
            newLine("L1", (short) 1),
            newLine("L2", (short) 2)
        ));
        when(lookup.isFordOnly("25490-03HA0")).thenReturn(true);

        List<String> ordered = policy.prioritize("25490-03HA0");

        assertThat(ordered).containsExactly("L-FORD");
        assertThat(ordered).noneMatch(id -> id.startsWith("L1") || id.startsWith("L2"));
    }

    @Test
    @DisplayName("활성 라인 없음 → 빈 리스트")
    void no_active_lines_returns_empty() {
        when(lookup.findAllActive()).thenReturn(List.of());
        when(lookup.isFordOnly("X")).thenReturn(false);

        assertThat(policy.prioritize("X")).isEmpty();
    }

    @Test
    @DisplayName("포드 전용 hose + FORD 라인 부재 → 빈 리스트 (신규 시도 0 보장)")
    void ford_only_but_no_ford_line_returns_empty() {
        when(lookup.findAllActive()).thenReturn(List.of(
            newLine("L1", (short) 1)
        ));
        when(lookup.isFordOnly("25490-03HA0")).thenReturn(true);

        assertThat(policy.prioritize("25490-03HA0")).isEmpty();
    }

    @Test
    @DisplayName("isFordOnly facade 위임")
    void isFordOnly_delegates_to_lookup() {
        when(lookup.isFordOnly("X")).thenReturn(true);
        assertThat(policy.isFordOnly("X")).isTrue();
        when(lookup.isFordOnly("Y")).thenReturn(false);
        assertThat(policy.isFordOnly("Y")).isFalse();
    }
}
