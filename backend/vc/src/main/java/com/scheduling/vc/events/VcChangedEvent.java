package com.scheduling.vc.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * VC 변경 도메인 이벤트 — TK-EX13-1-1 (EP-EX13 ST-EX13-1, BR-X03·E11).
 *
 * <p>성형 확정 row 변경 (override / UPDATE) 시 발행. ex 모듈 listener 가 영향 EX
 * candidate 자동 재계산.
 *
 * <p>변경 종류 분류 (EP-10 Sprint 4 확장):
 * <ul>
 *   <li>QUANTITY — planned_qty 변경 → Q_ext 재계산</li>
 *   <li>DATE — productionDate 변경 → ex_deadline 재산출</li>
 *   <li>MACHINE — machineId 변경 → grouping 재배치</li>
 * </ul>
 *
 * <p>Sprint 3 단계: 이벤트 record + listener 기반 구조만 구축. Partial replan 실제
 * 수행은 Sprint 4 EP-10 (Confirmed 상태) 완료 후 본격 활성.
 *
 * @param scheduleId  변경된 batch
 * @param changedAt   변경 시각 (Clock 주입, BR-X04)
 * @param changedRows 변경된 row 목록 (before/after 비교)
 */
public record VcChangedEvent(
    UUID scheduleId,
    Instant changedAt,
    List<VcChangedRow> changedRows
) {
    public VcChangedEvent {
        changedRows = changedRows == null ? List.of() : List.copyOf(changedRows);
    }

    /**
     * 변경된 단일 row.
     *
     * @param rowId          VcSchedule PK
     * @param hoseId         품번
     * @param previousDate   변경 전 productionDate (null = 신규)
     * @param newDate        변경 후 productionDate
     * @param previousQty    변경 전 plannedQty (null = 신규)
     * @param newQty         변경 후 plannedQty
     * @param changeType     QUANTITY / DATE / MACHINE / DELETED
     */
    public record VcChangedRow(
        UUID rowId,
        String hoseId,
        LocalDate previousDate,
        LocalDate newDate,
        Integer previousQty,
        int newQty,
        ChangeType changeType
    ) {}

    public enum ChangeType {
        QUANTITY, DATE, MACHINE, DELETED
    }
}
