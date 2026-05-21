package com.scheduling.vc.deadline;

import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.required.OrderInput;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * D-2 영업일 역산 — TK-06-1-2 (REQ-FUNC-VC-008 / BR-X07).
 *
 * <p>{@code vc_completion_deadline = delivery_date − 2 working days}. 같은 hose_id 에 여러
 * 수주가 있으면 가장 이른 납기 기준 deadline (hard 제약 — 가장 빠른 일정).
 *
 * <p>{@link GreedyRotationAllocator} 가 본 결과로 호라이즌 필터 — production_date > deadline
 * 슬롯 사용 금지. 부족 시 {@code AllocationConflict.deadlineExceeded} 발행.
 *
 * <p>"Calculator" 네이밍 — {@code @Component} (NamingConventionTest services_end_with_Service 정합).
 */
@Component
@Profile("with-infra")
public class BackwardDeadlineCalculator {

    /** BR-X07 — 성형 완료 D-2 영업일 (압출 EP-07 = D-1 별도). */
    public static final int BACKWARD_DAYS = 2;

    private final WorkingCalendar calendar;

    public BackwardDeadlineCalculator(WorkingCalendar calendar) {
        this.calendar = calendar;
    }

    /**
     * hose_id → 가장 이른 납기 기준 deadline 맵.
     *
     * <p>caller 는 보통 {@code AllocationContext.ordersByHose()} 를 그대로 전달.
     */
    public DeadlineMap compute(Map<String, List<OrderInput>> ordersByHose) {
        if (ordersByHose == null || ordersByHose.isEmpty()) {
            return new DeadlineMap(Map.of());
        }
        Map<String, LocalDate> deadlines = new HashMap<>();
        for (Map.Entry<String, List<OrderInput>> entry : ordersByHose.entrySet()) {
            LocalDate earliest = entry.getValue().stream()
                .map(OrderInput::deliveryDate)
                .min(LocalDate::compareTo)
                .orElse(null);
            if (earliest == null) continue;
            deadlines.put(entry.getKey(), calendar.subtractWorkingDays(earliest, BACKWARD_DAYS));
        }
        return new DeadlineMap(deadlines);
    }

    /** 단일 납기일의 D-2 deadline. */
    public LocalDate deadlineFor(LocalDate deliveryDate) {
        return calendar.subtractWorkingDays(deliveryDate, BACKWARD_DAYS);
    }
}
