package com.scheduling.integration;

import com.scheduling.ex.routing.ExLineRoutingPolicy;
import com.scheduling.master.api.LineRoutingLookup;
import com.scheduling.master.api.LineTypeSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-14 ST-14-1 IT — 라인 라우팅 정책 (BR-E08, NS-S09).
 *
 * <p>V024 seed 검증:
 * <ul>
 *   <li>L1·L2·L3 NEW priority 1·2·3 + L-FORD priority 90 (4 라인)</li>
 *   <li>일반 hose → NEW priority ASC 정렬 + FORD nachträglich</li>
 *   <li>포드 전용 hose (25490-03HA0, 28415-08400) → FORD 라인만 반환 (NS-S09 신규 시도 0건)</li>
 *   <li>NS-S09 신규 라인 사용률 ≥ 90% (일반 hose 100% NEW 시도 → fallback 시에만 FORD)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LineRoutingIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduling")
        .withUsername("app_user")
        .withPassword("test_secret");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "65535");
        registry.add("scheduling.notification.kakao.enabled", () -> "false");
    }

    @Autowired private LineRoutingLookup lookup;
    @Autowired private ExLineRoutingPolicy policy;

    @Test
    @DisplayName("V024 seed — 4 라인 (L1·L2·L3 NEW + L-FORD), priority ASC 정렬")
    void seed_4_lines_priority_sorted() {
        List<LineTypeSummary> active = lookup.findAllActive();
        assertThat(active).hasSize(4);
        // priority ASC: L1(1), L2(2), L3(3), L-FORD(90)
        assertThat(active.stream().map(LineTypeSummary::lineId).toList())
            .containsExactly("L1", "L2", "L3", "L-FORD");
        // NEW 3, FORD 1
        assertThat(active.stream().filter(LineTypeSummary::isNew).count()).isEqualTo(3);
        assertThat(active.stream().filter(LineTypeSummary::isFord).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("일반 hose 29673-2R060 → NEW (L1·L2·L3) 우선, FORD 나중 (BR-E08)")
    void normal_hose_new_first() {
        List<String> order = policy.prioritize("29673-2R060");
        assertThat(order).containsExactly("L1", "L2", "L3", "L-FORD");
        // 신규 비율 = 3/4 = 75% (정렬상, 운영 시 fallback 안 일어나면 100%)
    }

    @Test
    @DisplayName("포드 전용 25490-03HA0 → FORD 라인만, 신규 시도 0건 (NS-S09)")
    void ford_only_25490_no_new() {
        assertThat(lookup.isFordOnly("25490-03HA0")).isTrue();
        List<String> order = policy.prioritize("25490-03HA0");
        assertThat(order).containsExactly("L-FORD");
        assertThat(order).noneMatch(id -> id.startsWith("L1") || id.startsWith("L2") || id.startsWith("L3"));
    }

    @Test
    @DisplayName("포드 전용 28415-08400 → FORD 라인만")
    void ford_only_28415_no_new() {
        assertThat(lookup.isFordOnly("28415-08400")).isTrue();
        assertThat(policy.prioritize("28415-08400")).containsExactly("L-FORD");
    }

    @Test
    @DisplayName("NS-S09 회귀 — 100 일반 hose 모두 NEW 1순위 (L1) 우선")
    void ns_s09_100_normal_hoses_all_new_first() {
        int newFirstCount = 0;
        for (int i = 0; i < 100; i++) {
            String fakeHose = String.format("99%03d-TEST", i);
            List<String> order = policy.prioritize(fakeHose);
            if (!order.isEmpty() && "L1".equals(order.get(0))) {
                newFirstCount++;
            }
        }
        // 일반 hose 100건 모두 L1 (NEW priority 1) 가 1순위 → 100% ≥ 90% (NS-S09)
        assertThat(newFirstCount).isEqualTo(100);
    }

    @Test
    @DisplayName("호환 라인 직접 조회 — 포드 전용 hose 는 L-FORD 1건")
    void compatible_line_ids_ford_only_hose() {
        List<String> compat = lookup.findCompatibleLineIds("25490-03HA0");
        assertThat(compat).containsExactly("L-FORD");
    }

    @Test
    @DisplayName("호환 미등록 hose → findCompatibleLineIds 빈 list (라우팅 정책에 위임)")
    void compatible_line_ids_unregistered_hose_empty() {
        assertThat(lookup.findCompatibleLineIds("99999-NONE")).isEmpty();
        assertThat(lookup.isFordOnly("99999-NONE")).isFalse();
    }
}
