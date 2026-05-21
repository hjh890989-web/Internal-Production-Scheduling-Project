package com.scheduling.vc.validate;

import com.scheduling.vc.allocator.AllocationConflict;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 단일 스케줄 row 의 위반 — TK-VC16-1-1 (REQ-FUNC-VC-016).
 *
 * @param vcScheduleId   위반 row PK
 * @param hoseId         품번
 * @param productionDate 생산 일자
 * @param machineId      가류기
 * @param category       {@link AllocationConflict.Category} 재사용 (5 카테고리)
 * @param reason         한국어 사유 (UI/리포트용)
 */
public record ValidationViolation(
    UUID vcScheduleId,
    String hoseId,
    LocalDate productionDate,
    String machineId,
    AllocationConflict.Category category,
    String reason
) {}
