package com.scheduling.ex.conflict;

/**
 * 압출 충돌 해소 대안 유형 — TK-EX12-1-1 (REQ-FUNC-EX-012).
 *
 * <p>4 base 대안 (SRS 명시) + 운영 확장 2종 = 6 type. 압출 도메인 특화 — 성형 EP-VC15
 * 대안과 일부 중복하지만 차별화 (EARLIER_START vs NIGHT_ROTATION).
 */
public enum ExAlternativeType {
    EARLIER_START(       "조기 시작",         "전일 야간 후반 활용 — 압출 시작 1일 앞당김"),
    NIGHT_SECOND_BOOST(  "야간 후반 보강",    "NIGHT_LATE shift 추가 배치"),
    VC_DATE_NEGOTIATE(   "성형 투입일 협상", "vc_date 1일 연장 → ex_deadline +1"),
    OUTSOURCE(           "외주 처리",         "외주사 발주로 압출 capa 보강"),
    OVERTIME(            "잔업 적용",         "shift effective_min 일시 확장 (75% → 90%)"),
    SWAP_CANDIDATE(      "후보 순서 조정",   "낮은 우선순위 후보를 다음 horizon 으로 이동");

    private final String label;
    private final String description;

    ExAlternativeType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }
    public String description() { return description; }
}
