package com.scheduling.vc.mes;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Sprint 23 ST-MES-1 — 외부 MES 시스템 실적 조회 Port (Adapter 패턴 — read/fetch side).
 *
 * <h2>Adapter 패턴 역할 분리</h2>
 * <p>{@link MesShiftPort} 가 로컬 영속(reportProduction) 책임을 담당하고, 본 인터페이스는
 * 외부 MES 에서 머신·shift 실적을 <b>가져오는(fetch)</b> 책임만 담당한다.
 * 두 Port 를 분리함으로써 Excel 폴백·degraded mode (reportProduction 경유) 가
 * adapter 모드(jpa/http)와 무관하게 동작 — 회귀 0.
 *
 * <h2>구현체</h2>
 * <ul>
 *   <li>{@link HttpMesShiftClient} — {@code scheduling.mes.adapter=http} 시 활성 (REST polling)</li>
 *   <li>jpa(default) 모드 — fetch 클라이언트 미존재, {@link MesPollingService} 도 http 전용</li>
 * </ul>
 *
 * <p>Phase 5+ MQ/file adapter 는 본 인터페이스 신규 구현체로 교체 (port 불변).
 * Sprint 26 S26-B: vendor 실 spec 수신 후 {@link MesShiftResponse} DTO 필드 재정의.
 *
 * @see HttpMesShiftClient
 * @see MesShiftPort
 * @see MesShiftResponse
 */
public interface MesShiftClient {

    /**
     * MES 에서 (머신, 일자, shift) 실적 조회.
     *
     * @return 수신 데이터. MES 미수신 / 호출 실패(circuit OPEN·timeout) 시 {@link Optional#empty()}.
     */
    Optional<MesShiftResponse> fetchShift(String machineId, LocalDate shiftDate, short shiftNo);
}
