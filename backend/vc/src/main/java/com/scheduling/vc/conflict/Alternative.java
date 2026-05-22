package com.scheduling.vc.conflict;

/**
 * 충돌 해소 대안 — TK-VC15-1-2 (REQ-FUNC-VC-015).
 *
 * <p>{@code suggestedAction} = hoseId 별 차별화된 제안 (UI 표시).
 */
public record Alternative(
    AlternativeType type,
    String label,
    String description,
    String suggestedAction
) {
    public static Alternative of(AlternativeType type, ClassifiedConflict cc) {
        return new Alternative(type, type.label(), type.description(),
            buildAction(type, cc));
    }

    private static String buildAction(AlternativeType type, ClassifiedConflict cc) {
        String hose = cc.hoseId();
        return switch (type) {
            case NIGHT_ROTATION     -> "%s 야간 회전 +5 슬롯 시도".formatted(hose);
            case DEADLINE_NEGOTIATE -> "%s 납기 D+3 협상".formatted(hose);
            case IC_ROUTING         -> "%s IC 가류기 라우팅".formatted(hose);
            case OUTSOURCE          -> "%s 외주 발주".formatted(hose);
            case EXPAND_CAPA        -> "주말 가동 + %s 우선 배치".formatted(hose);
            case SWAP_ORDER         -> "%s 우선순위 ↑ + 후순위 수주 이동".formatted(hose);
        };
    }
}
