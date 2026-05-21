package com.scheduling.integration;

import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.deadline.BackwardDeadlineCalculator;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EP-06 ST-06-1 TK-06-1-3 — D-2 영업일 역산 edge case (TC-VC-008).
 *
 * <p>실 PG + V012 master.holiday seed (2026 KR 법정공휴일) + BackwardDeadlineCalculator
 * 와의 통합 동작 — 휴일 분포가 다양한 5 시나리오:
 * <ul>
 *   <li>금요일 납기 → 정상 (영업일만 카운트)</li>
 *   <li>설날 직후 월요일 납기 → 설날 3일 skip 후 직전 주 목요일</li>
 *   <li>주말 후 월요일 납기 → 토·일 skip 후 직전 주 목요일</li>
 *   <li>토요일 납기 → 영업일 아니어도 deterministic 역산</li>
 *   <li>추석 연휴 직후 화요일 납기 → 추석 3일 + 주말 skip</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("with-infra")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DeadlineEdgeCaseIT {

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

    @Autowired private BackwardDeadlineCalculator deadlineCalc;
    @Autowired private WorkingCalendar calendar;

    @Test
    @DisplayName("금요일 납기 일반 — 2026-03-06(금) → deadline 2026-03-04(수)")
    void delivery_friday_normal_case() {
        // 3/6 금 → 3/5 목 = 1차감, 3/5 → 3/4 수 = 2차감 — 휴일 없음
        LocalDate deadline = deadlineCalc.deadlineFor(LocalDate.of(2026, 3, 6));
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 3, 4));
    }

    @Test
    @DisplayName("설날 직후 월요일 납기 — 2026-02-23(월) → deadline 2026-02-19(목)")
    void delivery_right_after_lunar_new_year() {
        // 2/23 월 → 2/20(금)=1, 2/20 → 2/19(목)=2 (설날 2/16~18 미관여)
        LocalDate deadline = deadlineCalc.deadlineFor(LocalDate.of(2026, 2, 23));
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 2, 19));
    }

    @Test
    @DisplayName("주말 후 월요일 납기 — 2026-03-09(월) → deadline 2026-03-05(목)")
    void delivery_on_monday_after_weekend() {
        // 3/9 월 → 3/6(금)=1, 3/6 → 3/5(목)=2 (주말 skip 만)
        LocalDate deadline = deadlineCalc.deadlineFor(LocalDate.of(2026, 3, 9));
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    @DisplayName("토요일 납기 — 비영업일이라도 deterministic 역산")
    void delivery_on_non_working_day_treated_consistently() {
        // 2/28 토 → 2/27(금)=1, 2/27→2/26(목)=2 (휴일 없음)
        LocalDate deadline = deadlineCalc.deadlineFor(LocalDate.of(2026, 2, 28));
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 2, 26));
    }

    @Test
    @DisplayName("추석 연휴 직후 화요일 납기 — 2026-09-29(화) → deadline 2026-09-23(수)")
    void consecutive_holidays_chuseok() {
        // 9/29 화 → 9/28(월, working)=1, 9/27(일) skip, 9/26(토 + 추석연휴) skip,
        // 9/25(추석) skip, 9/24(추석연휴) skip, 9/23(수, working)=2
        LocalDate deadline = deadlineCalc.deadlineFor(LocalDate.of(2026, 9, 29));
        assertThat(deadline).isEqualTo(LocalDate.of(2026, 9, 23));
    }

    @Test
    @DisplayName("calendar.isWorkingDay — 설날 2/17 false, 영업일 2/19 true (seed 정합)")
    void calendar_reflects_seed() {
        assertThat(calendar.isWorkingDay(LocalDate.of(2026, 2, 17))).isFalse();
        assertThat(calendar.isWorkingDay(LocalDate.of(2026, 2, 19))).isTrue();
    }
}
