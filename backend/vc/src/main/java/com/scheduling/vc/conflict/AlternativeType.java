package com.scheduling.vc.conflict;

/**
 * 충돌 해소 대안 유형 — TK-VC15-1-2 (REQ-FUNC-VC-015).
 *
 * <p>4 base 대안 (SRS 명시) + 운영 확장 2종 = 6 type.
 */
public enum AlternativeType {
    NIGHT_ROTATION(    "야간 회전 추가",     "주간 회전 외 야간 회전 추가 활용"),
    DEADLINE_NEGOTIATE("납기 협상",         "고객과 납기 D+N 연장 협의"),
    IC_ROUTING(        "IC 라우팅 전환",   "LP 대신 IC 가류기 사용 (BR-V08 fallback)"),
    OUTSOURCE(         "외주 처리",         "외주사 발주로 capa 보강"),
    EXPAND_CAPA(       "capa 확장",         "주말 가동·임시 인력 투입"),
    SWAP_ORDER(        "수주 우선순위 조정", "낮은 우선순위 수주를 다음 주로 이동");

    private final String label;
    private final String description;

    AlternativeType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }
    public String description() { return description; }
}
