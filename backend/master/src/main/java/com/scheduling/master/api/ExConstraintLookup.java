package com.scheduling.master.api;

import java.util.Optional;

/**
 * 압출 제약 facade — TK-08-2-1 (Modulith cross-module).
 *
 * <p>ex 모듈 {@code YieldFormula} / {@code ExtrusionDemandCalculator} 가 speed / length /
 * die / line 조회. {@code spec_value} / {@code angle_count} 는 기존 {@link ProductSpecLookup}
 * 과 중복이지만 ex 모듈 단일 진입점 편의 제공.
 */
public interface ExConstraintLookup {

    Optional<ExConstraintSummary> findById(String hoseId);
}
