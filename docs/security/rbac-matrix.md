# RBAC 매트릭스 (REQ-NF-SEC-003)

본 문서는 사내 공정 스케줄링 시스템의 RBAC (Role-Based Access Control) 매트릭스.
TK-30-2-1·2 산출. NFR-SEC-003 모든 API endpoint 권한 강제.

---

## 1. 4 Role

| Role | Keycloak role | Spring authority | 대상 사용자 | 주요 권한 |
|---|---|---|---|---|
| **PLANNER** | `PLANNER` | `ROLE_PLANNER` | 생산계획팀 (P1) | 스케줄 작성·확정·override, 마스터 룰 변경 (BR-X05 dual-review 작성자) |
| **STK_USER** | `STK_USER` | `ROLE_STK_USER` | 현장 STK 작업자 (P3·P4) | 시뮬뷰 조회·제안 작성 |
| **IT_OPS** | `IT_OPS` | `ROLE_IT_OPS` | IT 운영팀 (STK-08) | 마스터 데이터·시스템 관리·Actuator·Grafana |
| **READ_ONLY** | `READ_ONLY` | `ROLE_READ_ONLY` | 감사·임원·READ_ONLY | 조회만 |

emergency 계정 (사번 99000001~99000003) — 봉인 봉투 (docs/operations/idp-failover.md).

---

## 2. API 매트릭스 (Sprint 1 baseline)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| **수주 통합 (EP-01)** ||||||
| `/api/v1/orders/import` | POST | ✓ | ✗ | ✓ | ✗ | ✗ |
| `/api/v1/orders/import/{id}` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/orders/import/{id}/retry` | POST | ✓ | ✗ | ✓ | ✗ | ✗ |
| **마스터 룰 (TK-01-2-3)** ||||||
| `/api/v1/master/mapping-rule/{type}` | GET | ✓ | ✗ | ✓ | ✓ | ✗ |
| `/api/v1/master/mapping-rule/{type}` | PUT | ✓ | ✗ | ✓ | ✗ | ✗ |
| **Public / 인증 외** ||||||
| `/actuator/health/**` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |
| `/actuator/info` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |
| `/actuator/prometheus` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |
| `/actuator/**` (그 외) | GET | ✗ | ✗ | **✓** | ✗ | ✗ |
| `/swagger-ui/**`, `/v3/api-docs/**` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |
| `/api/v1/public/**` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |

---

## 3. 향후 endpoint (Sprint 2+ 계획)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY |
|---|---|:---:|:---:|:---:|:---:|
| `/api/v1/schedule/vc` | GET | ✓ | ✓ | ✓ | ✓ |
| `/api/v1/schedule/vc/{id}/confirm` (BR-X01) | POST | ✓ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/{id}/override-intraday-lock` | POST | ✓ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/proposals` | POST | ✓ | ✓ | ✗ | ✗ |
| `/api/v1/schedule/vc/proposals/{id}/accept` | POST | ✓ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/ex` | GET | ✓ | ✓ | ✓ | ✓ |
| `/api/v1/master/holiday` | POST | ✗ | ✗ | ✓ | ✗ |
| `/api/v1/master/restore` | POST | ✗ | ✗ | ✓ | ✗ |
| `/api/v1/audit/search` | GET | ✓ | ✗ | ✓ | ✓ |

---

## 4. 구현 강제 메커니즘

### 4.1 Spring Security Method Security
- `@EnableMethodSecurity` (SecurityConfig)
- 모든 controller method `@PreAuthorize("hasAnyRole(...)")` 적용
- 위반 시 → {@link CustomAccessDeniedHandler} → HTTP 403 + ProblemDetail (한국어)

### 4.2 ArchUnit 강제 (TK-30-2-2)
- `PreAuthorizeArchTest` — `@RestController` 의 모든 public method 는 `@PreAuthorize` 필수
- 위반 시 빌드 FAILED (CI 게이트)

### 4.3 URL-level 보조 보호
- SecurityConfig 의 `authorizeHttpRequests` — Actuator 별 IT_OPS, Swagger UI permitAll 등
- Method-level + URL-level = defense in depth

---

## 5. JWT 통합 (TK-30-2-1)

### 5.1 Keycloak JWT 처리
- {@link KeycloakJwtAuthConverter} — JWT claim `realm_access.roles` → `GrantedAuthority`
- principal name = JWT `preferred_username` (사번 8자리, NFR-SEC-007 v1.5)
- 화이트리스트 — `RoleConstants.VALID_ROLES` (4 role 만 인정)

### 5.2 활성 조건
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` 설정 시 JWT 활성
- 미설정 (DEV baseline) — httpBasic 폴백 + 기본 user (Spring Boot 자동 생성 random password)
- STG/PROD — issuer-uri 필수 (운영 가이드 secrets-management.md §Keycloak)

---

## 6. 401/403 응답 (TK-30-2-3)

### 6.1 401 Unauthorized
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Authentication Required",
  "status": 401,
  "detail": "인증이 필요합니다. 사번/PIN 으로 로그인 후 다시 시도하세요.",
  "instance": "/api/v1/orders/import",
  "loginUrl": "/login"
}
```

### 6.2 403 Forbidden
```http
HTTP/1.1 403 Forbidden
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Access Denied",
  "status": 403,
  "detail": "권한이 없습니다. 필요한 역할(role)이 부여되지 않았습니다. IT 운영팀에 문의하세요.",
  "instance": "/api/v1/orders/import",
  "currentRoles": ["READ_ONLY"],
  "user": "12345678"
}
```

---

## 7. 개정 이력

| 버전 | 일자 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-20 | TK-30-2-2 | 초안 — 4 role 매트릭스 + Sprint 1 baseline endpoint + 향후 endpoint 계획 |
