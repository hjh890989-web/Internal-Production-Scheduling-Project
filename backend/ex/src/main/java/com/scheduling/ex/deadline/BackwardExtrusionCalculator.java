package com.scheduling.ex.deadline;

import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.events.VcConfirmedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 압출 D-1 영업일 역산 — TK-07-1-2 (REQ-FUNC-EX-001 / BR-E01).
 *
 * <p>{@code ex_deadline = vc_production_date − 1 working day}. 같은 hose_id 에 여러
 * vc_date 있을 시 가장 이른 production_date 기준 (hard 제약).
 *
 * <p>EP-06 {@link WorkingCalendar} 재사용 — CON-10 단일 캘린더 마스터 원칙 준수.
 *
 * <p>{@code @Profile("with-infra")} — WorkingCalendarService (JPA) 의존.
 */
@Component
@Profile("with-infra")
public class BackwardExtrusionCalculator {

    /** BR-E01 — 압출 완료 기한 = 성형 투입 - 1 working day. */
    public static final int BACKWARD_DAYS = 1;

    private final WorkingCalendar calendar;

    public BackwardExtrusionCalculator(WorkingCalendar calendar) {
        this.calendar = calendar;
    }

    /**
     * VcConfirmedEvent → hose_id 별 deadline 맵.
     *
     * <p>같은 hose 가 여러 row 에 있으면 가장 이른 production_date 기준 (가장 빠른 일정 우선).
     */
    public ExDeadlineMap compute(VcConfirmedEvent event) {
        if (event == null || event.rows().isEmpty()) {
            return new ExDeadlineMap(Map.of());
        }
        Map<String, LocalDate> earliestByHose = new HashMap<>();
        for (VcConfirmedEvent.VcConfirmedRow row : event.rows()) {
            earliestByHose.merge(row.hoseId(), row.productionDate(),
                (a, b) -> a.isBefore(b) ? a : b);
        }
        Map<String, LocalDate> deadlines = new HashMap<>();
        for (Map.Entry<String, LocalDate> entry : earliestByHose.entrySet()) {
            deadlines.put(entry.getKey(),
                calendar.subtractWorkingDays(entry.getValue(), BACKWARD_DAYS));
        }
        return new ExDeadlineMap(deadlines);
    }

    /** 단일 vc_production_date 의 D-1 deadline. */
    public LocalDate deadlineFor(LocalDate vcProductionDate) {
        return calendar.subtractWorkingDays(vcProductionDate, BACKWARD_DAYS);
    }
}
