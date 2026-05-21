package com.scheduling.master.api;

import java.time.LocalDate;
import java.util.List;

/**
 * 영업일 캘린더 facade — TK-06-1-1 (REQ-FUNC-VC-008 / BR-X07 / BR-V09 / BR-E02).
 *
 * <p>Modulith cross-module 경계 — {@code master.calendar.WorkingCalendarService} 가 구현,
 * vc / ex 모듈은 본 인터페이스만 사용 (allowedDependencies=master::api).
 *
 * <p>월~금 기본 영업일 + {@code master.holiday} 등록 일자 제외.
 *
 * @see com.scheduling.master.calendar.WorkingCalendarService
 */
public interface WorkingCalendar {

    /** 본 일자가 영업일인지. (월~금 + 휴일 미등록) */
    boolean isWorkingDay(LocalDate date);

    /** 본 일자가 등록된 휴일인지. */
    boolean isHoliday(LocalDate date);

    /** {@code date} 로부터 {@code n} 영업일 후 날짜. */
    LocalDate addWorkingDays(LocalDate date, int n);

    /** {@code date} 로부터 {@code n} 영업일 전 날짜 (D-N 역산). */
    LocalDate subtractWorkingDays(LocalDate date, int n);

    /** {@code [from, to]} 사이 영업일 수 (양 끝 포함). */
    long workingDaysBetween(LocalDate from, LocalDate to);

    /** {@code [from, to]} 안의 영업일 리스트. */
    List<LocalDate> workingDaysInRange(LocalDate from, LocalDate to);

    /** 휴일 마스터 갱신 후 캐시 invalidate — HolidayController 가 호출. */
    void reload();
}
