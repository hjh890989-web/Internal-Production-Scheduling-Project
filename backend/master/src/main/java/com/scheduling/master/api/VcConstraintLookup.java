package com.scheduling.master.api;

import java.util.List;
import java.util.Optional;

/**
 * VcConstraint 조회 facade — TK-05-2-1·2 (Modulith cross-module).
 *
 * <p>{@code com.scheduling.master.vc.VcConstraintRepository} 는 master 모듈 내부.
 * vc 모듈 (yield calculator + angle validator) 은 본 인터페이스만 사용 (allowedDependencies=master::api).
 *
 * <p>구현체: {@code com.scheduling.master.vc.VcConstraintLookupImpl}.
 */
public interface VcConstraintLookup {

    /** 47품번 일괄 조회 — YieldMatrix 빌드 hot path. */
    List<VcConstraintSummary> findAll();

    Optional<VcConstraintSummary> findById(String hoseId);
}
