package com.scheduling.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sprint 0 baseline SecurityConfig.
 *
 * <p>현재 Sprint 0 은 인프라 baseline — RBAC + Keycloak 통합은 ST-30-2 (Sprint 1) 에서 도입.
 * 그때까지 actuator scrape 만 permitAll, 나머지는 잠금 유지.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(b -> {});
        return http.build();
    }
}
