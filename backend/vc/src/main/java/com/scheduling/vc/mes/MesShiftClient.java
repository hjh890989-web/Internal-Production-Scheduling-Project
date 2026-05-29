package com.scheduling.vc.mes;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Sprint 23 ST-MES-1 — 외부 MES 시스템 실적 조회 클라이언트 (EP-MES-ADAPTER-1).
 *
 * <p>{@link MesShiftPort} 가 로컬 영속(reportProduction) 책임이라면, 본 인터페이스는 외부 MES
 * 에서 머신·shift 실적을 <b>가져오는(fetch)</b> 책임. 둘을 분리해 Excel 폴백·degraded mode
 * (reportProduction 경유) 가 adapter 모드와 무관하게 동작 — 회귀 0.
 *
 * <p>구현체:
 * <ul>
 *   <li>{@link HttpMesShiftClient} — {@code scheduling.mes.adapter=http} 시 활성 (REST polling)</li>
 *   <li>jpa(default) 모드는 fetch 클라이언트 미존재 — {@link MesPollingService} 도 http 전용</li>
 * </ul>
 *
 * <p>Phase 5+ MQ/file adapter 는 본 인터페이스 신규 구현체로 교체 (port 불변).
 */
public interface MesShiftClient {

    /**
     * MES 에서 (머신, 일자, shift) 실적 조회.
     *
     * @return 수신 데이터. MES 미수신 / 호출 실패(circuit OPEN·timeout) 시 {@link Optional#empty()}.
     */
    Optional<MesShiftResponse> fetchShift(String machineId, LocalDate shiftDate, short shiftNo);
}
