package com.scheduling.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KST 경계 일자·시간대 검증 — BR-X04 (TK-34-3-2).
 *
 * <p>검증 시나리오:
 * <ul>
 *   <li>23:59:59 KST → 다음날 00:00:00 KST 경계 정확도</li>
 *   <li>UTC 14:00 ↔ KST 23:00 (UTC+9) 변환 일관성</li>
 *   <li>한국 표준시 = UTC+9 고정 (DST 없음, 1988 이후 미적용)</li>
 *   <li>{@link Clock} 주입 — fixed clock 으로 testable 시간 검증</li>
 * </ul>
 */
@DisplayName("BR-X04 KST 시간 통일 경계 테스트")
class KstBoundaryTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("23:59:59 KST → 00:00:00 KST 경계 (영업일 boundary)")
    void midnight_boundary_kst() {
        ZonedDateTime endOfDay = ZonedDateTime.of(2026, 5, 19, 23, 59, 59, 0, KST);
        ZonedDateTime nextDay = endOfDay.plus(1, ChronoUnit.SECONDS);

        assertThat(nextDay.getYear()).isEqualTo(2026);
        assertThat(nextDay.getMonthValue()).isEqualTo(5);
        assertThat(nextDay.getDayOfMonth()).isEqualTo(20);
        assertThat(nextDay.getHour()).isEqualTo(0);
        assertThat(nextDay.getMinute()).isEqualTo(0);
        assertThat(nextDay.getSecond()).isEqualTo(0);
    }

    @Test
    @DisplayName("UTC 14:00 = KST 23:00 (UTC+9)")
    void utc_to_kst_offset_9h() {
        ZonedDateTime utc14 = ZonedDateTime.of(2026, 5, 19, 14, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime kst23 = utc14.withZoneSameInstant(KST);

        assertThat(kst23.getHour()).isEqualTo(23);
        assertThat(kst23.getDayOfMonth()).isEqualTo(19);   // 같은 날짜
    }

    @Test
    @DisplayName("UTC 16:00 = KST 다음날 01:00 (날짜 경계)")
    void utc_to_kst_date_boundary() {
        ZonedDateTime utc16 = ZonedDateTime.of(2026, 5, 19, 16, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime kst = utc16.withZoneSameInstant(KST);

        assertThat(kst.getHour()).isEqualTo(1);
        assertThat(kst.getDayOfMonth()).isEqualTo(20);     // 다음 날짜
    }

    @Test
    @DisplayName("한국 표준시 = UTC+9 고정 (DST 없음 — 1988 이후 미적용)")
    void kst_no_dst() {
        // 1년 12개월 — 모든 달의 1일 00:00 KST 의 offset 검증
        for (int month = 1; month <= 12; month++) {
            ZonedDateTime kst = ZonedDateTime.of(2026, month, 1, 0, 0, 0, 0, KST);
            assertThat(kst.getOffset())
                .as("Month=%d", month)
                .isEqualTo(ZoneOffset.ofHours(9));
        }
    }

    @Test
    @DisplayName("Clock 주입 — fixed clock 으로 테스트 가능한 시간")
    void clock_injection_pattern() {
        Instant fixedNow = Instant.parse("2026-05-19T08:42:33Z");   // UTC
        Clock fixedClock = Clock.fixed(fixedNow, KST);

        Instant now = Instant.now(fixedClock);
        ZonedDateTime kst = now.atZone(KST);

        // UTC 08:42 → KST 17:42
        assertThat(kst.getHour()).isEqualTo(17);
        assertThat(kst.getMinute()).isEqualTo(42);
        assertThat(kst.getDayOfMonth()).isEqualTo(19);
        assertThat(now).isEqualTo(fixedNow);
    }

    @Test
    @DisplayName("LocalDate.now(clock) — D-Day 계산용 (BR-X01)")
    void localdate_now_with_clock() {
        Instant fixedNow = Instant.parse("2026-05-19T14:59:59Z");   // UTC = KST 23:59:59
        Clock fixedClock = Clock.fixed(fixedNow, KST);

        LocalDate today = LocalDate.now(fixedClock);
        assertThat(today).isEqualTo(LocalDate.of(2026, 5, 19));

        // 1초 더하면 자정 넘김 → 5월 20일
        Clock oneSecLater = Clock.fixed(fixedNow.plusSeconds(1), KST);
        LocalDate nextDay = LocalDate.now(oneSecLater);
        assertThat(nextDay).isEqualTo(LocalDate.of(2026, 5, 20));
    }

    @Test
    @DisplayName("LocalDateTime — timezone 정보 없음, KST 명시 ZonedDateTime 사용 권장")
    void localdatetime_no_zone() {
        LocalDateTime ldt = LocalDateTime.of(2026, 5, 19, 12, 0);

        // LocalDateTime → ZonedDateTime + KST
        ZonedDateTime kst = ldt.atZone(KST);
        assertThat(kst.getZone()).isEqualTo(KST);
        assertThat(kst.getOffset()).isEqualTo(ZoneOffset.ofHours(9));

        // 같은 LocalDateTime 을 UTC 로 해석하면 다른 Instant
        ZonedDateTime utc = ldt.atZone(ZoneOffset.UTC);
        assertThat(kst.toInstant()).isNotEqualTo(utc.toInstant());
        assertThat(kst.toInstant().toEpochMilli())
            .isEqualTo(utc.toInstant().toEpochMilli() - 9 * 3600_000L);
    }
}
