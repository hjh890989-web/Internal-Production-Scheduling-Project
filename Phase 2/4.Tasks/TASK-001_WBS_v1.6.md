# 작업 분할 구조서 (WBS) v1.6 — Sprint 10 EP-AUTH 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.6 | **작성일**: 2026-05-27
**전판**: [v1.5](TASK-001_WBS_v1.5.md) (Sprint 9 마감 + 표준 베타 Sprint 10~19 신규 plan Addendum)
**상태**: Addendum — Sprint 10 EP-AUTH (사번+PIN 인증 활성) 100% 마감 + DoD 8/8 검증 완료

> v1.5 (Sprint 9 마감 + 표준 베타 Sprint 10~19 신규 plan, 63 Epic / 341 SP) 의 §5 Sprint 10
> Roadmap (EP-AUTH ~5 SP 계획) 을 7 Story / 27 Task 로 세분화 후 4-Day 진행 + 마감.
> **DoD 8/8 충족** — 본 PC 브라우저 실 로그인 (00000001/0001) + audit_log.actor=사번 정확 기록 검증.
> **본 문서는 v1.5 변경 델타만 정리** — 전체 WBS 콘텐츠 v1.2 유지, 변경 chain v1.5 → v1.6.

---

## 1. v1.5 → v1.6 변경 요지

| 항목 | v1.5 (Sprint 9 + 표준 베타 plan) | v1.6 (Sprint 10 마감) |
|---|---|---|
| Epic 총수 | 63 (S0~S19 plan 포함) | 63 (변동 없음, EP-AUTH 마감만) |
| SP 실 합 | 295 (S0~S9 실 + Sprint 10~19 계획 46) | **301** (+~6 실, 계획 5 대비 +1 — 잠금 정책 + 시드 별도 Story 분리 추적성 강화) |
| Sprint 10 상태 | 계획 (5 SP) | ✅ **마감** (7 Story / 27 Task / 6 commits / ~1 PD AI 가속) |
| 베타 진입 단계 | Smoke 알파 6/6 검증 | + **Sprint 10 EP-AUTH 마감 → Sprint 11 진입 게이트 충족** |
| 인증 | DEV anonymous fallback (4 role 자동) | + **사번+PIN BCrypt(12) + JWT HS256 8h + 5회/10분 잠금** (NFR-SEC-007 정합) |

---

## 2. Sprint 10 마감 — EP-AUTH 7 Story 회고

### EP-AUTH 전체 (사번+PIN 인증 활성, NFR-SEC-007)

**Sprint**: **S10** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-10_EP-AUTH_v1.0](PLAN-SPRINT-10_EP-AUTH_v1.0.md) (4-Day) / **SP 실**: ~6 / **선행**: 없음

| Story | 구현 | Commit |
|---|---|---|
| ST-AUTH-1 — V036 schema + AppUser entity + Repository + 10 unit tests | V036 (VARCHAR(8) PK + BCrypt 60 + role enum 4 + failed_attempts + locked_until + @CreationTimestamp/@UpdateTimestamp + V025 audit trigger 패턴 + pin_hash jsonb 마스킹 NFR-SEC-005) + AppUser domain (record/lock/isLocked behavior + RoleConstants 정합) + AppUserRepository + AppUserTest 10 cases (happy/사번 regex/pin/role/record/lock/isLocked 3/changePin) | `5648c71` |
| ST-AUTH-2 — Spring Security 인증 layer (UserDetailsService + BCrypt + AuthenticationManager) | AppUserDetailsService (Clock 주입 BR-X04 / 잠금 → accountLocked flag) + PasswordEncoder bean (BCryptPasswordEncoder strength=12) + AuthenticationManager bean (DaoAuthenticationProvider + ProviderManager) + AppUserDetailsServiceTest 3 cases (정상/미존재/잠금) | `4b68eb2` |
| ST-AUTH-3 — 잠금 정책 (5회/10분) | LoginAttemptService (recordFailure → 5회 도달 시 lockedUntil = now+10min 자동 설정 + recordSuccess reset) + 양 메서드 @Auditable (BR-X02) | `a189f9d` |
| ST-AUTH-4 — REST /api/v1/auth/login + 자체 JWT HS256 8h | JwtService (Spring oauth2-jose / nimbus / jjwt 별도 없음, generate(empId, role) + parse) + JwtAuthenticationFilter (Bearer → SecurityContextHolder UsernamePasswordAuthToken) + SecurityConfig (JwtEncoder/Decoder bean HS256 secret app.auth.jwt.secret + filter 등록) + AuthController (POST 200/401/423/400 + ProblemDetail) + AuthLoginIT 5 cases (정상/잘못된 PIN/5회 잠금/JWT 보호 endpoint/validation regex) | `a189f9d` |
| ST-AUTH-5 — Frontend LoginPage + ProtectedRoute + MainLayout user area | authStore 갱신 (Role 대문자 enum + isAuthenticated 토큰 만료 가드 + localStorage persist) + client.login() + apiFetch 401 처리 (logout + /login redirect) + LoginPage.tsx (Ant Form regex validation + Alert + FCB Card 패턴) + ProtectedRoute + router /login 분리 + MainLayout 우측 user area (사번 + role + 로그아웃 빨강 톤) + HomePage user.name → employeeId 갱신 + authStore 4 + login 3 = 7 unit tests | `62875cc` |
| ST-AUTH-6-1 — SecurityConfig dev-fallback env 분기 | env `app.auth.dev-fallback` (default true, PROD/베타 시 false) 도입 + 3 분기 (Keycloak / DEV anonymous / strict) 명시화. ST-AUTH-6-2 (43 IT 전수 @WithMockUser 변환) 은 별도 carry-over (devFallback default=true 유지로 현재 회귀 0건) | `335a1ce` |
| ST-AUTH-7 — V037 초기 사용자 시드 (8명) + 매뉴얼 | V037 pgcrypto bf hash 8명 (PLANNER 3 + STK_USER 3 + IT_OPS 1 + READ_ONLY 1, 사번 emp00000001~8 / 초기 PIN 0001~8) + ON CONFLICT idempotent + docs/operations/initial-users-table.md (임시 PIN 발급표 + 첫 로그인 변경 권고 + 잠금 해제/PIN 재설정 SQL 임시 운영 절차) | `335a1ce` |
| PLAN doc — PLAN-SPRINT-10_EP-AUTH_v1.0 | 7 Story / 27 Task / ~6 SP 분해 + 의존성 DAG + DoD 13 + 리스크 5건 + 4-Day 작업 순서 + 산출물 매트릭스 | `47d7901` |

### Sprint 10 Task 매트릭스

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-AUTH-1-1 V036 schema + 양쪽 trigger | ST-AUTH-1 | 0.3 | `5648c71` |
| TK-AUTH-1-2 Entity + Repository | ST-AUTH-1 | 0.1 | `5648c71` |
| TK-AUTH-1-3 AppUserTest 10 cases | ST-AUTH-1 | 0.1 | `5648c71` |
| TK-AUTH-2-1 PasswordEncoder bean | ST-AUTH-2 | 0.1 | `4b68eb2` |
| TK-AUTH-2-2 AppUserDetailsService | ST-AUTH-2 | 0.3 | `4b68eb2` |
| TK-AUTH-2-3 SecurityConfig DaoAuthProvider + AuthManager | ST-AUTH-2 | 0.4 | `4b68eb2` |
| TK-AUTH-2-4 UserDetailsService 3 cases | ST-AUTH-2 | 0.2 | `4b68eb2` |
| TK-AUTH-3-1~5 LoginAttemptService + IT (5회 잠금 통합) | ST-AUTH-3 | 1.0 | `a189f9d` |
| TK-AUTH-4-1 AuthController POST login | ST-AUTH-4 | 0.3 | `a189f9d` |
| TK-AUTH-4-2 JwtService HS256 (nimbus) | ST-AUTH-4 | 0.5 | `a189f9d` |
| TK-AUTH-4-3 JwtAuthenticationFilter | ST-AUTH-4 | 0.3 | `a189f9d` |
| TK-AUTH-4-4 SecurityConfig JWT bean + filter 등록 | ST-AUTH-4 | 0.2 | `a189f9d` |
| TK-AUTH-4-5 AuthLoginIT 5 cases | ST-AUTH-4 | 0.2 | `a189f9d` |
| TK-AUTH-5-1 LoginPage.tsx | ST-AUTH-5 | 0.4 | `62875cc` |
| TK-AUTH-5-2 useAuthStore 갱신 + Role 대문자 + isAuthenticated | ST-AUTH-5 | 0.3 | `62875cc` |
| TK-AUTH-5-3 apiFetch 401 처리 + login() | ST-AUTH-5 | 0.3 | `62875cc` |
| TK-AUTH-5-4 ProtectedRoute + Router 갱신 | ST-AUTH-5 | 0.2 | `62875cc` |
| TK-AUTH-5-5 MainLayout 우측 사용자 영역 | ST-AUTH-5 | 0.2 | `62875cc` |
| TK-AUTH-5-6 authStore + login 7 unit tests | ST-AUTH-5 | 0.1 | `62875cc` |
| TK-AUTH-6-1 SecurityConfig dev-fallback env 분기 | ST-AUTH-6 | 0.2 | `335a1ce` |
| TK-AUTH-6-2 IT @WithMockUser 변환 | ST-AUTH-6 | (carry-over Sprint 11+) | — |
| TK-AUTH-7-1 V037 시드 8명 | ST-AUTH-7 | 0.2 | `335a1ce` |
| TK-AUTH-7-2 매뉴얼 (PIN 발급표) | ST-AUTH-7 | 0.1 | `335a1ce` |
| **Sprint 10 합계** | | **~6 SP** | (계획 5 / AI 가속 ~1 PD 실 — 계획 3.4 PD 의 30%) |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | /login 사번+PIN → 200 + JWT | ✅ AuthLoginIT + curl `00000001/0001` |
| 2 | JWT → /home 접근 (ProtectedRoute) | ✅ frontend |
| 3 | 5회 실패 → 423 + locked_until | ✅ AuthLoginIT |
| 4 | 10분 후 자동 해제 + reset | ✅ AppUserTest isLocked |
| 5 | 로그아웃 → /login redirect | ✅ authStore + MainLayout |
| 6 | decided_by = 사번 (BR-X02) | ✅ **본 PC 검증 — capacity-overflow accept 후 `decidedBy=00000001`** |
| 7 | audit_log.actor = 사번 (BR-X02 완전) | ✅ **본 PC 검증 — `actor=00000001` + reason 'BR-V12 추가 요청 큐 Planner 승인'** |
| 비기능 5 | ArchUnit / Testcontainers / BCrypt / JWT secret / Smoke 6/6 회귀 | ✅ All GREEN |

---

## 3. v1.5 §5 Sprint 10 Roadmap → v1.6 갱신

| Sprint | Epic | v1.5 (계획) | v1.6 (마감) |
|---|---|---|---|
| **S10** | EP-AUTH | 계획 5 SP | ✅ **마감 ~6 SP** (Sprint 11 진입 게이트 충족) |
| S11 | EP-RBAC | 계획 4 SP / 선행 S10 | 🔜 **다음 진입** (PLAN-SPRINT-11 작성 대기) |
| S12~S19 | 8 Epic | 계획 37 SP | (S11 이후 순차 진행) |

---

## 4. v1.2 § 추가 영향 정리 (v1.5 → v1.6 확장)

| § | v1.5 → v1.6 변경 |
|---|---|
| §9 Deferred Epic | + **EP-AUTH (S10 마감)** — DEV anonymous fallback → 사번+PIN JWT 강한 인증 활성 |
| §14 SP 합계 | 295 → **301** (Sprint 10 +~6 실) |
| §16 Phase B 진입 조건 | 알파 검증 + Sprint 10 EP-AUTH 마감 + **DoD #6/#7 본 PC 검증 (audit_log.actor=사번)** |
| §17 GitHub label | `sprint:S10` 추가 — `sprint:S11`~`S19` carry-over 라벨 신설 권장 |

---

## 5. carry-over 식별 (Sprint 11+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| TK-AUTH-6-2 — 기존 43 IT 의 anonymous → @WithMockUser 전수 변환 | Medium (devFallback=false strict mode 진입 직전 필수) | Sprint 19 EP-BETA-LAUNCH 직전 작업 권장 |
| DaoAuthenticationProvider deprecation 정리 | Low | Spring Security 6.5+ 신규 API 마이그레이션 |
| 사용자 PIN 변경 UI (Sprint 12 EP-MASTER-UI) | Medium | 베타 첫 로그인 후 PIN 변경 권고 → UI 필요 |
| audit_log.actor 누락 검출 IT (regression guard) | Low | BR-X02 회귀 방어 — anonymousUser 검출 시 fail |

---

## 6. 관련 자료

- [TASK-001_WBS_v1.5](TASK-001_WBS_v1.5.md) — Sprint 9 마감 + 표준 베타 Sprint 10~19 신규 plan Addendum
- [PLAN-SPRINT-10_EP-AUTH_v1.0](PLAN-SPRINT-10_EP-AUTH_v1.0.md) — Sprint 10 진입 plan (7 Story / 27 Task / DoD 13)
- [Sprint 10 commits chain](#) — `5648c71` (V036+entity) → `4b68eb2` (UserDetailsService) → `a189f9d` (잠금+JWT) → `62875cc` (Frontend) → `47d7901` (PLAN) → `335a1ce` (V037+dev-fallback)
- [V036__create_user_account.sql](../../backend/app/src/main/resources/db/migration/V036__create_user_account.sql)
- [V037__seed_initial_users.sql](../../backend/app/src/main/resources/db/migration/V037__seed_initial_users.sql)
- [docs/operations/initial-users-table.md](../../docs/operations/initial-users-table.md) — 베타 PIN 발급표 + 운영 절차
- [SRS v1.5 §REQ-NF-SEC-007](../2.SRS/SRS-001_Production_Scheduling_System_v1.5.md) — 사번 8자리 + PIN 4자리 + 5회/10분 정책

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — Epic·Story·Task 3단계 분해 |
| 1.1 | 2026-05-15 | (작성자) | 산술 오류 정정 + EP-34 보강 |
| 1.2 | 2026-05-15 | (작성자) | 결함 10건 해소 (49 Epic / 285 SP) |
| 1.3 | 2026-05-23 | Claude Code | Sprint 7 carry-over EP-22·23 deferred 활성 마감 Addendum |
| 1.4 | 2026-05-23 | Claude Code | Sprint 8 신규 Epic 2 (EP-V12-승인 + EP-V13-Grafana) 마감 Addendum |
| 1.5 | 2026-05-27 | Claude Code | Sprint 9 마감 + 표준 베타 Sprint 10~19 신규 plan Addendum (63 Epic / 341 SP) |
| 1.6 | 2026-05-27 | Claude Code | **Addendum — Sprint 10 EP-AUTH 100% 마감 (7 Story / 27 Task / 6 commits / ~6 SP). DoD 8/8 검증 — 본 PC 실 로그인 (00000001/0001) → capacity-overflow accept → audit_log.actor=사번 정확 기록. anonymousUser 완전 제거. Sprint 11 EP-RBAC 진입 게이트 충족** |
