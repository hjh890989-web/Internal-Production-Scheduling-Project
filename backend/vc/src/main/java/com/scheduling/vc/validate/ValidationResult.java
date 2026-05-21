package com.scheduling.vc.validate;

import com.scheduling.vc.allocator.AllocationConflict;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * validate-all API 응답 — TK-VC16-1-1 (REQ-FUNC-VC-016).
 *
 * <p>모든 위반 수집 (early-exit 금지) + 카테고리별 카운트 summary.
 *
 * @param fromDate    검사 호라이즌 시작
 * @param toDate      검사 호라이즌 끝 (양 끝 포함)
 * @param violations  위반 목록
 * @param summary     카테고리별 카운트 (위반 없으면 빈 맵)
 * @param totalRows   검사된 row 수
 * @param executedAt  실행 시각 (UTC, BR-X04 Clock 주입)
 */
public record ValidationResult(
    LocalDate fromDate,
    LocalDate toDate,
    List<ValidationViolation> violations,
    Map<AllocationConflict.Category, Long> summary,
    int totalRows,
    Instant executedAt
) {
    public ValidationResult {
        violations = violations == null ? List.of() : List.copyOf(violations);
        summary = summary == null ? Map.of() : Map.copyOf(summary);
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }
}
