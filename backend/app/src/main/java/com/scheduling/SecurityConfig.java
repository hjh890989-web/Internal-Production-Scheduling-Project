package com.scheduling;

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
 *
 * <p>본 클래스는 application 합성 패키지({@code com.scheduling}) 에 위치 — Spring Modulith
 * 모듈 경계({@link com.scheduling.order}, {@link com.scheduling.vc} 등) 와 별개의 launcher 구성.
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
