package com.scheduling.vc.required;

/**
 * Q_required 계산 결과 단일 row — TK-05-3-1 (REQ-FUNC-VC-009).
 *
 * @param hoseId         품번
 * @param qNet           호라이즌 내 수주 합계
 * @param targetStock    목표 재고
 * @param currentStock   현재 재고
 * @param qRequired      max(0, qNet + targetStock − currentStock)
 */
public record QRequiredResult(
    String hoseId,
    int qNet,
    int targetStock,
    int currentStock,
    int qRequired
) {
    public boolean needsProduction() {
        return qRequired > 0;
    }
}
