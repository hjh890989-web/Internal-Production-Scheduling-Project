/**
 * 마스터 데이터 모듈 — 47품번 마스터, 성형/압출 제약, KD 발주량 sync.
 *
 * REQ-FUNC-CO-002 dual-review (작성자 ≠ 승인자, BR-X05).
 * BR-V12/V13 활성 트리거 (수주 통합 후).
 *
 * 의존: common, audit::events (마스터 변경 audit 발행).
 * 발행 이벤트: MasterChangedEvent (vc·ex 구독 — 제약 갱신).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Master Data",
    allowedDependencies = { "common", "audit::events" }
)
package com.scheduling.master;
