# Sprint 10 진입 계획 — EP-AUTH (사번+PIN 인증 활성) v1.0

**작성일**: 2026-05-27 | **버전**: 1.0 | **상태**: Sprint 10 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 10 Roadmap](TASK-001_WBS_v1.5.md) + [SRS v1.5 §REQ-NF-SEC-007](../2.SRS/SRS-001_Production_Scheduling_System_v1.5.md) (사번 8자리 + PIN 4자리 + 5회/10분 잠금, 2026-05-19 운영 결정)

---

## 1. 목적

**표준 베타 진입 첫 Sprint** — Sprint 9 까지 적용된 DEV 가벼운 mode (anonymous user + 4 역할 자동 부여) 를 폐지하고, **실제 사번+PIN 인증** 으로 전환. 베타 사용자 5~10명 식별 + audit actor 정확화 + RBAC 기반 마련 (Sprint 11 EP-RBAC 선행 조건).

**활성 후 효과:**
- audit_log.actor 가 'anonymousUser' 가 아닌 실 사번 기록 (BR-X02 완전 충족)
- BR-X05 dual-review (작성자 ≠ 승인자) 의 식별 가능성 마련 (Sprint 16 선행)
- 사내 5명 LAN 접속 시 누가 무엇을 했는지 추적 가능

---

## 2. Sprint 10 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-AUTH-1 DB schema (V036 + entity + Repo) | 0.5 | 0.3 |
| ST-AUTH-2 Spring Security 인증 layer | 1.0 | 0.5 |
| ST-AUTH-3 잠금 정책 (5회/10분) | 1.0 | 0.5 |
| ST-AUTH-4 REST `/api/auth/login` + JWT | 1.5 | 0.8 |
| ST-AUTH-5 Frontend LoginPage + axios 통합 | 1.5 | 0.8 |
| ST-AUTH-6 DEV fallback 제거 + IT 변환 | 0.5 | 0.3 |
| ST-AUTH-7 초기 사용자 시드 (V037) | 0.3 | 0.2 |
| **합계** | **~6 SP** | **~3.4 PD** |

> **WBS v1.5 계획 5 SP 대비 +1 SP** (잠금 정책 + 초기 시드 별도 Story 로 분리하여 추적성 강화).

---

## 3. 의존성 DAG (Story 간)

```
ST-AUTH-1 (DB)
    ↓
ST-AUTH-2 (Security layer) ──┬─→ ST-AUTH-4 (REST + JWT) ──→ ST-AUTH-5 (Frontend) ──┐
                              │                                                       │
                              └─→ ST-AUTH-3 (잠금 정책) ──────────────────────────┐  │
                                                                                    │  │
                                                                                    ▼  ▼
                                                          ST-AUTH-6 (IT 변환) → ST-AUTH-7 (시드) → DoD
```

**병렬 윈도우:**
- **ST-AUTH-3 ↔ ST-AUTH-4 병렬** — 잠금 정책 (handler) 과 JWT REST (controller) 는 서로 다른 layer
- **ST-AUTH-5 ↔ ST-AUTH-6 병렬** — frontend 작업과 backend IT 변환 분리

---

## 4. Story · Task 매트릭스

### ST-AUTH-1 — DB schema (V036) + JPA entity + Repository

| Task | 내용 | SP |
|---|---|:--:|
| TK-AUTH-1-1 | V036__create_app_user.sql — employee_id CHAR(8) PK + pin_hash VARCHAR(60) BCrypt + role VARCHAR(20) CHECK IN (PLANNER, STK_USER, IT_OPS, READ_ONLY) + failed_attempts SMALLINT DEFAULT 0 + locked_until TIMESTAMPTZ + created_at + updated_at | 0.3 |
| TK-AUTH-1-2 | AppUser entity (@Entity, schema=app) + AppUserRepository (JpaRepository) | 0.1 |
| TK-AUTH-1-3 | 단위 test — entity 불변 (employee_id 8 char 검증, role enum) | 0.1 |

### ST-AUTH-2 — Spring Security 인증 layer

| Task | 내용 | SP |
|---|---|:--:|
| TK-AUTH-2-1 | PasswordEncoder bean — BCryptPasswordEncoder(strength=12) | 0.1 |
| TK-AUTH-2-2 | AppUserDetailsService implements UserDetailsService — findByEmployeeId + ROLE_ prefix + AppUserDetails (UserDetails 구현) | 0.3 |
| TK-AUTH-2-3 | SecurityConfig 갱신 — DaoAuthenticationProvider 등록 + AuthenticationManager bean + DEV fallback 영역 분리 (env 분기 유지하되 default 강한 mode) | 0.4 |
| TK-AUTH-2-4 | 단위 test — UserDetailsService 3 cases (정상 / 미존재 / 잠긴 사용자) | 0.2 |

### ST-AUTH-3 — 잠금 정책 (5회 실패 → 10분)

| Task | 내용 | SP |
|---|---|:--:|
| TK-AUTH-3-1 | LoginAttemptService — recordFailure(empId) + recordSuccess(empId) + isLocked(empId) (Transactional) | 0.3 |
| TK-AUTH-3-2 | AppUserDetailsService — locked_until > now 시 LockedException (Spring Security 표준 LockedException) | 0.2 |
| TK-AUTH-3-3 | AuthenticationFailureHandler — LoginAttemptService.recordFailure 호출 + 응답 (401 또는 423) | 0.2 |
| TK-AUTH-3-4 | AuthenticationSuccessHandler — recordSuccess (failed_attempts reset) | 0.1 |
| TK-AUTH-3-5 | IT — 5회 실패 잠금 / 10분 후 해제 (Clock 주입) / 성공 시 카운터 reset | 0.2 |

### ST-AUTH-4 — REST `/api/auth/login` + JWT 발급

| Task | 내용 | SP |
|---|---|:--:|
| TK-AUTH-4-1 | AuthController — POST /api/auth/login (LoginRequest = empId + pin → LoginResponse = token + role + expiresAt) | 0.3 |
| TK-AUTH-4-2 | JwtService — HS256 generate(empId, role, exp 8h) + parse(token) → Claims (jjwt 라이브러리 또는 nimbus-jose-jwt — 의존성 검토) | 0.5 |
| TK-AUTH-4-3 | JwtAuthenticationFilter — Authorization: Bearer 헤더 → JwtService.parse → SecurityContextHolder 주입 | 0.3 |
| TK-AUTH-4-4 | SecurityConfig — JWT filter 등록 (BasicAuthenticationFilter 이전) + 기존 OAuth2 resource server JwtDecoder 와 공존 (Keycloak + 로컬 JWT 양립) | 0.2 |
| TK-AUTH-4-5 | IT — 정상 로그인 200 + 토큰 반환 / 잘못된 PIN 401 / 잠금 423 / 토큰 만료 후 401 / 토큰 없이 보호 endpoint 호출 401 | 0.2 |

### ST-AUTH-5 — Frontend LoginPage + 통합

| Task | 내용 | SP |
|---|---|:--:|
| TK-AUTH-5-1 | LoginPage.tsx — 사번 8자리 input (number-only, maxLength=8) + PIN 4자리 input (password, maxLength=4) + 로그인 버튼 + 에러 alert (잘못된 PIN / 잠금) | 0.4 |
| TK-AUTH-5-2 | useAuthStore (Zustand) — token + user (empId, role) + login(empId, pin) action + logout action (localStorage persist) | 0.3 |
| TK-AUTH-5-3 | axios interceptor (client.ts 갱신) — request: Authorization: Bearer ${token} 자동 부착 / response: 401 시 logout + /login redirect | 0.3 |
| TK-AUTH-5-4 | ProtectedRoute wrapper — useAuthStore.token 없으면 /login 으로 redirect (react-router) | 0.2 |
| TK-AUTH-5-5 | MainLayout 우측 사용자 정보 영역 추가 — `{empId} ({role})` + 로그아웃 버튼 (FCB 패턴 .nav .user 정합) | 0.2 |
| TK-AUTH-5-6 | 단위 test — LoginPage 입력 validation + axios interceptor 401 처리 | 0.1 |

### ST-AUTH-6 — DEV fallback 제거 + IT 변환

| Task | 내용 | SP |
|---|---|:--:|
| TK-AUTH-6-1 | SecurityConfig — env `AUTH_DEV_FALLBACK=true` 시에만 anonymous 허용 (default false). PROD 무조건 강한 인증 | 0.2 |
| TK-AUTH-6-2 | 기존 IT 의 anonymous 의존 변환 — @WithMockUser(roles="PLANNER", username="emp00000001") 패턴 통일. CapacityOverflowApprovalIT 외 ~5 IT | 0.2 |
| TK-AUTH-6-3 | AuditableAspect actor resolution 검증 IT — JWT subject (empId) 가 audit_log.actor 에 정확히 기록 | 0.1 |

### ST-AUTH-7 — 초기 사용자 시드 (V037)

| Task | 내용 | SP |
|---|---|:--:|
| TK-AUTH-7-1 | V037__seed_initial_users.sql — PLANNER 3명 + STK_USER 3명 + IT_OPS 1명 + READ_ONLY 1명 (사번 emp00000001~8, BCrypt PIN 사전 생성). idempotent ON CONFLICT DO NOTHING | 0.2 |
| TK-AUTH-7-2 | 사용자 매뉴얼 1page — 사번 + 초기 PIN 발급표 (베타 5명 분배용, BETA-LAUNCH S19 까지 임시 PIN 사용) | 0.1 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ 사용자가 `http://localhost:5173/login` 진입 → 사번 8자리 + PIN 4자리 → 200 OK + JWT 발급
2. ✅ JWT 발급 후 `/home` 접근 가능 (ProtectedRoute 통과)
3. ✅ 잘못된 PIN 5회 → 423 Locked + `locked_until = now + 10min` DB 기록
4. ✅ 10분 후 재로그인 시 자동 해제 + `failed_attempts = 0` reset
5. ✅ 로그아웃 → localStorage token 삭제 + /login redirect
6. ✅ 로그인 후 capacity-queue 접근 시 — accept/reject 의 `decided_by` = 사번 (anonymousUser 아님)
7. ✅ audit_log.actor = 사번 (BR-X02 완전 충족)

**비기능 DoD:**
1. ✅ ArchUnit 통과 (`Instant.now()` 0건 유지)
2. ✅ Testcontainers IT 전부 GREEN — @WithMockUser 변환 후
3. ✅ BCrypt strength 12 — 로그인 응답 < 500ms (5명 사용자라 무관)
4. ✅ JWT secret 환경변수 `JWT_HMAC_SECRET` (Sprint 10 후 env 등록, default = DEV-only insecure secret)
5. ✅ Smoke 알파 6 페이지 모두 로그인 후 정상 접근

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| JWT secret 노출 (.env commit) | 토큰 위조 가능 | `.gitignore` `.env*` 확인 + DEV secret 별도 / PROD env var 주입 |
| 기존 IT 변환 누락 → build red | CI fail | TK-AUTH-6-2 전 `grep -r anonymousUser` 전수 검색 후 변환 list 작성 |
| Keycloak JWT (OAuth2) ↔ 로컬 JWT 공존 충돌 | 인증 분기 혼선 | SecurityConfig 에 `JwtAuthenticationFilter` 우선 + Keycloak issuer-uri 미설정 시 로컬 JWT 만 활성 |
| BCrypt strength 12 vs PROD CPU 부하 | 응답 지연 | 사용자 ~10명 + 로그인 빈도 낮음 → 무관 (~200ms acceptable) |
| 사용자 사번 변경 (입사·퇴사) 운영 절차 | 운영 누락 | V037 시드는 베타 임시 / Sprint 11 후 IT_OPS UI 통한 사용자 관리 (Sprint 12 EP-MASTER-UI 부속) |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — Backend foundation:
1. TK-AUTH-1-1 (V036)
2. TK-AUTH-1-2 (Entity)
3. TK-AUTH-2-1 (PasswordEncoder)
4. TK-AUTH-2-2 (UserDetailsService)
5. TK-AUTH-1-3 + TK-AUTH-2-4 (단위 test)

**Day 2** — Security + JWT:
6. TK-AUTH-2-3 (SecurityConfig + DaoAuthenticationProvider)
7. TK-AUTH-3-1~5 (잠금 정책 일괄)
8. TK-AUTH-4-2 (JwtService)
9. TK-AUTH-4-3 (JwtAuthenticationFilter)
10. TK-AUTH-4-1 (AuthController)
11. TK-AUTH-4-4 (SecurityConfig JWT filter 등록)
12. TK-AUTH-4-5 (IT 5)

**Day 3** — Frontend + 통합:
13. TK-AUTH-5-1 (LoginPage)
14. TK-AUTH-5-2 (useAuthStore)
15. TK-AUTH-5-3 (axios interceptor)
16. TK-AUTH-5-4 (ProtectedRoute)
17. TK-AUTH-5-5 (MainLayout 사용자 영역)
18. TK-AUTH-5-6 (test)

**Day 4** — Cleanup + 시드:
19. TK-AUTH-6-1 (DEV fallback env 분기)
20. TK-AUTH-6-2 (기존 IT @WithMockUser 변환)
21. TK-AUTH-6-3 (audit actor 검증 IT)
22. TK-AUTH-7-1 (V037 시드)
23. TK-AUTH-7-2 (매뉴얼)
24. **DoD 검증** — 본 PC 로그인 → capacity-queue accept → audit_log.actor = 사번 확인

**총 ~3.4 PD (1인 AI 가속)** — 4 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Migration | `backend/app/src/main/resources/db/migration/V036__create_app_user.sql`, `V037__seed_initial_users.sql` |
| Backend | `backend/common/src/main/java/com/scheduling/common/security/auth/AppUser.java`, `AppUserRepository.java`, `AppUserDetailsService.java`, `LoginAttemptService.java`, `JwtService.java`, `JwtAuthenticationFilter.java`, `AuthController.java` (위치 — common 모듈 또는 신규 `auth` 모듈 — TK-AUTH-2-3 단계에서 결정) |
| Backend SecurityConfig | `backend/app/src/main/java/com/scheduling/app/security/SecurityConfig.java` (갱신) |
| Frontend | `frontend/src/pages/LoginPage.tsx`, `frontend/src/stores/authStore.ts`, `frontend/src/api/client.ts` (interceptor 갱신), `frontend/src/routes/ProtectedRoute.tsx`, `frontend/src/pages/layouts/MainLayout.tsx` (사용자 영역 추가) |
| IT | `backend/app/src/test/java/com/scheduling/integration/AuthLoginIT.java`, `LoginAttemptLockIT.java`, `AuditActorWithJwtIT.java` |
| Docs | `docs/operations/initial-users-table.md` (베타 5명 PIN 발급표) |

---

## 9. Sprint 10 후 다음 단계

**Sprint 11 (EP-RBAC) 진입 조건:**
- ✅ DoD 8/8 충족
- ✅ V037 시드 5명 모두 로그인 검증
- ✅ audit_log.actor 정확화 확인 (BR-X02)

**Sprint 11 첫 작업** — PLAN-SPRINT-11 작성 (페이지별 권한 매트릭스 정의 — 어느 페이지가 어느 role 접근 가능).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-27 | Claude Code | 초안 — EP-AUTH 7 Story / 27 Task / ~6 SP 분해 + 의존성 DAG + DoD 13 + 작업 순서 4 Day |
