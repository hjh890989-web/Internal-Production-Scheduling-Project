package com.scheduling.audit.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Audit 캡쳐 마커 — TK-11-1-2 (EP-11 ST-11-1, BR-X02).
 *
 * <p>대상 메서드 실행 직전 {@link AuditableAspect} 가:
 * <ol>
 *   <li>Spring Security context 에서 actor 추출 ({@code "system"} fallback)</li>
 *   <li>{@code reason()} 을 Postgres session-local {@code audit.reason} 변수에 주입</li>
 *   <li>Postgres trigger 가 {@code current_setting('audit.actor'|'audit.reason', true)} 로 읽음</li>
 * </ol>
 *
 * <p>예:
 * <pre>
 *   &#64;Auditable("Planner 단건 확정")
 *   public VcSchedule confirm(UUID id, String plannerId) { ... }
 * </pre>
 *
 * <p>주의 — {@code @Transactional} 메서드 내부에서 호출되어야 함 (SET LOCAL 은 transaction 범위).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /** Audit log 의 reason 필드에 기록될 사유. */
    String value();
}
