package com.scheduling.master.api;

import java.math.BigDecimal;

/**
 * 압출 제약 요약 — TK-08-2-1 (cross-module).
 *
 * <p>ex 모듈 {@code YieldFormula} 가 본 record 로 speed/length 조회 (BR-E05 수식 입력).
 *
 * @param hoseId         품번
 * @param specValue      규격값 (null 가능)
 * @param angleCount     슬롯당 점유 앵글 수
 * @param speedMPerMin   압출 속도 m/min (null = 마스터 미설정)
 * @param lengthMm       단위 길이 mm (null = 마스터 미설정)
 * @param dieCode        다이 식별자 (EP-09 셋팅 그룹핑 입력)
 * @param lineCode       압출 라인 식별자
 */
public record ExConstraintSummary(
    String hoseId,
    Integer specValue,
    int angleCount,
    BigDecimal speedMPerMin,
    Integer lengthMm,
    String dieCode,
    String lineCode
) {
    public boolean hasYieldInput() {
        return speedMPerMin != null && lengthMm != null && lengthMm > 0;
    }
}
