package com.scheduling.vc.mes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Sprint 23 ST-MES-1 — MES REST 응답 DTO (mock contract — 실 vendor spec Phase 5+ 교체).
 *
 * <p>contract: {@code GET /api/mes/shift?machine=&date=&shift_no=} → 본 JSON.
 * {@code actualQty} 는 shift 진행 중 null 가능. {@code receivedAt} 미제공 시 polling 시점 사용.
 *
 * <p>{@code machineId} 누락(부분 응답) 시 호출부에서 skip — {@link MesPollingService} 검증.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MesShiftResponse(
    String machineId,
    LocalDate shiftDate,
    short shiftNo,
    int plannedQty,
    Integer actualQty,
    Instant receivedAt
) {
    /** 필수 필드(machineId/shiftDate) 존재 여부 — 부분 응답 방어. */
    public boolean isComplete() {
        return machineId != null && !machineId.isBlank() && shiftDate != null;
    }
}
