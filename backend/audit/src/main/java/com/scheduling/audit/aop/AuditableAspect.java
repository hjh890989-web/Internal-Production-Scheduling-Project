package com.scheduling.audit.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@link Auditable} 메서드 호출 시 Postgres session-local 변수 주입 —
 * TK-11-1-2 (EP-11 ST-11-1, BR-X02).
 *
 * <p>주입 변수:
 * <ul>
 *   <li>{@code audit.actor}  — SecurityContext.authentication.name ({@code "system"} fallback)</li>
 *   <li>{@code audit.reason} — {@code @Auditable("...")} 값</li>
 * </ul>
 *
 * <p>{@code set_config(name, value, is_local=true)} — 현재 transaction 안에서만 유효 (BEGIN/COMMIT 경계 해제).
 */
@Aspect
@Component
@Profile("with-infra")
public class AuditableAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditableAspect.class);

    private final JdbcTemplate jdbc;

    public AuditableAspect(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Around("@annotation(auditable)")
    public Object setAuditContext(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String actor = resolveActor();
        String reason = auditable.value();
        jdbc.queryForObject("SELECT set_config('audit.actor', ?, true)",
            String.class, actor);
        jdbc.queryForObject("SELECT set_config('audit.reason', ?, true)",
            String.class, reason == null ? "" : reason);
        if (log.isDebugEnabled()) {
            log.debug("@Auditable actor={} reason={} method={}",
                actor, reason, pjp.getSignature().toShortString());
        }
        return pjp.proceed();
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "system";
        String name = auth.getName();
        return (name == null || name.isBlank()) ? "system" : name;
    }
}
