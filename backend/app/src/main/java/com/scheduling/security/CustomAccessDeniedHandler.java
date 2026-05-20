package com.scheduling.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 403 Forbidden 처리 — TK-30-2-3.
 *
 * <p>RFC 7807 ProblemDetail + 한국어 메시지 (NFR-USA-003).
 * 현재 사용자의 role 과 부족한 권한을 명시 — 사용자 친화 진단.
 *
 * <p>보안: 다른 사용자의 role 노출 없음 (SecurityContext 의 자기 정보만).
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException e) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth == null ? "anonymous" : auth.getName();
        List<String> currentRoles = extractRoleNames(auth);

        log.warn("403 Access denied: user={} uri={} method={} currentRoles={}",
            username, request.getRequestURI(), request.getMethod(), currentRoles);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "권한이 없습니다. 필요한 역할(role)이 부여되지 않았습니다. IT 운영팀에 문의하세요."
        );
        problem.setTitle("Access Denied");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("currentRoles", currentRoles);
        problem.setProperty("user", username);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private List<String> extractRoleNames(Authentication auth) {
        if (auth == null) return List.of();
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith(RoleConstants.ROLE_PREFIX))
            .map(a -> a.substring(RoleConstants.ROLE_PREFIX.length()))
            .collect(Collectors.toList());
    }
}
