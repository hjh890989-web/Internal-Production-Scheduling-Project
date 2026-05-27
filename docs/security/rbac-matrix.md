# RBAC 매트릭스 (REQ-NF-SEC-003) v1.1

본 문서는 사내 공정 스케줄링 시스템의 RBAC (Role-Based Access Control) 매트릭스.
TK-30-2-1·2 산출. NFR-SEC-003 모든 API endpoint 권한 강제.

**최종 갱신**: 2026-05-27 (Sprint 11 ST-RBAC-1 — Sprint 10 baseline 반영)

---

## 1. 4 Role

| Role | Keycloak role | Spring authority | 대상 사용자 | 주요 권한 |
|---|---|---|---|---|
| **PLANNER** | `PLANNER` | `ROLE_PLANNER` | 생산계획팀 (P1) | 스케줄 작성·확정·override, 마스터 룰 변경 (BR-X05 dual-review 작성자) |
| **STK_USER** | `STK_USER` | `ROLE_STK_USER` | 현장 STK 작업자 (P3·P4) | 시뮬뷰 조회·제안 작성 |
| **IT_OPS** | `IT_OPS` | `ROLE_IT_OPS` | IT 운영팀 (STK-08) | 마스터 데이터·시스템 관리·Actuator·Grafana |
| **READ_ONLY** | `READ_ONLY` | `ROLE_READ_ONLY` | 감사·임원·READ_ONLY | 조회만 |

emergency 계정 (사번 99000001~99000003) — 봉인 봉투 (docs/operations/idp-failover.md).
베타 초기 사용자 8명 (emp00000001~8) — docs/operations/initial-users-table.md (Sprint 10 V037 시드).

---

## 2. API 매트릭스 (Sprint 10 baseline — Sprint 0~10 마감 endpoint 전수)

### 2.1 인증 (Sprint 10 EP-AUTH 신규)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/auth/login` | POST | ✓ | ✓ | ✓ | ✓ | **✓** (로그인 시도) |

### 2.2 수주 통합 (EP-01)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/orders/import` | POST | ✓ | ✗ | ✓ | ✗ | ✗ |
| `/api/v1/orders/import/{id}` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/orders/import/{id}/mapping-result` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/orders/import/{id}/retry` | POST | ✓ | ✗ | ✓ | ✗ | ✗ |

### 2.3 마스터 (TK-01-2-3 + Sprint 6 + Sprint 11)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/master/mapping-rule/{type}` | GET | ✓ | ✗ | ✓ | ✓ | ✗ |
| `/api/v1/master/mapping-rule/{type}` | PUT | ✓ | ✗ | ✓ | ✗ | ✗ |
| `/api/v1/master/compat` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/master/compat/{hoseId}/{slot}` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/master/vc-hose-rule` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/master/vc-hose-rule/{hoseId}` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/master/vc-hose-rule` | POST | ✗ | ✗ | ✓ | ✗ | ✗ |
| `/api/v1/master/vc-hose-rule/{hoseId}` | DELETE | ✗ | ✗ | ✓ | ✗ | ✗ |
| `/api/v1/master/holiday` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/master/holiday` | POST | ✗ | ✗ | ✓ | ✗ | ✗ |
| `/api/v1/master/holiday/{date}` | DELETE | ✗ | ✗ | ✓ | ✗ | ✗ |

### 2.4 성형 스케줄 (PDD-02, EP-04~21 + EP-22·23 Sprint 7~9 carry-over)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/schedule/vc/slots` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/schedule/vc/{id}/confirm` (BR-X01) | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/confirm-batch` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/proposals` | POST | ✓ | ✓ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/proposals/{id}/accept` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/proposals/{id}/reject` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/proposals` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/schedule/vc/capacity-overflow/split` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/capacity-overflow/supplement` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/capacity-overflow/enqueue` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/capacity-overflow/queue/{id}/accept` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/capacity-overflow/queue/{id}/reject` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/vc/capacity-overflow/queue` | GET | ✓ | ✗ | ✓ | ✓ | ✗ |
| `/api/v1/schedule/validate-all` | POST | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/schedule/conflicts` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |

### 2.5 압출 스케줄 (PDD-03, EP-04~12)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/schedule/ex/matrix` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |
| `/api/v1/schedule/ex/{id}/confirm` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/ex/confirm-batch` | POST | ✓ | ✗ | ✗ | ✗ | ✗ |
| `/api/v1/schedule/ex/candidates/ranking` | GET | ✓ | ✓ | ✓ | ✓ | ✗ |

### 2.6 알림 (Sprint 3 EP-03)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/notifications/{id}/ack` | POST | ✓ | ✓ | ✓ | ✓ | ✗ |

### 2.7 Export (EP-12 + EP-EX12)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/export/master` | GET | ✓ | ✗ | ✓ | ✗ | ✗ |
| `/api/v1/export/extrusion-matrix` | GET | ✓ | ✗ | ✓ | ✗ | ✗ |

### 2.8 감사 + KPI (EP-11 + Sprint 6 EP-46)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/audit/snapshot` | GET | ✓ | ✗ | ✓ | ✓ | ✗ |
| `/api/v1/audit/timeline` | GET | ✓ | ✗ | ✓ | ✓ | ✗ |
| `/api/v1/kpi/measurements` | GET | ✓ | ✗ | ✓ | ✓ | ✗ |
| `/api/v1/kpi/measurements/{kpiCode}` | POST | ✗ | ✗ | ✓ | ✗ | ✗ |
| `/api/v1/kpi/definitions` | GET | ✓ | ✗ | ✓ | ✓ | ✗ |

### 2.9 Public / 인프라

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Anonymous |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/actuator/health/**` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |
| `/api/actuator/info` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |
| `/api/actuator/prometheus` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |
| `/api/actuator/**` (그 외) | GET | ✗ | ✗ | **✓** | ✗ | ✗ |
| `/swagger-ui/**`, `/v3/api-docs/**` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |
| `/api/v1/public/**` | GET | ✓ | ✓ | ✓ | ✓ | **✓** |

---

## 3. 향후 endpoint (Sprint 12~19 계획)

| Endpoint | Method | PLANNER | STK_USER | IT_OPS | READ_ONLY | Sprint |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/master/user` (사용자 관리) | GET/POST/PUT | ✗ | ✗ | ✓ | ✗ | S12 |
| `/api/v1/master/product` (47 품번 CRUD) | GET/POST/PUT/DELETE | ✗ | ✗ | ✓ | ✗ | S12 |
| `/api/v1/master/machine` (LP/IC) | GET/POST/PUT | ✗ | ✗ | ✓ | ✗ | S12 |
| `/api/v1/master/setting-group` | GET/POST/PUT | ✗ | ✗ | ✓ | ✗ | S12 |
| `/api/v1/schedule/vc/{id}/override-intraday-lock` (BR-V07) | POST | ✓ | ✗ | ✗ | ✗ | S17 |
| `/api/v1/master/restore` (BR-X04 forensic) | POST | ✗ | ✗ | ✓ | ✗ | S12 |
| `/api/v1/audit/search` | GET | ✓ | ✗ | ✓ | ✓ | (재정렬) |
| `/api/v1/notify/kakao/webhook` (BR-O02 + Sprint 18) | POST | ✗ | ✗ | ✓ | ✗ | S18 |

---

## 4. 구현 강제 메커니즘

### 4.1 Spring Security Method Security
- `@EnableMethodSecurity` (SecurityConfig)
- 모든 controller method `@PreAuthorize("hasAnyRole(...)")` 적용
- 위반 시 → {@link CustomAccessDeniedHandler} → HTTP 403 + ProblemDetail (한국어)
- **Sprint 11 ST-RBAC-2 정합화** — `isAuthenticated()` 잔존 0건 (이전 baseline 에서 7건 → 명시 role 변환)

### 4.2 ArchUnit 강제 (TK-30-2-2)
- `PreAuthorizeArchTest` — `@RestController` 의 모든 public method 는 `@PreAuthorize` 필수
- 위반 시 빌드 FAILED (CI 게이트)

### 4.3 URL-level 보조 보호
- SecurityConfig 의 `authorizeHttpRequests` — Actuator 별 IT_OPS, Swagger UI permitAll 등
- `/api/v1/auth/**` permitAll (양 분기 — Keycloak / DEV anonymous / strict)
- Method-level + URL-level = defense in depth

### 4.4 Frontend 권한 가드 (Sprint 11 ST-RBAC-3 신규)
- `useAuthStore.hasRole(role)` + `hasAnyRole(...roles)` selector
- `<RoleGuard roles={['PLANNER','IT_OPS']}>` wrap — 미충족 시 `/forbidden` redirect
- MainLayout 메뉴 — role 별 가시성 필터 (STK_USER 마스터 disabled, READ_ONLY mutation 메뉴 disabled)

---

## 5. JWT 통합 (Sprint 10 EP-AUTH 갱신)

### 5.1 자체 JWT (DEV/STG 본 PC 베타)
- {@link com.scheduling.security.auth.JwtService} — HS256 + 8h 유효 + secret `app.auth.jwt.secret`
- {@link com.scheduling.security.auth.JwtAuthenticationFilter} — Bearer → SecurityContext (사번 + ROLE_{role})
- subject = 사번 8자리 (NFR-SEC-007 v1.5)

### 5.2 Keycloak JWT (PROD)
- {@link KeycloakJwtAuthConverter} — JWT claim `realm_access.roles` → `GrantedAuthority`
- principal name = JWT `preferred_username` (사번)
- 화이트리스트 — `RoleConstants.VALID_ROLES` (4 role 만 인정)
- 활성 조건 — `spring.security.oauth2.resourceserver.jwt.issuer-uri` 설정 시
- 양 JWT 공존 — JwtAuthenticationFilter (자체) + oauth2-resource-server (Keycloak)

### 5.3 DEV fallback (Sprint 10 ST-AUTH-6)
- env `app.auth.dev-fallback` (default true) — anonymous 4 role 자동 부여 (본 PC 알파)
- false 시 strict mode — 사번+PIN JWT 필수, anonymous 비활성 (베타/PROD)

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
  "user": "00000008"
}
```

### 6.3 423 Locked (Sprint 10 EP-AUTH 신규)
```http
HTTP/1.1 423 Locked
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "로그인 실패",
  "status": 423,
  "detail": "계정 잠금 — 5회 실패 후 10분 잠금 적용"
}
```

---

## 7. 개정 이력

| 버전 | 일자 | 작성자 | 변경 |
|---|---|---|---|
| 1.0 | 2026-05-20 | TK-30-2-2 | 초안 — 4 role 매트릭스 + Sprint 1 baseline endpoint + 향후 endpoint 계획 |
| **1.1** | **2026-05-27** | **Claude Code (Sprint 11 ST-RBAC-1)** | **§2 API 매트릭스 — Sprint 0~10 마감 endpoint 전수 (8 카테고리 · ~32 endpoint). §3 향후 endpoint Sprint 12~19 재정렬. §4.1 isAuthenticated → 명시 role 정합화. §4.4 Frontend 권한 가드 신규. §5 JWT 통합 — 자체 JWT + Keycloak 양립 + DEV fallback env. §6.3 423 Locked 응답 추가 (EP-AUTH 정합)** |
