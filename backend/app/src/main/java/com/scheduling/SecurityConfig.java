package com.scheduling;

import com.scheduling.security.CustomAccessDeniedHandler;
import com.scheduling.security.CustomAuthenticationEntryPoint;
import com.scheduling.security.KeycloakJwtAuthConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 — JWT Resource Server (Keycloak) — TK-30-2-2.
 *
 * <p>설계:
 * <ul>
 *   <li>{@code @EnableMethodSecurity} — controller method 의 {@code @PreAuthorize} 활성</li>
 *   <li>JWT resource server — Keycloak issuer 토큰 검증 ({@link KeycloakJwtAuthConverter} 로 role 매핑)</li>
 *   <li>STATELESS — JWT 기반 무상태 (CSRF disable OK)</li>
 *   <li>permitAll: Actuator(health/info/prometheus) + Swagger UI + OpenAPI docs + auth endpoint</li>
 *   <li>{@link CustomAccessDeniedHandler} 403 + {@link CustomAuthenticationEntryPoint} 401 — ProblemDetail RFC 7807</li>
 * </ul>
 *
 * <p>JWT 활성 조건: {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} 또는
 * {@code jwk-set-uri} 가 application.yml 에 설정되어 있으면 자동 활성. 미설정 시 (DEV baseline)
 * 모든 요청 permitAll 폴백 — 개발 편의 (PROD/STG 는 반드시 issuer-uri 설정).
 *
 * <p>RBAC 매트릭스: {@code docs/security/rbac-matrix.md} 참조.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final KeycloakJwtAuthConverter jwtConverter;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final String issuerUri;

    public SecurityConfig(
        KeycloakJwtAuthConverter jwtConverter,
        CustomAccessDeniedHandler accessDeniedHandler,
        CustomAuthenticationEntryPoint authenticationEntryPoint,
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri
    ) {
        this.jwtConverter = jwtConverter;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.issuerUri = issuerUri;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // permitAll — Actuator (health·info·prometheus only) + Swagger + auth endpoint
                .requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                // 그 외 actuator endpoint 는 IT_OPS만
                .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("IT_OPS")
                // 인증 요구 (JWT 비활성 시 본 정책은 effective 안 됨 — Sprint 0 baseline DEV 폴백)
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        // JWT resource server 활성 조건: issuer-uri 설정된 경우만 (PROD/STG/with-infra).
        // DEV baseline (issuer-uri 빈 값) 은 httpBasic 폴백.
        if (issuerUri != null && !issuerUri.isBlank()) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        } else {
            http.httpBasic(b -> {});
        }

        return http.build();
    }
}
