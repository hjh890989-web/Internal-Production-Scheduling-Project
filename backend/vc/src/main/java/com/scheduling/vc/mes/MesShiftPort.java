package com.scheduling.vc.mes;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Sprint 17 BR-X06 MES 영속 Port (Adapter 패턴 — write side) — TK-DAY-LOCK-3-1.
 *
 * <h2>Adapter 패턴 구조</h2>
 * <pre>
 * MesShiftPort          ← write/영속 Port (본 인터페이스)
 *   └─ JpaMesShiftPort  ← @ConditionalOnProperty(adapter=jpa, matchIfMissing=true)
 *                           + @Profile("with-infra") — DEV/STG DB stub
 *
 * MesShiftClient        ← fetch/read Port (외부 MES 조회 전용)
 *   └─ HttpMesShiftClient ← @ConditionalOnProperty(adapter=http) — PROD REST polling
 * </pre>
 *
 * <p>write-side (reportProduction) 와 read-side (fetchShift) 를 두 Port 로 분리하여
 * Excel 폴백·degraded mode 가 adapter 모드(jpa/http)와 무관하게 동작. 회귀 0.
 *
 * <p>Phase 5+ 실 MES 연동 (HTTP/MQ/file) 시 {@link MesShiftClient} 구현체만 교체.
 * 본 Port 는 변경 없음.
 *
 * <p>BR-X06: 1 shift (4 shifts/day, 6h each) 미수신 시 degraded mode 진입.
 *
 * @see JpaMesShiftPort
 * @see MesShiftClient
 * @see HttpMesShiftClient
 * @see DegradedModeService
 */
public interface MesShiftPort {

    /**
     * MES 또는 Excel 폴백으로 shift 결과 보고.
     *
     * <p>UNIQUE (machine_id, shift_date, shift_no) 충돌 시 UPDATE.
     * source = EXCEL_FALLBACK 시 reportedBy 필수 (BR-X02 감사).
     */
    MesShiftEvent reportProduction(String machineId, LocalDate shiftDate, short shiftNo,
                                    int plannedQty, Integer actualQty,
                                    MesShiftSource source, String reportedBy, String note);

    /**
     * 머신의 가장 최근 수신 shift event — degraded 임계 감지.
     *
     * @see DegradedModeService
     */
    Optional<MesShiftEvent> lastReceivedShift(String machineId);
}
