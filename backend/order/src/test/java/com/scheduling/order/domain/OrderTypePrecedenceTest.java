package com.scheduling.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderType BR-O01 우선순위 정의 회귀 — TK-02-2-1.
 *
 * <p>enum 정의 순서 변경 시 본 테스트가 즉시 실패 — 정책 가드.
 */
class OrderTypePrecedenceTest {

    @Test
    @DisplayName("BR-O01 ordinal 순서 — FORECAST(0) < KD(1) < WEEKLY(2) < CONFIRMED(3)")
    void ordinal_order_matches_business_rule() {
        assertThat(OrderType.FORECAST.precedenceRank()).isEqualTo(0);
        assertThat(OrderType.KD.precedenceRank()).isEqualTo(1);
        assertThat(OrderType.WEEKLY.precedenceRank()).isEqualTo(2);
        assertThat(OrderType.CONFIRMED.precedenceRank()).isEqualTo(3);
    }

    @ParameterizedTest(name = "{0} > {1} = {2}")
    @CsvSource({
        "CONFIRMED,FORECAST,true",
        "CONFIRMED,KD,true",
        "CONFIRMED,WEEKLY,true",
        "WEEKLY,KD,true",
        "WEEKLY,FORECAST,true",
        "KD,FORECAST,true",
        // 역방향 false
        "FORECAST,CONFIRMED,false",
        "KD,WEEKLY,false",
        "FORECAST,KD,false",
        // 동률 false
        "CONFIRMED,CONFIRMED,false",
        "FORECAST,FORECAST,false"
    })
    @DisplayName("isStrongerThan 진리표")
    void truth_table_is_stronger_than(OrderType a, OrderType b, boolean expected) {
        assertThat(a.isStrongerThan(b)).isEqualTo(expected);
    }

    @Test
    @DisplayName("isAtLeastAsStrongAs — 동률 시 true")
    void at_least_as_strong_as_handles_tie() {
        assertThat(OrderType.WEEKLY.isAtLeastAsStrongAs(OrderType.WEEKLY)).isTrue();
        assertThat(OrderType.CONFIRMED.isAtLeastAsStrongAs(OrderType.FORECAST)).isTrue();
        assertThat(OrderType.FORECAST.isAtLeastAsStrongAs(OrderType.CONFIRMED)).isFalse();
    }
}
