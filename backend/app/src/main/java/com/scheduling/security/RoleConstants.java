package com.scheduling.security;

import java.util.Set;

/**
 * RBAC 4 role 상수 — TK-30-2-1 (REQ-NF-SEC-003).
 *
 * <p>Keycloak realm role 명 (Spring Security 의 {@code ROLE_} prefix 미포함):
 * <ul>
 *   <li>{@link #PLANNER} — 생산계획팀 (스케줄 작성·확정·override, BR-X05 dual-review 대상)</li>
 *   <li>{@link #STK_USER} — 현장 STK 작업자 (시뮬뷰·제안 조회)</li>
 *   <li>{@link #IT_OPS} — IT 운영팀 (마스터·시스템·Actuator·Grafana)</li>
 *   <li>{@link #READ_ONLY} — 감사·임원 (조회 전용)</li>
 * </ul>
 *
 * <p>{@link #VALID_ROLES} 는 whitelist — 무관한 role 은 무시 (보안 차원).
 *
 * @see KeycloakJwtAuthConverter
 */
public final class RoleConstants {

    public static final String PLANNER   = "PLANNER";
    public static final String STK_USER  = "STK_USER";
    public static final String IT_OPS    = "IT_OPS";
    public static final String READ_ONLY = "READ_ONLY";

    /** Spring Security 표준 prefix — @PreAuthorize("hasRole('PLANNER')") 에서 자동 추가. */
    public static final String ROLE_PREFIX = "ROLE_";

    public static final Set<String> VALID_ROLES = Set.of(PLANNER, STK_USER, IT_OPS, READ_ONLY);

    private RoleConstants() {
        // utility class
    }
}
