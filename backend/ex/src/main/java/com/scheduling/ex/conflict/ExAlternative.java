package com.scheduling.ex.conflict;

/**
 * 압출 충돌 해소 대안 — TK-EX12-1-1 (REQ-FUNC-EX-012).
 *
 * <p>{@code suggestedAction} = candidate / hose 별 차별화된 제안 (UI 표시).
 */
public record ExAlternative(
    ExAlternativeType type,
    String label,
    String description,
    String suggestedAction
) {
    public static ExAlternative of(ExAlternativeType type, ExClassifiedConflict cc) {
        return new ExAlternative(type, type.label(), type.description(),
            buildAction(type, cc));
    }

    private static String buildAction(ExAlternativeType type, ExClassifiedConflict cc) {
        String hose = cc.hoseId();
        return switch (type) {
            case EARLIER_START      -> "%s 전일 야간 후반 활용".formatted(hose);
            case NIGHT_SECOND_BOOST -> "%s NIGHT_LATE shift 추가 배치".formatted(hose);
            case VC_DATE_NEGOTIATE  -> "%s 성형 vc_date +1일 협상".formatted(hose);
            case OUTSOURCE          -> "%s 외주 발주".formatted(hose);
            case OVERTIME           -> "%s shift 잔업 (75%% → 90%%)".formatted(hose);
            case SWAP_CANDIDATE     -> "%s 우선순위 ↓ + 다음 horizon 이동".formatted(hose);
        };
    }
}
