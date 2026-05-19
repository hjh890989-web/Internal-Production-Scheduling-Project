/**
 * 감사 모듈 — BR-X02 mutation audit log 강제.
 *
 * REQ-NF-SEC-004: ≥3년 보존, schema 'audit' INSERT-only role.
 * 모든 도메인 모듈의 audit 이벤트를 수신 (단방향).
 *
 * 의존: common (만).
 * 발행 이벤트: AuditRecordedEvent (notify 구독 — 위반 감지 시 알림).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Audit Log",
    allowedDependencies = { "common" }
)
package com.scheduling.audit;
