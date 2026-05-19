/**
 * 공통 모듈 — OPEN 타입.
 *
 * Modulith OPEN 모듈 = 모든 internal API 가 자동 public.
 * 본 시스템의 모든 모듈이 common.* 의 모든 클래스를 자유롭게 import 가능.
 *
 * 본인은 다른 어떤 모듈에도 의존 금지 (순환 금지 — SAD §4.1).
 * TK-00-2-3 ArchUnit 검증으로 강제.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Common",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.scheduling.common;
