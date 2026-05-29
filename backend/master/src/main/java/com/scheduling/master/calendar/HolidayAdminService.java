package com.scheduling.master.calendar;

import com.scheduling.audit.aop.Auditable;
import com.scheduling.master.api.WorkingCalendar;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Sprint 21 ST-CRUD-5 Holiday 마스터 관리 Service (IT_OPS 전용).
 *
 * <p>{@code @Auditable} BR-X02 — 모든 mutation audit_log 기록.
 * 추가/삭제 후 {@link WorkingCalendar#reload()} 로 캐시 invalidate.
 * BR-X04 — LocalDate (timezone-free, KST 정합).
 *
 * @see BR-X02
 * @see BR-X04
 */
@Service
public class HolidayAdminService {

    private static final Logger log = LoggerFactory.getLogger(HolidayAdminService.class);

    private final HolidayRepository repository;
    private final ObjectProvider<WorkingCalendar> calendarProvider;

    public HolidayAdminService(HolidayRepository repository,
                               ObjectProvider<WorkingCalendar> calendarProvider) {
        this.repository = repository;
        this.calendarProvider = calendarProvider;
    }

    /**
     * 휴일 신규 추가.
     *
     * @throws EntityExistsException 동일 날짜 중복 시 (→ 409)
     */
    @Auditable("ST-CRUD-5 HOLIDAY 추가 (IT_OPS)")
    @Transactional
    public Holiday add(LocalDate date, String name, HolidayType type,
                       String description, String actor) {
        if (repository.existsById(date)) {
            throw new EntityExistsException("holiday_date 중복: " + date);
        }
        Holiday saved = repository.save(new Holiday(date, name, type, description, actor));
        invalidateCalendar();
        log.info("ST-CRUD-5 holiday add — date={} name={} actor={}", date, name, actor);
        return saved;
    }

    /**
     * 휴일 삭제.
     *
     * @throws EntityNotFoundException 대상 날짜 미존재 시 (→ 404)
     */
    @Auditable("ST-CRUD-5 HOLIDAY 삭제 (IT_OPS)")
    @Transactional
    public void remove(LocalDate date) {
        if (!repository.existsById(date)) {
            throw new EntityNotFoundException("holiday_date 미존재: " + date);
        }
        repository.deleteById(date);
        invalidateCalendar();
        log.info("ST-CRUD-5 holiday remove — date={}", date);
    }

    /** WorkingCalendar 캐시 invalidate — bean 부재 시(단위 테스트 환경) 스킵. */
    private void invalidateCalendar() {
        WorkingCalendar cal = calendarProvider.getIfAvailable();
        if (cal != null) {
            cal.reload();
        } else {
            log.debug("ST-CRUD-5 WorkingCalendar bean 부재 — cache invalidate 스킵");
        }
    }
}
