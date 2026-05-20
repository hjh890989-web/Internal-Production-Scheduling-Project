package com.scheduling.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KeycloakJwtAuthConverter 회귀 — TK-30-2-1.
 */
class KeycloakJwtAuthConverterTest {

    private final KeycloakJwtAuthConverter converter = new KeycloakJwtAuthConverter();

    private Jwt buildJwt(String username, List<String> roles) {
        Map<String, Object> claims = Map.of(
            "preferred_username", username,
            "realm_access", Map.of("roles", roles)
        );
        return new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "RS256"),
            claims
        );
    }

    @Test
    @DisplayName("PLANNER role → ROLE_PLANNER GrantedAuthority")
    void planner_role_mapped() {
        Jwt jwt = buildJwt("12345678", List.of("PLANNER"));
        AbstractAuthenticationToken token = converter.convert(jwt);
        assertThat(authorityNames(token)).containsExactly("ROLE_PLANNER");
        assertThat(token.getName()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("STK_USER role → ROLE_STK_USER")
    void stk_user_role_mapped() {
        Jwt jwt = buildJwt("12345679", List.of("STK_USER"));
        assertThat(authorityNames(converter.convert(jwt))).containsExactly("ROLE_STK_USER");
    }

    @Test
    @DisplayName("IT_OPS role → ROLE_IT_OPS")
    void it_ops_role_mapped() {
        Jwt jwt = buildJwt("12345680", List.of("IT_OPS"));
        assertThat(authorityNames(converter.convert(jwt))).containsExactly("ROLE_IT_OPS");
    }

    @Test
    @DisplayName("READ_ONLY role → ROLE_READ_ONLY")
    void read_only_role_mapped() {
        Jwt jwt = buildJwt("12345681", List.of("READ_ONLY"));
        assertThat(authorityNames(converter.convert(jwt))).containsExactly("ROLE_READ_ONLY");
    }

    @Test
    @DisplayName("다중 role — 모두 ROLE_ prefix 부여")
    void multiple_roles_mapped() {
        Jwt jwt = buildJwt("12345682", List.of("PLANNER", "IT_OPS"));
        assertThat(authorityNames(converter.convert(jwt)))
            .containsExactlyInAnyOrder("ROLE_PLANNER", "ROLE_IT_OPS");
    }

    @Test
    @DisplayName("알 수 없는 role — 화이트리스트 필터링 (무시)")
    void unknown_role_ignored() {
        Jwt jwt = buildJwt("99999999", List.of("PLANNER", "SUPER_ADMIN", "GUEST"));
        assertThat(authorityNames(converter.convert(jwt))).containsExactly("ROLE_PLANNER");
    }

    @Test
    @DisplayName("realm_access claim 없음 — 빈 권한 집합")
    void no_realm_access_returns_empty() {
        Jwt jwt = new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "RS256"),
            Map.of("preferred_username", "12345683")
        );
        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("realm_access.roles 빈 리스트 — 빈 권한")
    void empty_roles_list() {
        Jwt jwt = buildJwt("12345684", List.of());
        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }

    private List<String> authorityNames(AbstractAuthenticationToken token) {
        return token.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
    }
}
