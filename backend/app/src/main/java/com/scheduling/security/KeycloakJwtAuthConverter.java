package com.scheduling.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keycloak JWT → Spring Security Authentication 변환기 — TK-30-2-1.
 *
 * <p>변환 흐름:
 * <ol>
 *   <li>JWT claim {@code realm_access.roles} (List&lt;String&gt;) 추출</li>
 *   <li>{@link RoleConstants#VALID_ROLES} 화이트리스트 필터 — 알 수 없는 role 무시 (보안)</li>
 *   <li>{@code ROLE_} prefix 부여 → {@link SimpleGrantedAuthority}</li>
 *   <li>principal 명 = JWT {@code preferred_username} (사번 8자리, NFR-SEC-007 v1.5)</li>
 * </ol>
 *
 * @see SecurityConfig
 */
@Component
public class KeycloakJwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String PRINCIPAL_CLAIM = "preferred_username";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalName = jwt.getClaimAsString(PRINCIPAL_CLAIM);
        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return Collections.emptySet();
        }
        Object rolesObj = realmAccess.get(ROLES_CLAIM);
        if (!(rolesObj instanceof Collection<?> roles)) {
            return Collections.emptySet();
        }
        Set<GrantedAuthority> result = new LinkedHashSet<>();
        for (Object role : roles) {
            if (role == null) continue;
            String roleName = role.toString();
            if (RoleConstants.VALID_ROLES.contains(roleName)) {
                result.add(new SimpleGrantedAuthority(RoleConstants.ROLE_PREFIX + roleName));
            }
        }
        return result;
    }
}
