package com.scheduling.ex.grouping;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * shift 단일 그룹 배정 결과 — TK-09-1-2 (BR-E06·E07).
 *
 * <p>한 (date, machineId, shiftCode) 슬롯에 단일 셋팅 그룹 + 호환 hose 들 묶음.
 *
 * @param date          압출 일자
 * @param lineCode      압출 라인 식별자 (ex_constraint.line_code)
 * @param shiftCode     shift (DAY_EARLY / DAY_LATE / NIGHT_EARLY / NIGHT_LATE)
 * @param groupNumber   선택된 셋팅 그룹 1~8
 * @param candidateIds  본 shift 에 배정된 ExScheduleCandidate IDs
 */
public record ShiftAssignment(
    LocalDate date,
    String lineCode,
    String shiftCode,
    short groupNumber,
    List<UUID> candidateIds
) {
    public ShiftAssignment {
        candidateIds = candidateIds == null ? List.of() : List.copyOf(candidateIds);
    }
}
