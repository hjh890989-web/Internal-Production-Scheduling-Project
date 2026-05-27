package com.scheduling.vc.mes;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Sprint 17 BR-X06 MES 인터페이스 — TK-DAY-LOCK-3-1.
 *
 * <p>Sprint 17 baseline = JPA-backed in-memory stub ({@link JpaMesShiftPort}).
 * 실 MES 연동 (HTTP/MQ/file) 은 Phase 5+ — 인터페이스만 정의 + adapter 교체.
 *
 * <p>BR-X06: 1 shift (4 shifts/day, 6h each) 미수신 시 degraded mode 진입.
 */
public interface MesShiftPort {

    /** MES 또는 Excel 폴백으로 shift 결과 보고 (UNIQUE (machine, date, shift_no) 시 UPDATE). */
    MesShiftEvent reportProduction(String machineId, LocalDate shiftDate, short shiftNo,
                                    int plannedQty, Integer actualQty,
                                    MesShiftSource source, String reportedBy, String note);

    /** 머신의 가장 최근 수신 shift event — degraded 임계 감지. */
    Optional<MesShiftEvent> lastReceivedShift(String machineId);
}
