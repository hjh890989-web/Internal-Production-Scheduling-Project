package com.scheduling;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.scheduling.security.CustomAccessDeniedHandler;
import com.scheduling.security.CustomAuthenticationEntryPoint;
import com.scheduling.security.KeycloakJwtAuthConverter;
import com.scheduling.security.auth.AppUserDetailsService;
import com.scheduling.security.auth.JwtAuthenticationFilter;
import com.scheduling.security.auth.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

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
    private final String jwtSecret;
    private final boolean devFallback;

    public SecurityConfig(
        KeycloakJwtAuthConverter jwtConverter,
        CustomAccessDeniedHandler accessDeniedHandler,
        CustomAuthenticationEntryPoint authenticationEntryPoint,
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
        @Value("${app.auth.jwt.secret:DEV-INSECURE-32-CHARS-min-secret-replace-in-PROD}") String jwtSecret,
        // Sprint 10 ST-AUTH-6 — env APP_AUTH_DEV_FALLBACK=false 로 strict mode 진입 (사번+PIN 필수).
        // default true 유지 (본 PC 알파 + Sprint 11 RBAC 작업 중 호환). PROD/베타 진입 시 false.
        @Value("${app.auth.dev-fallback:true}") boolean devFallback
    ) {
        this.jwtConverter = jwtConverter;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.issuerUri = issuerUri;
        this.jwtSecret = jwtSecret;
        this.devFallback = devFallback;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        // Sprint 10 ST-AUTH-4 — 자체 JWT 필터 (사번+PIN 발급 토큰 검증).
        // 양 분기 (Keycloak / DEV anonymous) 모두에서 활성 — Bearer 헤더 있으면 검증, 없으면 통과.
        http.addFilterBefore(new JwtAuthenticationFilter(jwtService),
            UsernamePasswordAuthenticationFilter.class);

        if (issuerUri != null && !issuerUri.isBlank()) {
            // STG/PROD — Keycloak JWT resource server + RBAC strict (BR-X05 dual-review)
            http.authorizeHttpRequests(auth -> auth
                .requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()    // Sprint 10 사번+PIN 로그인 endpoint
                .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("IT_OPS")
                .anyRequest().authenticated()
            );
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        } else if (devFallback) {
            // DEV 가벼운 mode — 본 PC 알파 + Sprint 11~ 작업 중. JWT 있으면 인증, 없으면 anonymous fallback.
            http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().permitAll()
            );
            // anonymous 4 role 자동 부여 — JwtAuthenticationFilter 가 SecurityContext 설정 시 우선.
            http.anonymous(anon -> anon.authorities(
                "ROLE_PLANNER", "ROLE_STK_USER", "ROLE_IT_OPS", "ROLE_READ_ONLY"));
        } else {
            // strict mode (env APP_AUTH_DEV_FALLBACK=false) — 사번+PIN JWT 필수, anonymous 비활성.
            // Sprint 19 EP-BETA-LAUNCH 진입 시 (또는 사용자 명시 결정 시) 전환.
            http.authorizeHttpRequests(auth -> auth
                .requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("IT_OPS")
                .anyRequest().authenticated()
            );
        }

        return http.build();
    }

    // =========================================================================
    // Sprint 10 ST-AUTH-2 — 사번+PIN 인증 bean (NFR-SEC-007)
    //
    // BCryptPasswordEncoder strength=12 — 5명 사용자라 응답 ~200ms acceptable.
    // DaoAuthenticationProvider — AppUserDetailsService + PasswordEncoder.
    // AuthenticationManager — AuthController (ST-AUTH-4) 에서 inject 받아
    //   authenticate(UsernamePasswordAuthenticationToken) 호출. JWT 발급은 별도.
    // =========================================================================

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(AppUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    // =========================================================================
    // Sprint 10 ST-AUTH-4 — 자체 JWT 발급/검증 bean (HS256)
    //
    // secret — application.yml app.auth.jwt.secret (default = DEV-INSECURE,
    //   PROD 진입 시 env var JWT_HMAC_SECRET 필수). 32 bytes 이상 권장 (HS256 표준).
    // =========================================================================

    @Bean
    JwtEncoder jwtEncoder() throws Exception {
        OctetSequenceKey key = new OctetSequenceKey.Builder(
                jwtSecret.getBytes(StandardCharsets.UTF_8))
            .algorithm(JWSAlgorithm.HS256)
            .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }
}
