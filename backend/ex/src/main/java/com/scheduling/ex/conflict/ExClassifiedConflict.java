package com.scheduling.ex.conflict;

import com.scheduling.ex.gate.ExGateViolation;

import java.util.UUID;

/**
 * 분류된 압출 충돌 — TK-EX12-1-1.
 *
 * @param candidateId  검증 fail candidate
 * @param hoseId       품번
 * @param category     {@link ExConflictCategory} 매핑
 * @param violation    원본 게이트 위반
 */
public record ExClassifiedConflict(
    UUID candidateId,
    String hoseId,
    ExConflictCategory category,
    ExGateViolation violation
) {}
