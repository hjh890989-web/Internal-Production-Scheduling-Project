package com.scheduling.vc.capacity;

import com.scheduling.master.api.VcMachineQuery;
import com.scheduling.master.api.VcMachineSummary;
import com.scheduling.master.api.WorkingCalendar;
import com.scheduling.vc.domain.RotationSlot;
import com.scheduling.vc.domain.VcSchedule;
import com.scheduling.vc.domain.VcScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CapacityLedger 빌드 — TK-05-1-2 (BR-V04 / BR-V05 / REQ-FUNC-VC-005).
 *
 * <p>호라이즌 입력 → active 가류기 + 영업일 × 18 회전 × totalSlots 격자 생성 →
 * 기존 VcSchedule 반영 (CANDIDATE→RESERVED, CONFIRMED/DONE→CONFIRMED).
 *
 * <p>{@code @Profile("with-infra")} — VcScheduleRepository (JPA) 의존.
 * master 데이터는 {@link VcMachineQuery} facade 로 cross-module 안전 접근.
 *
 * <p>성능 — 7일 호라이즌 ≤ 100ms (684 × 7 ≈ 4800 entry HashMap put).
 */
@Component
@Profile("with-infra")
public class CapacityLedgerBuilder {

    private static final Logger log = LoggerFactory.getLogger(CapacityLedgerBuilder.class);

    private final VcMachineQuery machineQuery;
    private final VcScheduleRepository scheduleRepo;
    private final WorkingCalendar calendar;

    public CapacityLedgerBuilder(
        VcMachineQuery machineQuery,
        VcScheduleRepository scheduleRepo,
        WorkingCalendar calendar
    ) {
        this.machineQuery = machineQuery;
        this.scheduleRepo = scheduleRepo;
        this.calendar = calendar;
    }

    public CapacityLedger build(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate/toDate 필수");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate >= fromDate 필수");
        }

        long startNanos = System.nanoTime();
        List<VcMachineSummary> machines = machineQuery.findAllActive();
        List<VcSchedule> existing = scheduleRepo.findByDateRange(fromDate, toDate);

        Map<RotationSlot, SlotAvailability> cells = new HashMap<>();
        for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
            if (!calendar.isWorkingDay(d)) continue;
            for (VcMachineSummary m : machines) {
                int totalRotations = m.totalRotationsPerDay();
                for (int rot = 1; rot <= totalRotations; rot++) {
                    for (int slot = 1; slot <= m.totalSlots(); slot++) {
                        cells.put(new RotationSlot(d, m.machineId(), rot, slot),
                            SlotAvailability.AVAILABLE);
                    }
                }
            }
        }

        // 기존 스케줄 반영 — CANDIDATE→RESERVED, CONFIRMED/DONE→CONFIRMED
        for (VcSchedule s : existing) {
            RotationSlot key = s.asSlot();
            cells.put(key, mapStatus(s.getStatus()));
        }

        CapacityLedger ledger = new CapacityLedger(fromDate, toDate, cells);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        log.info("CapacityLedger built {} ~ {} — cells={}, machines={}, schedules={}, elapsed={}ms",
            fromDate, toDate, cells.size(), machines.size(), existing.size(), elapsedMs);
        return ledger;
    }

    private SlotAvailability mapStatus(com.scheduling.vc.domain.VcScheduleStatus status) {
        return switch (status) {
            case CANDIDATE -> SlotAvailability.RESERVED;
            case CONFIRMED, DONE -> SlotAvailability.CONFIRMED;
        };
    }
}
