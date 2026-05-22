package com.scheduling.vc.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * VC 확정 도메인 이벤트 — TK-07-1-1 (EP-05 발행 → EP-07 압출 구독).
 *
 * <p>Spring Modulith 모듈 경계: ex 모듈은 vc 도메인 모델 (VcSchedule 등) 직접 의존 금지,
 * 본 record 만 의존 (vc::events namedinterface).
 *
 * @param scheduleId   확정된 스케줄 batch PK
 * @param confirmedAt  확정 시각 (Clock 주입, BR-X04)
 * @param rows         확정된 회전 슬롯 row 목록
 */
public record VcConfirmedEvent(
    UUID scheduleId,
    Instant confirmedAt,
    List<VcConfirmedRow> rows
) {
    public VcConfirmedEvent {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    /**
     * 확정된 단일 회전 슬롯 — EP-07 D-1 역산 + EP-08 yield 입력.
     *
     * @param rowId           VcSchedule PK
     * @param hoseId          품번
     * @param productionDate  성형 투입일 (D-1 역산 기준)
     * @param machineId       가류기 ID (LP-01~04 / IC-01)
     * @param rotationNo      회전 번호 (1~18)
     * @param slotPosition    슬롯 위치 (1~8)
     * @param vcYield         성형 회전당 yield (EP-08 압출 Q_ext 입력)
     */
    public record VcConfirmedRow(
        UUID rowId,
        String hoseId,
        LocalDate productionDate,
        String machineId,
        short rotationNo,
        short slotPosition,
        int vcYield
    ) {}
}
