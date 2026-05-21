package com.scheduling.master.calendar;

import com.scheduling.master.api.WorkingCalendar;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 영업일 캘린더 — TK-06-1-1 (EP-06 ST-06-1, REQ-FUNC-VC-008 / BR-X07).
 *
 * <p>월~금 기본 영업일 + {@code master.holiday} 등록 휴일 제외. PostConstruct 시 1회 로드,
 * HolidayController 가 변경 후 {@link #reload()} 호출.
 *
 * <p>{@code @Profile("with-infra")} — HolidayRepository (JPA) 의존. 단위 테스트는 ObjectProvider
 * fallback (휴일 0건) 으로 with-infra 없이도 isWorkingDay 만 검증 가능 — 그러나 본 서비스는
 * with-infra 만 활성. unit test 는 별도 {@code WorkingCalendarServiceTest} 가 in-memory holiday set
 * 으로 검증.
 */
@Service
@Profile("with-infra")
public class WorkingCalendarService implements WorkingCalendar {

    private static final Logger log = LoggerFactory.getLogger(WorkingCalendarService.class);

    private final ObjectProvider<HolidayRepository> repoProvider;
    private final AtomicReference<Set<LocalDate>> holidaysCache = new AtomicReference<>(Set.of());

    public WorkingCalendarService(ObjectProvider<HolidayRepository> repoProvider) {
        this.repoProvider = repoProvider;
    }

    @PostConstruct
    public void initCache() {
        reload();
    }

    @Override
    public synchronized void reload() {
        HolidayRepository repo = repoProvider.getIfAvailable();
        if (repo == null) {
            log.warn("[WorkingCalendar] HolidayRepository 부재 — 휴일 0건 (월~금만 영업일)");
            holidaysCache.set(Set.of());
            return;
        }
        Set<LocalDate> snapshot = repo.findAllHolidayDates().stream()
            .collect(Collectors.toUnmodifiableSet());
        holidaysCache.set(snapshot);
        log.info("[WorkingCalendar] reload — 휴일 {}건 캐시 갱신", snapshot.size());
    }

    @Override
    public boolean isWorkingDay(LocalDate date) {
        if (date == null) return false;
        DayOfWeek d = date.getDayOfWeek();
        if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) return false;
        return !holidaysCache.get().contains(date);
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        if (date == null) return false;
        return holidaysCache.get().contains(date);
    }

    @Override
    public LocalDate addWorkingDays(LocalDate date, int n) {
        if (date == null) throw new IllegalArgumentException("date 필수");
        if (n < 0) return subtractWorkingDays(date, -n);
        LocalDate result = date;
        int added = 0;
        while (added < n) {
            result = result.plusDays(1);
            if (isWorkingDay(result)) added++;
        }
        return result;
    }

    @Override
    public LocalDate subtractWorkingDays(LocalDate date, int n) {
        if (date == null) throw new IllegalArgumentException("date 필수");
        if (n < 0) return addWorkingDays(date, -n);
        LocalDate result = date;
        int subtracted = 0;
        while (subtracted < n) {
            result = result.minusDays(1);
            if (isWorkingDay(result)) subtracted++;
        }
        return result;
    }

    @Override
    public long workingDaysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("from/to 필수");
        if (from.isAfter(to)) return -workingDaysBetween(to, from);
        long count = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (isWorkingDay(d)) count++;
        }
        return count;
    }

    @Override
    public List<LocalDate> workingDaysInRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("from/to 필수");
        if (from.isAfter(to)) return Collections.emptyList();
        List<LocalDate> list = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (isWorkingDay(d)) list.add(d);
        }
        return list;
    }
}
