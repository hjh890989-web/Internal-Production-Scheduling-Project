package com.scheduling.ex.conflict;

/**
 * 압출 충돌 카테고리 — TK-EX12-1-1 (REQ-FUNC-EX-012).
 *
 * <p>{@link com.scheduling.ex.gate.ExGateViolation.Category} (2종) +
 * 압출 도메인 확장 (D-1 위반·셋업 위반·그룹 혼합 등).
 */
public enum ExConflictCategory {
    CUMULATIVE_YIELD_SHORT(    "BR-E10", "누적 yield < Q_ext"),
    SHIFT_CAPACITY_EXCEEDED(   "BR-E04", "shift effective_min 초과"),
    DEADLINE_D1_VIOLATION(     "BR-E01", "압출 D-1 deadline 위반"),
    SETUP_REQUIRED(            "BR-E06", "shift 내 셋업 변경 발생"),
    GROUP_MIXED(               "BR-E07", "다른 셋팅 그룹 같은 shift 혼합"),
    UNKNOWN(                   "BR-???", "미분류");

    private final String brCode;
    private final String description;

    ExConflictCategory(String brCode, String description) {
        this.brCode = brCode;
        this.description = description;
    }

    public String brCode() { return brCode; }
    public String description() { return description; }
}
