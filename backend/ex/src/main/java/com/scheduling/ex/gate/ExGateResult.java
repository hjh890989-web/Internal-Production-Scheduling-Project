package com.scheduling.ex.gate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 압출 검증 게이트 결과 — TK-EX11-1-1·2·3 (REQ-FUNC-EX-011).
 *
 * @param candidateId   검증된 ExScheduleCandidate
 * @param passed        모든 검증 통과 (violations 비어있음)
 * @param violations    실패 사유 (≥1 시 passed=false)
 * @param measuredAt    측정 시각 (Clock 주입, BR-X04)
 */
public record ExGateResult(
    UUID candidateId,
    boolean passed,
    List<ExGateViolation> violations,
    Instant measuredAt
) {
    public ExGateResult {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }
}
