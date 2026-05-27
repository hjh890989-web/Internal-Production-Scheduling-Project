# Sprint 11 진입 계획 — EP-RBAC (4 페르소나 권한 매트릭스 강화) v1.0

**작성일**: 2026-05-27 | **버전**: 1.0 | **상태**: Sprint 11 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 11 Roadmap](TASK-001_WBS_v1.5.md) + [WBS v1.6 §5 carry-over](TASK-001_WBS_v1.6.md) + [docs/security/rbac-matrix.md](../../docs/security/rbac-matrix.md) v1.0 (Sprint 1 baseline)

---

## 1. 목적

**Sprint 10 EP-AUTH 직후 진입** — 사번+PIN 인증으로 사용자 식별 가능해진 상태에서, **4 페르소나 권한 분리 정합화**:

1. **Backend** — 모든 controller 의 @PreAuthorize 가 rbac-matrix.md 매트릭스와 1:1 정합 (현재 일부 `isAuthenticated()` 만 — 명시 role 필요)
2. **Frontend** — 메뉴 항목 + 라우트별 role 가드 (현재 모든 role 이 모든 메뉴 봄 — STK_USER 는 마스터 안 보여야)
3. **IT 전수 변환** — 기존 anonymous 의존 IT → @WithMockUser (Sprint 10 TK-AUTH-6-2 carry-over 흡수)
4. **Strict mode 검증** — env `APP_AUTH_DEV_FALLBACK=false` 부팅 → 전 IT GREEN

**활성 후 효과**:
- BR-X05 dual-review (작성자 ≠ 승인자) 의 selectable role 정의 (Sprint 16 EP-CONFIRM 선행)
- STK_USER 가 임의로 capacity-overflow accept 등 차단 (403 명확)
- 임원 READ_ONLY 가 마스터 변경 못 함 (403)
- 베타 운영 시 사번별 권한 명확 (audit_log 분석 시 의도 명확)

---

## 2. Sprint 11 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-RBAC-1 rbac-matrix.md v1.1 갱신 (Sprint 10 baseline) | 0.5 | 0.3 |
| ST-RBAC-2 Backend @PreAuthorize 매트릭스 정합 | 1.0 | 0.5 |
| ST-RBAC-3 Frontend RoleGuard + 메뉴 필터 | 1.0 | 0.5 |
| ST-RBAC-4 IT @WithMockUser 전수 변환 (43 IT) | 1.5 | 0.8 |
| ST-RBAC-5 Strict mode 부팅 검증 IT | 0.5 | 0.3 |
| **합계** | **~4.5 SP** | **~2.4 PD** |

> **WBS v1.5 계획 4 SP 대비 +0.5 SP** (Sprint 10 carry-over TK-AUTH-6-2 흡수).

---

## 3. 의존성 DAG

```
ST-RBAC-1 (매트릭스 갱신)
    ↓
ST-RBAC-2 (Backend @PreAuthorize) ──┐
    ↓                                │
ST-RBAC-3 (Frontend RoleGuard)      │
                                     ↓
                          ST-RBAC-4 (IT 변환)
                                     ↓
                          ST-RBAC-5 (Strict mode 검증) → DoD
```

**병렬 윈도우**:
- **ST-RBAC-2 ↔ ST-RBAC-3** — backend/frontend 디커플링 (매트릭스 합의만 정해지면 양쪽 독립 작업)
- **ST-RBAC-4** — 단순 grep + 대량 sed 작업이라 자동화 가능

---

## 4. Story · Task 매트릭스

### ST-RBAC-1 — rbac-matrix.md v1.1 갱신

| Task | 내용 | SP |
|---|---|:--:|
| TK-RBAC-1-1 | rbac-matrix.md v1.1 — Sprint 10 baseline (EP-AUTH + 7 페이지 + audit 복원) endpoint 추가. Sprint 2+ 계획 → Sprint 11~15 으로 재정렬. revision history 추가 | 0.5 |

### ST-RBAC-2 — Backend @PreAuthorize 매트릭스 정합

| Task | 내용 | SP |
|---|---|:--:|
| TK-RBAC-2-1 | `isAuthenticated()` → 명시 role 변환 — ScheduleValidateController + AckController + ConflictController + MasterCompatController + HolidayController GET + HoseRuleController GET (총 ~7 endpoint) | 0.4 |
| TK-RBAC-2-2 | AuthController `/api/v1/auth/login` 명시 — public (permitAll, SecurityConfig 이미) + 누락된 `/api/v1/auth/logout` POST (옵션) 추가 | 0.2 |
| TK-RBAC-2-3 | IT 회귀 — 변경된 endpoint 영향받는 IT 확인 (예: ScheduleValidateIT 가 STK_USER 도 통과해야 → role 매핑 검증) | 0.4 |

### ST-RBAC-3 — Frontend RoleGuard + 메뉴 필터

| Task | 내용 | SP |
|---|---|:--:|
| TK-RBAC-3-1 | `useAuthStore` 에 `hasRole(role)` + `hasAnyRole(...roles)` selector helper 추가 | 0.1 |
| TK-RBAC-3-2 | MainLayout 메뉴 — role 별 가시성 필터 (STK_USER 마스터 숨김, READ_ONLY 모든 mutation 메뉴 숨김 또는 disabled) | 0.4 |
| TK-RBAC-3-3 | RoleGuard 컴포넌트 (`<RoleGuard roles={['PLANNER','IT_OPS']}>` wrap 시 미충족 시 403 페이지) + router 의 일부 페이지 wrap (마스터=IT_OPS only, capacity-queue=PLANNER+IT_OPS+READ_ONLY) | 0.3 |
| TK-RBAC-3-4 | 403 페이지 신설 (`ForbiddenPage.tsx`) — 사용자에게 "권한 없음" 안내 + 홈 링크 | 0.1 |
| TK-RBAC-3-5 | 단위 test — useAuthStore.hasRole + RoleGuard 분기 | 0.1 |

### ST-RBAC-4 — IT @WithMockUser 전수 변환 (Sprint 10 TK-AUTH-6-2 흡수)

| Task | 내용 | SP |
|---|---|:--:|
| TK-RBAC-4-1 | inventory — `grep -r "anonymous\|@PreAuthorize" backend/app/src/test` 로 IT 별 인증 의존 분류 (anonymous vs @WithMockUser 있음) | 0.2 |
| TK-RBAC-4-2 | 자동 변환 시도 — 각 IT 의 class-level `@WithMockUser(roles="PLANNER", username="emp00000001")` 일괄 추가 (단, 이미 method-level @WithMockUser 있는 IT 는 제외) | 0.7 |
| TK-RBAC-4-3 | 회귀 — `./gradlew :app:test` 전체 GREEN 확인. fail IT 는 role 조정 (예: SwapProposal 은 STK_USER 시도 등) | 0.5 |
| TK-RBAC-4-4 | TK-AUTH-6-2 carry-over 마감 표시 — WBS v1.6 §5 cross-reference 갱신 | 0.1 |

### ST-RBAC-5 — Strict mode 부팅 검증 IT

| Task | 내용 | SP |
|---|---|:--:|
| TK-RBAC-5-1 | StrictAuthModeIT — `@SpringBootTest(properties = {"app.auth.dev-fallback=false"})` Testcontainers + 1 case (보호 endpoint 호출 시 토큰 없으면 401, 잘못된 토큰 401) | 0.3 |
| TK-RBAC-5-2 | application.yml 의 profile `prod-auth-strict` 신설 (선택) — env var `APP_AUTH_DEV_FALLBACK=false` + 명시 documentation | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD**:
1. ✅ rbac-matrix.md v1.1 — 현재 backend endpoint 100% 커버 + Sprint 12~15 계획 반영
2. ✅ 모든 controller 의 @PreAuthorize 명시 role (isAuthenticated 잔존 0건)
3. ✅ Frontend 메뉴 — 사번 `00000001` (PLANNER) 로 로그인 시 마스터 disabled / 사번 `00000007` (IT_OPS) 로 로그인 시 마스터 활성 시각 확인
4. ✅ STK_USER (`00000004/0004`) 가 capacity-overflow accept 시도 → 403 + ForbiddenPage 표시
5. ✅ 전 IT GREEN — 43 IT 모두 @WithMockUser 변환 후 회귀 0건
6. ✅ Strict mode (`APP_AUTH_DEV_FALLBACK=false`) 부팅 → StrictAuthModeIT 1 case PASS

**비기능 DoD**:
1. ✅ ArchUnit 통과
2. ✅ TypeScript compile 통과 + frontend unit tests GREEN
3. ✅ Smoke 알파 6 페이지 재검증 (사번 로그인 후) — 모두 정상 로드

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| @WithMockUser 변환 대량 → IT 회귀 fail 다수 | Sprint 11 일정 + 1 Day | TK-RBAC-4-2 자동화 + TK-RBAC-4-3 incremental fix (한 모듈씩 변환 + test 반복) |
| Frontend RoleGuard 가 lazy route 와 충돌 | router 작동 안 함 | RoleGuard 가 Suspense 안에서 작동하도록 구조 검증 (Sprint 10 ProtectedRoute 패턴 동일) |
| 매트릭스 갱신 후 누락 endpoint 발견 | DoD #2 fail | TK-RBAC-1-1 작성 전 `grep -r "@RestController\|@RequestMapping"` 으로 전체 controller 인벤토리 + 매트릭스 1:1 매칭 검증 |
| Strict mode 부팅 시 hidden bean 의존 fail | Sprint 19 BETA-LAUNCH 직전 발견 위험 | ST-RBAC-5 에서 미리 검증 → 발견 시 Sprint 11 carry-over (또는 Sprint 19 까지 보류) |
| 페이지 전환 시 role-by-role 시각 확인 부담 | DoD #3·#4 검증 시간 | curl 또는 자동 E2E (Playwright Sprint 19) — Sprint 11 은 sample 1-2 role 만 시각 검증 |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — 매트릭스 + Backend:
1. TK-RBAC-1-1 (rbac-matrix.md v1.1)
2. TK-RBAC-2-1~3 (Backend @PreAuthorize 정합 + IT 회귀)

**Day 2** — Frontend + 자동 변환:
3. TK-RBAC-3-1~5 (useAuthStore.hasRole + RoleGuard + ForbiddenPage + 메뉴 필터)
4. TK-RBAC-4-1~2 (IT inventory + 일괄 @WithMockUser 추가)

**Day 3** — 회귀 + Strict 검증:
5. TK-RBAC-4-3~4 (IT 회귀 GREEN 보장 + TK-AUTH-6-2 carry-over 마감)
6. TK-RBAC-5-1~2 (StrictAuthModeIT + prod-auth-strict profile)
7. **DoD 검증** — 본 PC 사번 3종 (PLANNER/STK/IT_OPS) 로 로그인 후 메뉴/페이지 visibility 시각 확인

**총 ~2.4 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Docs | `docs/security/rbac-matrix.md` v1.1 |
| Backend | Controller 7-8건 (`@PreAuthorize` 명시 갱신) — ScheduleValidateController, AckController, ConflictController, MasterCompatController, HolidayController, HoseRuleController |
| Frontend | `frontend/src/components/RoleGuard.tsx`, `frontend/src/pages/ForbiddenPage.tsx`, `frontend/src/stores/authStore.ts` (hasRole helper), `frontend/src/pages/layouts/MainLayout.tsx` (메뉴 필터), `frontend/src/router/index.tsx` (RoleGuard wrap) |
| IT | `backend/app/src/test/java/com/scheduling/integration/StrictAuthModeIT.java` (신규) + 기존 ~43 IT class-level @WithMockUser 추가 |
| Profile | `backend/app/src/main/resources/application.yml` (`prod-auth-strict` profile 추가, 선택) |

---

## 9. Sprint 11 후 다음 단계

**Sprint 12 (EP-MASTER-UI) 진입 조건**:
- ✅ DoD 9/9 충족
- ✅ Strict mode 검증 PASS — 베타 운영 직전 인증 baseline 확정
- ✅ 본 PC 사번 3종 시각 검증 — STK_USER 가 마스터 못 봄

**Sprint 12 첫 작업** — PLAN-SPRINT-12 작성 (47 품번 + LP-01~04 + IC + 셋팅그룹 + 합금형 + 회전수 + PRODUCT_PRIORITY + KD_ORDER UI 정의).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-27 | Claude Code | 초안 — EP-RBAC 5 Story / 17 Task / ~4.5 SP 분해 + 의존성 DAG + DoD 9 + 3-Day 작업 순서 + TK-AUTH-6-2 carry-over 흡수 |
