package com.scheduling.vc.mes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Sprint 23 ST-MES-1 — MES REST 응답 DTO (mock contract placeholder).
 *
 * <h2>Mock Contract — Sprint 26 S26-B carry-over</h2>
 * <p>본 DTO 는 실 vendor spec 미확보 상태의 <b>mock contract placeholder</b>이다.
 * <b>Sprint 26 S26-B: vendor 실 spec 수신 후 DTO 필드 재정의 예정</b> — 필드명·타입·nullable
 * 여부가 변경될 수 있음. 변경 시 {@link HttpMesShiftClient#fetchShift} JSON 매핑만 수정하며
 * {@link MesShiftPort}, {@link MesShiftClient} 인터페이스는 불변.
 *
 * <h2>현재 mock 필드 정의 (Sprint 23 baseline)</h2>
 * <ul>
 *   <li>{@code machineId} — 필수. 누락 시 {@link #isComplete()} = false → skip</li>
 *   <li>{@code shiftDate} — 필수 (LocalDate). 누락 시 skip</li>
 *   <li>{@code shiftNo} — 1..4 (주간전반/후반·야간전반/후반)</li>
 *   <li>{@code plannedQty} — shift 계획 수량</li>
 *   <li>{@code actualQty} — shift 진행 중 null 가능</li>
 *   <li>{@code receivedAt} — Instant. 미제공 시 polling 시점 대체</li>
 * </ul>
 *
 * <p>contract endpoint: {@code GET /api/mes/shift?machine=&date=&shift_no=} → 본 JSON.
 *
 * @see HttpMesShiftClient
 * @see MesShiftClient
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
