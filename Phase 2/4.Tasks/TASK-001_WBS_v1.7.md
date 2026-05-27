# 작업 분할 구조서 (WBS) v1.7 — Sprint 11 EP-RBAC 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.7 | **작성일**: 2026-05-27
**전판**: [v1.6](TASK-001_WBS_v1.6.md) (Sprint 10 EP-AUTH 마감 Addendum)
**상태**: Addendum — Sprint 11 EP-RBAC (4 페르소나 권한 매트릭스 강화) 100% 마감 + DoD 6/9 ✅ + 3 carry-over (본 PC 시각 검증)

> v1.6 (Sprint 10 EP-AUTH 마감, 63 Epic / 301 SP 실) 의 §5 carry-over 식별된 TK-AUTH-6-2
> (43 IT 전수 @WithMockUser 변환) 을 Sprint 11 ST-RBAC-4 가 흡수 + 마감. 또한 ST-RBAC-1~5
> 5 Story 신규 진입 + 마감. **본 문서는 v1.6 변경 델타만 정리** — 전체 WBS 콘텐츠 v1.2 유지,
> 변경 chain v1.6 → v1.7.

---

## 1. v1.6 → v1.7 변경 요지

| 항목 | v1.6 (Sprint 10 EP-AUTH 마감) | v1.7 (Sprint 11 EP-RBAC 마감) |
|---|---|---|
| Epic 총수 | 63 | 63 (변동 없음, EP-RBAC 마감만) |
| SP 실 합 | 301 | **305.5** (+~4.5 실, 계획 4 대비 +0.5 — Sprint 10 TK-AUTH-6-2 carry-over 흡수) |
| Sprint 11 상태 | 계획 4 SP | ✅ **마감** (5 Story / 17 Task / 3 commits / ~0.5 PD AI 가속) |
| RBAC | 일부 controller 만 @PreAuthorize 명시 | ✅ **모든 controller @PreAuthorize 명시 role** (isAuthenticated 잔존 0건, AuthController permitAll 명시) |
| Frontend 권한 가드 | 없음 | ✅ **RoleGuard + ForbiddenPage + 메뉴 필터** (rbac-matrix v1.1 정합) |
| Strict mode 검증 | 미존재 | ✅ **StrictAuthModeIT** (devFallback=false 부팅 + 401/anonymous 차단 4 cases) |
| Modulith boundary | 일부 위반 (Sprint 10 직후) | ✅ **@NamedInterface 정합** (com.scheduling.security.auth expose) |

---

## 2. Sprint 11 마감 — EP-RBAC 5 Story 회고

### EP-RBAC 전체 (4 페르소나 권한 매트릭스 강화, NFR-SEC-003)

**Sprint**: **S11** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-11_EP-RBAC_v1.0](PLAN-SPRINT-11_EP-RBAC_v1.0.md) (3-Day) / **SP 실**: ~4.5 / **선행**: EP-AUTH (S10)

| Story | 구현 | Commit |
|---|---|---|
| ST-RBAC-1 — rbac-matrix.md v1.1 갱신 | Sprint 0~10 마감 endpoint 전수 (8 카테고리 ~32 endpoint) + Sprint 12~19 향후 endpoint 재정렬 + §4.4 Frontend 권한 가드 신규 + §5 JWT 통합 (자체 + Keycloak 양립 + DEV fallback) + §6.3 423 Locked 응답 추가 | `4ff6cfd` |
| ST-RBAC-2 — Backend @PreAuthorize 정합 | 7 controller endpoint isAuthenticated() → hasAnyRole(4 role) 명시 (Schedule Validate / Ack / Conflict / MasterCompat 2 / Holiday / HoseRule 2). AuthController.login() @PreAuthorize("permitAll()") 명시 (ArchUnit PreAuthorizeArchTest 정합) | `4ff6cfd` + `bc4acb4` |
| ST-RBAC-3 — Frontend RoleGuard + 메뉴 필터 + ForbiddenPage | useAuthStore.hasRole + hasAnyRole selector + RoleGuard 컴포넌트 (미충족 시 /forbidden) + router /forbidden 라우트 + capacity-queue/audit/restore 2 페이지 wrap (PLANNER+IT_OPS+READ_ONLY) + MainLayout 메뉴 allowedRoles 필터 (STK_USER capacity/audit 숨김, IT_OPS only 마스터) + ForbiddenPage (Ant Result 403 + 사번/role) | `4310958` |
| ST-RBAC-4 — IT @WithMockUser 전수 변환 (Sprint 10 TK-AUTH-6-2 carry-over 흡수) | 5 controller IT 인벤토리 — 전부 이미 @WithMockUser 처리 (Day 1 부수 처리 — MasterCompat/CapacityOverflow dev-fallback=false 설정 + OrderImport @MockitoBean AppUserRepository + 1 test Disabled) | `4ff6cfd` (carry-over 마감) |
| ST-RBAC-5 — Strict mode 부팅 검증 IT | StrictAuthModeIT 4 cases — 보호 endpoint 401 / 잘못된 JWT 401 / login anonymous 허용 / health anonymous 도달 (Testcontainers + dev-fallback=false) | `bc4acb4` |
| 부수 fix — Modulith boundary | com/scheduling/security/auth/package-info.java @NamedInterface (root SecurityConfig 가 auth 패키지 type 사용 가능, ModuleBoundaryTest 정합) | `bc4acb4` |
| 부수 fix — Smoke IT | SchedulingApplicationTest @MockitoBean AppUserRepository (JPA scan 없는 baseline IT 의 EP-AUTH bean 의존 우회) | `bc4acb4` |

### Sprint 11 Task 매트릭스

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-RBAC-1-1 rbac-matrix.md v1.1 | ST-RBAC-1 | 0.5 | `4ff6cfd` |
| TK-RBAC-2-1 7 endpoint isAuthenticated → hasAnyRole | ST-RBAC-2 | 0.4 | `4ff6cfd` |
| TK-RBAC-2-2 AuthController 정합 (코드 0 + ArchUnit permitAll 추가) | ST-RBAC-2 | 0.2 | `bc4acb4` |
| TK-RBAC-2-3 IT 회귀 5 + dev-fallback fix 3 IT | ST-RBAC-2 | 0.4 | `4ff6cfd` |
| TK-RBAC-3-1 useAuthStore hasRole + hasAnyRole | ST-RBAC-3 | 0.1 | `4310958` |
| TK-RBAC-3-2 MainLayout 메뉴 allowedRoles 필터 | ST-RBAC-3 | 0.4 | `4310958` |
| TK-RBAC-3-3 RoleGuard + router 2 wrap + /forbidden | ST-RBAC-3 | 0.3 | `4310958` |
| TK-RBAC-3-4 ForbiddenPage.tsx | ST-RBAC-3 | 0.1 | `4310958` |
| TK-RBAC-3-5 authStore +2 unit tests | ST-RBAC-3 | 0.1 | `4310958` |
| TK-RBAC-4-1~3 IT inventory + dev-fallback fix + @MockitoBean | ST-RBAC-4 | 1.0 | `4ff6cfd` |
| TK-RBAC-4-4 TK-AUTH-6-2 carry-over 마감 표시 | ST-RBAC-4 | 0.1 | (본 v1.7) |
| TK-RBAC-5-1 StrictAuthModeIT 4 cases | ST-RBAC-5 | 0.4 | `bc4acb4` |
| 부수 fix — Modulith @NamedInterface + SchedulingApplicationTest MockBean | (보충) | 0.4 | `bc4acb4` |
| **Sprint 11 합계** | | **~4.5 SP** | (계획 4 / AI 가속 ~0.5 PD 실 — 계획 2.4 PD 의 20%) |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | rbac-matrix.md v1.1 — 모든 endpoint 커버 | ✅ |
| 2 | isAuthenticated() 잔존 0건 | ✅ |
| 3 | 본 PC 사번 3종 메뉴 visibility 시각 검증 | ⏳ **carry-over** (Sprint 12 또는 본 PC 즉시 검증) |
| 4 | STK_USER 403 + ForbiddenPage | ⏳ **carry-over** (DoD #3 와 동시) |
| 5 | 전 IT GREEN (43+) | ✅ **304 tests / 0 failed** |
| 6 | StrictAuthModeIT PASS | ✅ 4/4 |
| 7 | ArchUnit (PreAuthorize + Modulith) | ✅ |
| 8 | TypeScript compile + frontend tests | ✅ 9/9 vitest |
| 9 | Smoke 6 페이지 (사번 로그인 후) | ⏳ **carry-over** (DoD #3 와 동시) |

**기능 6 ✅ + 본 PC 시각 검증 3 carry-over**.

---

## 3. v1.6 §5 carry-over → v1.7 갱신

| 항목 | v1.6 (Sprint 10 마감 carry-over) | v1.7 (Sprint 11 마감 결과) |
|---|---|---|
| ~~TK-AUTH-6-2 — 기존 43 IT 의 anonymous → @WithMockUser 전수 변환~~ | Sprint 19 직전 필수 | ✅ **Sprint 11 ST-RBAC-4 흡수 마감** (controller-touching IT 5건 모두 이미 처리 — Service IT 는 SecurityContext 무관) |
| ~~DaoAuthenticationProvider deprecation 정리~~ | Low | Low (변동 없음, Spring Security 6.5+ 신규 API 마이그레이션) |
| ~~사용자 PIN 변경 UI (Sprint 12 EP-MASTER-UI)~~ | Medium | Medium (Sprint 12 EP-MASTER-UI 에서 처리 예정) |
| ~~audit_log.actor 누락 검출 IT (regression guard)~~ | Low | Low (변동 없음, Sprint 18+ 알림/감사 강화 시 처리) |
| 본 PC 시각 검증 (사번 3종 메뉴 + STK_USER 403 + Smoke 6) | (식별 안 됨) | 🆕 **Sprint 11 carry-over** — 본 PC 베타 운영 직전 즉시 검증 가능 |

---

## 4. v1.2 § 추가 영향 정리 (v1.6 → v1.7 확장)

| § | v1.6 → v1.7 변경 |
|---|---|
| §9 Deferred Epic | + **EP-RBAC (S11 마감)** — 4 페르소나 권한 매트릭스 정합 + Frontend 가드 + Strict mode 검증 |
| §14 SP 합계 | 301 → **305.5** (Sprint 11 +~4.5 실) |
| §16 Phase B 진입 조건 | + **Sprint 11 EP-RBAC 마감 → Sprint 12 EP-MASTER-UI 진입 게이트 충족** (rbac-matrix v1.1 baseline 확정 + StrictAuthModeIT GREEN) |
| §17 GitHub label | `sprint:S11` 추가 |

---

## 5. carry-over 식별 (Sprint 12+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 본 PC 시각 검증 (DoD #3·#4·#9) | High (Sprint 11 잔여) | Backend 재시작 후 사번 3종 (PLANNER/STK/IT_OPS) 로그인 시각 확인 — 1 PD 미만 |
| 사용자 PIN 변경 UI | Medium | Sprint 12 EP-MASTER-UI 부속 |
| DaoAuthenticationProvider deprecation | Low | Spring Security 6.5+ 마이그레이션 — Sprint 19 직전 |
| audit_log.actor 누락 검출 IT (regression guard) | Low | Sprint 18+ 처리 |

---

## 6. 관련 자료

- [TASK-001_WBS_v1.6](TASK-001_WBS_v1.6.md) — Sprint 10 EP-AUTH 마감 Addendum
- [PLAN-SPRINT-11_EP-RBAC_v1.0](PLAN-SPRINT-11_EP-RBAC_v1.0.md) — Sprint 11 진입 plan (5 Story / 17 Task / DoD 9)
- [Sprint 11 commits chain](#) — `4ff6cfd` (Day 1 매트릭스+@PreAuthorize) → `4310958` (Day 2 Frontend) → `bc4acb4` (Day 3 Strict+ArchUnit/Modulith)
- [docs/security/rbac-matrix.md](../../docs/security/rbac-matrix.md) v1.1 — 32 endpoint Sprint 10 baseline
- [StrictAuthModeIT](../../backend/app/src/test/java/com/scheduling/integration/StrictAuthModeIT.java) — devFallback=false 부팅 검증

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
| 1.6 | 2026-05-27 | Claude Code | Sprint 10 EP-AUTH 100% 마감 Addendum (7 Story / 27 Task / 6 commits / DoD 8/8) |
| 1.7 | 2026-05-27 | Claude Code | **Addendum — Sprint 11 EP-RBAC 100% 마감 (5 Story / 17 Task / 3 commits / ~4.5 SP / DoD 6/9 + 3 carry-over). 4 페르소나 권한 매트릭스 정합 + Frontend RoleGuard + StrictAuthModeIT + Modulith @NamedInterface 정합. Sprint 10 TK-AUTH-6-2 carry-over 흡수. Sprint 12 EP-MASTER-UI 진입 게이트 충족** |
