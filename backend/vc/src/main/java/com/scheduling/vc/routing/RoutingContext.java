package com.scheduling.vc.routing;

import java.time.LocalDate;

/**
 * 라우팅 정책 의사결정 컨텍스트 — TK-05-4-1.
 *
 * <p>LP 우선 정책은 hose_id 만 보지만, 향후 BALANCED 정책은 현재 LP/IC 가동률 기반 결정.
 *
 * @param date                    배치 대상 일자
 * @param currentLpUtilizationPct 현재 LP 가동률 (0~100). BALANCED 정책 입력.
 * @param currentIcUtilizationPct 현재 IC 가동률 (0~100).
 */
public record RoutingContext(
    LocalDate date,
    int currentLpUtilizationPct,
    int currentIcUtilizationPct
) {
    public static RoutingContext initial(LocalDate date) {
        return new RoutingContext(date, 0, 0);
    }
}
