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
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        if (issuerUri != null && !issuerUri.isBlank()) {
            // STG/PROD — JWT resource server + RBAC strict (BR-X05 dual-review)
            http.authorizeHttpRequests(auth -> auth
                .requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("IT_OPS")
                .anyRequest().authenticated()
            );
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        } else {
            // DEV 가벼운 mode (KEYCLOAK_ISSUER_URI 미설정) — 개발자 PC 또는 사내 LAN 베타
            // (사용자 ~10명, BR-X05 우회 OK — 사내 한정).
            // 사내 STG/PROD 진입 시 KEYCLOAK_ISSUER_URI 설정 → 위 분기 자동 활성.
            // @PreAuthorize method security 는 본 분기에서도 활성 (테스트 IT @WithMockUser 호환).
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }

        return http.build();
    }
}
