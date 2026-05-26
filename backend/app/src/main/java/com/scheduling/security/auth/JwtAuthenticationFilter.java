package com.scheduling.security.auth;

import com.scheduling.security.RoleConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Sprint 10 EP-AUTH JWT 인증 필터 (ST-AUTH-3, NFR-SEC-007).
 *
 * <p>{@code Authorization: Bearer <token>} 헤더가 있으면 {@link JwtService#parse} 로 검증 →
 * 성공 시 SecurityContext 에 {@link UsernamePasswordAuthenticationToken} 주입 (principal = 사번,
 * authority = ROLE_{role}).
 *
 * <p>토큰 없거나 검증 실패 시 — SecurityContext 미설정, 다음 필터로 chain 통과
 * (anonymous 또는 EntryPoint 가 401 처리).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Jwt jwt = jwtService.parse(token);
                String employeeId = jwt.getSubject();
                String role = jwt.getClaimAsString(JwtService.ROLE_CLAIM);
                if (employeeId != null && role != null) {
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            employeeId, null,
                            List.of(new SimpleGrantedAuthority(RoleConstants.ROLE_PREFIX + role)));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException e) {
                if (log.isDebugEnabled()) {
                    log.debug("JWT 검증 실패 — {}", e.getMessage());
                }
                // SecurityContext 미설정 → EntryPoint 가 401 처리
            }
        }
        chain.doFilter(request, response);
    }
}
