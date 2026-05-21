package com.scheduling.vc.calendar;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 영업일 캘린더 stub — TK-05-1-2 (EP-06 후속 정식 구현).
 *
 * <p>Sprint 2 baseline: 월~금 = 영업일. 토·일 + 공휴일 처리는 EP-06 ST-06-1 에서
 * KoreanHolidayService 통합 (한국 공휴일 calendar 라이브러리 또는 마스터 테이블).
 *
 * <p>BR-V09 / BR-E02 영업일 역산 정합.
 */
@Service
public class WorkingCalendarService {

    /** 월요일~금요일 → true. 토·일 → false. */
    public boolean isWorkingDay(LocalDate date) {
        if (date == null) return false;
        DayOfWeek d = date.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY;
    }
}
