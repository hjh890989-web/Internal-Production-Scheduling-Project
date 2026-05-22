package com.scheduling.master.api;

/**
 * ProductSpec 요약 DTO — TK-21-5-1 (cross-module).
 *
 * <p>vc 모듈 SpecLt7CapRule 이 본 record 만 사용 (Modulith 경계).
 *
 * @param hoseId      품번
 * @param spec        압출 규격값 (null = EX 마스터 미등록)
 * @param angleCount  슬롯당 점유 앵글 수
 * @param isSpecLt7   spec < 7 → BR-V17 적용 marker
 */
public record ProductSpecSummary(
    String hoseId,
    Integer spec,
    int angleCount,
    boolean isSpecLt7
) {}
