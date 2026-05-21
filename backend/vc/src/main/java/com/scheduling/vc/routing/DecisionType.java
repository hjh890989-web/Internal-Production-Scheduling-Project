package com.scheduling.vc.routing;

/**
 * 라우팅 결정 분류 — TK-05-4-2 (K-V06 가류기 사용률 KPI).
 *
 * <ul>
 *   <li>{@link #LP_PRIMARY} — LP_FIRST 정책으로 LP 가 첫 시도 → 성공</li>
 *   <li>{@link #LP_FALLBACK} — IC_FIRST 정책인데 IC 포화 → LP 폴백</li>
 *   <li>{@link #IC_PRIMARY} — IC_FIRST 정책 또는 LP yield 없는 IC-only 품번</li>
 *   <li>{@link #IC_FALLBACK} — LP_FIRST 정책인데 LP 포화 → IC 폴백 (BR-V08)</li>
 * </ul>
 */
public enum DecisionType {
    LP_PRIMARY,
    LP_FALLBACK,
    IC_PRIMARY,
    IC_FALLBACK
}
