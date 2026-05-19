/**
 * 압출 스케줄링 모듈 (PDD-03).
 *
 * 4-shift × 75% 가동률. 성형 일정 cascade (vc.changed → ex 자동 역산 — REQ-FUNC-EX-013).
 *
 * 의존: common, master::api, audit::events, vc::events.
 * 발행 이벤트: ExScheduleChangedEvent (notify 구독).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "EX Scheduling",
    allowedDependencies = { "common", "master::api", "audit::events", "vc::events" }
)
package com.scheduling.ex;
