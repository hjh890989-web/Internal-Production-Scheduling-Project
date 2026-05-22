package com.scheduling.vc.conflict;

/**
 * 충돌 카테고리 — TK-VC15-1-1 (REQ-FUNC-VC-015).
 *
 * <p>{@link com.scheduling.vc.allocator.AllocationConflict} 의 reason 텍스트에서
 * BR 코드 추출 → 본 enum 매핑. UI 요약 + 대안 생성 입력.
 */
public enum ConflictCategory {
    SLOT_OX(           "BR-V13", "슬롯 O/X 부적합"),
    ANGLE_CAPA(        "BR-V06", "앵글 capa 초과"),
    DAILY_CAPA(        "BR-V03", "일일 capa 초과"),
    DEADLINE_D2(       "BR-X07", "납기 D-2 초과"),
    DAY_LOCK(          "BR-V07", "당일 락 위반"),
    LEFT_RIGHT(        "BR-V15·V16", "좌/우 셋팅 위반"),
    MACHINE_PIN(       "BR-V14", "고정 호기 위반"),
    SPEC_LT7(          "BR-V17", "규격<7 가류기당 ≤4 초과"),
    HOSE_CAP(          "BR-V14·V15·V16", "회전당 동시 슬롯 상한 초과"),
    UNSCHEDULABLE(     "BR-V11", "모든 슬롯 X — 외주·재고 대응"),
    UNKNOWN(           "BR-???", "미분류");

    private final String brCode;
    private final String description;

    ConflictCategory(String brCode, String description) {
        this.brCode = brCode;
        this.description = description;
    }

    public String brCode() { return brCode; }
    public String description() { return description; }
}
