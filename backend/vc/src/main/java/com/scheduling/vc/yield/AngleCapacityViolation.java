package com.scheduling.vc.yield;

import java.time.LocalDate;

/**
 * 앵글 capa 위반 — TK-05-2-2 (BR-V06).
 *
 * <p>같은 (date, hoseId, machineType, rotationNo) 그룹의 동시 점유 슬롯 수가 앵글 보유량 초과.
 * 한국어 메시지 — REQ-NF-USA-002 사유 명시.
 *
 * @param date              생산일
 * @param hoseId            품번
 * @param machineType       "LP" / "IC"
 * @param rotationNo        회전 (1~18)
 * @param actualSlotsUsed   동시 점유 슬롯 수
 * @param allowedAngles     앵글 보유 한도 (lp/ic_angle_qty)
 */
public record AngleCapacityViolation(
    LocalDate date,
    String hoseId,
    String machineType,
    int rotationNo,
    int actualSlotsUsed,
    int allowedAngles
) {
    public String userMessage() {
        return "앵글 과초과: %s %s 회전 %d %s 가류기 슬롯 %d개 점유, 보유 %d개 한도 초과 (BR-V06)"
            .formatted(date, hoseId, rotationNo, machineType, actualSlotsUsed, allowedAngles);
    }
}
