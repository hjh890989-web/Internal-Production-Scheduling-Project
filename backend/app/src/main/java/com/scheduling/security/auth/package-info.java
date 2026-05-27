/**
 * Sprint 10 EP-AUTH — 사번+PIN 인증 (NFR-SEC-007).
 *
 * <p>{@link AppUser} entity + {@link AppUserRepository} + {@link AppUserDetailsService}
 * (Spring Security UserDetailsService) + {@link LoginAttemptService} (5회/10분 잠금) +
 * {@link JwtService} (HS256 8h) + {@link JwtAuthenticationFilter} (Bearer → SecurityContext) +
 * {@link AuthController} (POST /api/v1/auth/login).
 *
 * <p>{@link org.springframework.modulith.NamedInterface @NamedInterface} — 본 패키지의
 * public type 들을 root {@code com.scheduling.SecurityConfig} 에서 사용 (bean 등록 + filter 체인).
 */
@org.springframework.modulith.NamedInterface
package com.scheduling.security.auth;
