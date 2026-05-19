/**
 * 수주 정보 통합 모듈 (PDD-01).
 *
 * 영업 폴더 watch → 엑셀 파싱 (월별 예상·주간·확정·KD) → 통합 DB 적재.
 *
 * 의존: common (공통), master::api (품번 조회), audit::events (변경 audit 발행).
 * 발행 이벤트: OrderChangedEvent (수주 변경 시 vc·notify 구독).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Order Consolidation",
    allowedDependencies = { "common", "master::api", "audit::events" }
)
package com.scheduling.order;
