/**
 * Audit AOP NamedInterface — EP-11 ST-11-1 ({@code @Auditable}).
 *
 * <p>다른 모듈 (vc, ex, order) 이 본 패키지의 {@link com.scheduling.audit.aop.Auditable}
 * 어노테이션 + Aspect Bean 을 사용. AspectJ 가 자동 wrap.
 */
@org.springframework.modulith.NamedInterface("aop")
package com.scheduling.audit.aop;
