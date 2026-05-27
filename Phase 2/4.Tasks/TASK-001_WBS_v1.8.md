# 작업 분할 구조서 (WBS) v1.8 — Sprint 12 EP-MASTER-UI 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.8 | **작성일**: 2026-05-27
**전판**: [v1.7](TASK-001_WBS_v1.7.md) (Sprint 11 EP-RBAC 마감 Addendum)
**상태**: Addendum — Sprint 12 EP-MASTER-UI (마스터 데이터 입력 UI, IT_OPS 권한) 100% 마감 + DoD 8/12 ✅ + 4 carry-over (본 PC 시각 검증)

> v1.7 (Sprint 11 EP-RBAC 마감, 63 Epic / 305.5 SP 실) 의 §5 carry-over 식별된 **사용자
> PIN 변경 UI (Sprint 10 TK-AUTH-7 carry-over)** 를 Sprint 12 ST-MASTER-2 가 흡수 + 마감.
> 추가로 ST-MASTER-1~5 5 Story 신규 진입 + 마감. **본 문서는 v1.7 변경 델타만 정리**.

---

## 1. v1.7 → v1.8 변경 요지

| 항목 | v1.7 (Sprint 11 EP-RBAC 마감) | v1.8 (Sprint 12 EP-MASTER-UI 마감) |
|---|---|---|
| Epic 총수 | 63 | 63 (변동 없음, EP-MASTER-UI 마감만) |
| SP 실 합 | 305.5 | **310** (+~4.5 실, 계획 5 대비 -0.5 — 장비/셋팅 5 entity Sprint 14 carry-over) |
| Sprint 12 상태 | 계획 5 SP | ✅ **마감** (5 Story / 19 Task / 3 commits / ~1 PD AI 가속) |
| 마스터 UI | 메뉴 disabled (placeholder) | ✅ **5 페이지 활성** (Hub + 사용자 CRUD + 우선순위 CRUD + KD CRUD + 47 품번 조회) |
| Modulith | master 모듈 audit::events 만 | ✅ **master 모듈 audit::aop 추가 의존** (Admin Service @Auditable BR-X02) |
| IT_OPS 운영 | PSQL SQL 수동 (PIN reset / priority 변경 등) | ✅ **UI 변경 즉시 BR-V12/V13 반영** + audit_log.actor=사번 자동 |

---

## 2. Sprint 12 마감 — EP-MASTER-UI 5 Story 회고

### EP-MASTER-UI 전체 (마스터 데이터 입력 UI, IT_OPS 권한)

**Sprint**: **S12** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-12_EP-MASTER-UI_v1.0](PLAN-SPRINT-12_EP-MASTER-UI_v1.0.md) (3-Day) / **SP 실**: ~4.5 / **선행**: EP-RBAC (S11)

| Story | 구현 | Commit |
|---|---|---|
| ST-MASTER-1 — 마스터 Hub + 메뉴 활성 | MasterHubPage.tsx (Ant Card grid 6 카드 — 4 enabled + 2 Sprint 14 placeholder) + MainLayout `/master` disabled 제거 + router `/master` RoleGuard IT_OPS | `eff97b3` |
| ST-MASTER-2 — AppUser CRUD (Sprint 10 TK-AUTH-7 carry-over 흡수) | UserAdminController (list/create/resetPin/unlock/delete, IT_OPS only) + UserAdminService (@Auditable BCryptPasswordEncoder.encode) + UserAdminPage.tsx (Table + 신규/PIN reset Modal + 삭제 Confirm) + userAdminApi.ts + client.ts 204/빈 body 처리 보완 | `eff97b3` |
| ST-MASTER-3 — PRODUCT_PRIORITY CRUD | ProductPriorityAdminService (create/update/delete + @Auditable, 변경 후 capacity-overflow split() next call 시 즉시 반영) + ProductPriorityController (read 4 role / write IT_OPS) + ProductPriorityPage.tsx (Table + Modal + DatePicker) + productPriorityApi.ts | `4f718d1` |
| ST-MASTER-4 — KD_ORDER CRUD | KdOrderAdminService (UUID 자동, create/update/delete + @Auditable, 변경 후 supplement() next call 시 반영) + KdOrderController (read 4 role / write IT_OPS) + KdOrderPage.tsx (Table + Modal + status tag) + kdOrderApi.ts | `4f718d1` |
| ST-MASTER-5 — ProductSpec read (47 품번) | ProductSpecController (list + get, 4 role read, CRUD 는 Sprint 13 OC-FULL) + ProductSpecPage.tsx (Table + Input.Search substring 필터 + BR-V17 규격<7 tag) + productSpecApi.ts | `a46b8ff` |
| 부수 fix — Modulith boundary | master/build.gradle.kts `implementation(project(":audit"))` + master/package-info.java `allowedDependencies = { "common", "audit::events", "audit::aop" }` (@Auditable AOP 사용 위해) | `4f718d1` |
| 부수 fix — Frontend client | client.ts 204/Content-Length=0/빈 text 처리 (DELETE/POST 빈 응답 endpoint 호환) | `eff97b3` |

### Sprint 12 Task 매트릭스

| Task | 소속 Story | SP 실 | Commit |
|---|---|---|---|
| TK-MASTER-1-1 MasterHubPage 6 카드 | ST-MASTER-1 | 0.3 | `eff97b3` |
| TK-MASTER-1-2 MainLayout disabled 제거 | ST-MASTER-1 | 0.1 | `eff97b3` |
| TK-MASTER-1-3 router /master/* 라우트 | ST-MASTER-1 | 0.1 | `eff97b3` + `4f718d1` + `a46b8ff` |
| TK-MASTER-2-1 UserAdminController 5 endpoint | ST-MASTER-2 | 0.4 | `eff97b3` |
| TK-MASTER-2-2 UserAdminService + @Auditable | ST-MASTER-2 | 0.3 | `eff97b3` |
| TK-MASTER-2-3 UserAdminPage + 2 Modal | ST-MASTER-2 | 0.5 | `eff97b3` |
| TK-MASTER-2-4 userAdminApi.ts | ST-MASTER-2 | 0.1 | `eff97b3` |
| TK-MASTER-2-5 UserAdminIT 5 cases | ST-MASTER-2 | 0.2 | `eff97b3` |
| TK-MASTER-3-1 ProductPriorityController | ST-MASTER-3 | 0.3 | `4f718d1` |
| TK-MASTER-3-2 ProductPriorityAdminService | ST-MASTER-3 | 0.2 | `4f718d1` |
| TK-MASTER-3-3 ProductPriorityPage + Modal | ST-MASTER-3 | 0.4 | `4f718d1` |
| TK-MASTER-3-4 productPriorityApi.ts + IT 3 | ST-MASTER-3 | 0.1 | `4f718d1` |
| TK-MASTER-4-1 KdOrderController | ST-MASTER-4 | 0.3 | `4f718d1` |
| TK-MASTER-4-2 KdOrderAdminService | ST-MASTER-4 | 0.2 | `4f718d1` |
| TK-MASTER-4-3 KdOrderPage + Modal | ST-MASTER-4 | 0.4 | `4f718d1` |
| TK-MASTER-4-4 kdOrderApi.ts + IT 3 | ST-MASTER-4 | 0.1 | `4f718d1` |
| TK-MASTER-5-1 ProductSpecController | ST-MASTER-5 | 0.2 | `a46b8ff` |
| TK-MASTER-5-2 ProductSpecPage + 검색 | ST-MASTER-5 | 0.2 | `a46b8ff` |
| TK-MASTER-5-3 productSpecApi + IT 2 | ST-MASTER-5 | 0.1 | `a46b8ff` |
| 부수 — Modulith audit::aop + client 204 처리 | (보충) | 0.3 | `4f718d1` + `eff97b3` |
| **Sprint 12 합계** | | **~4.5 SP** | (계획 5 / AI 가속 ~1 PD 실 — 계획 2.4 PD 의 42%) |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | IT_OPS 로그인 → 마스터 메뉴 → Hub 4 카드 | ⏳ **carry-over** (본 PC 시각) |
| 2 | 사용자 카드 → list + 신규 + reset + unlock | ✅ UserAdminIT 5 / ⏳ 시각 |
| 3 | 우선순위 → list + 수정 → BR-V12 즉시 반영 | ✅ Service @Auditable / ⏳ 시각 |
| 4 | KD → list + 수정 → BR-V13 즉시 반영 | ✅ / ⏳ 시각 |
| 5 | 품번 → 47 list + 검색 | ✅ / ⏳ 시각 |
| 6 | PLANNER → 마스터 메뉴 안 보임 | ✅ (Sprint 11 매트릭스 정합) |
| 7 | STK_USER → /master → /forbidden | ✅ (Sprint 11 RoleGuard) |
| 8 | audit_log.actor = 사번 (IT_OPS 변경) | ✅ UserAdminIT actor=00000007 검증 |
| 비기능 1~4 | ArchUnit / IT GREEN / TypeScript / Smoke | ✅ 13 IT + tsc OK |

**기능 5 ✅ + 본 PC 시각 4 carry-over** (Sprint 13 진입 직전 검증).

---

## 3. v1.7 §5 carry-over → v1.8 갱신

| 항목 | v1.7 carry-over | v1.8 결과 |
|---|---|---|
| 본 PC 시각 검증 (사번 3종 메뉴 + STK 403 + Smoke 6) | High Sprint 11 잔여 | ✅ **사용자 직접 검증 완료 (4 스크린샷)** — DoD #3·#4·#9 100% |
| ~~사용자 PIN 변경 UI~~ | Medium Sprint 12 EP-MASTER-UI 부속 | ✅ **Sprint 12 ST-MASTER-2 마감** (PIN reset + 잠금 해제 + 신규 사용자 추가) |
| ~~DaoAuthenticationProvider deprecation~~ | Low Sprint 19 직전 | Low (변동 없음) |
| ~~audit_log.actor 누락 검출 IT~~ | Low Sprint 18+ 처리 | Low (변동 없음) |

---

## 4. v1.2 § 추가 영향 정리 (v1.7 → v1.8 확장)

| § | v1.7 → v1.8 변경 |
|---|---|
| §9 Deferred Epic | + **EP-MASTER-UI (S12 마감)** — IT_OPS 가 사용자/우선순위/KD/품번 운영 가능, PSQL SQL 수동 우회 |
| §14 SP 합계 | 305.5 → **310** (Sprint 12 +~4.5 실) |
| §16 Phase B 진입 조건 | + **Sprint 12 EP-MASTER-UI 마감 → Sprint 13 EP-OC-FULL 진입 게이트 충족** (마스터 4종 UI 운영 가능) |
| §17 GitHub label | `sprint:S12` 추가 |

---

## 5. carry-over 식별 (Sprint 13+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 본 PC 시각 검증 (DoD #1·#3·#4·#5) | High (Sprint 12 잔여) | IT_OPS 00000007 로그인 → 5 페이지 + Priority 수정 → BR-V12 검증 |
| 장비 (LP/IC) CRUD UI | Medium | Sprint 14 EP-VC-FULL 부속 |
| 셋팅 그룹 / 합금형 / 회전수 CRUD UI | Medium | Sprint 14 EP-VC-FULL 부속 |
| 라인 (Pod) CRUD UI | Low | Sprint 15 EP-EX-FULL 부속 |
| 휴일 캘린더 UI | Low | HolidayController 이미 있음, UI Sprint 13+ 옵션 |
| ProductSpec CRUD UI | Medium | Sprint 13 EP-OC-FULL — underlying VC/EX_CONSTRAINT CRUD 시점 |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 |

---

## 6. 관련 자료

- [TASK-001_WBS_v1.7](TASK-001_WBS_v1.7.md) — Sprint 11 EP-RBAC 마감 Addendum
- [PLAN-SPRINT-12_EP-MASTER-UI_v1.0](PLAN-SPRINT-12_EP-MASTER-UI_v1.0.md) — Sprint 12 진입 plan (5 Story / 19 Task / DoD 12)
- [Sprint 12 commits chain](#) — `eff97b3` (Day 1 Hub + User) → `4f718d1` (Day 2 Priority + KD) → `a46b8ff` (Day 3 ProductSpec)
- [Frontend 마스터 5 페이지](../../frontend/src/pages/master/) — MasterHubPage / UserAdminPage / ProductPriorityPage / KdOrderPage / ProductSpecPage
- [Backend Admin Service 3](../../backend/master/src/main/java/com/scheduling/master/) — priority / kd + UserAdminService (security.auth)

---

## 7. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — Epic·Story·Task 3단계 분해 |
| 1.1 | 2026-05-15 | (작성자) | 산술 오류 정정 + EP-34 보강 |
| 1.2 | 2026-05-15 | (작성자) | 결함 10건 해소 (49 Epic / 285 SP) |
| 1.3 | 2026-05-23 | Claude Code | Sprint 7 carry-over EP-22·23 deferred 활성 마감 Addendum |
| 1.4 | 2026-05-23 | Claude Code | Sprint 8 신규 Epic 2 마감 Addendum |
| 1.5 | 2026-05-27 | Claude Code | Sprint 9 마감 + 표준 베타 Sprint 10~19 신규 plan Addendum (63 Epic / 341 SP) |
| 1.6 | 2026-05-27 | Claude Code | Sprint 10 EP-AUTH 100% 마감 Addendum (DoD 8/8) |
| 1.7 | 2026-05-27 | Claude Code | Sprint 11 EP-RBAC 100% 마감 Addendum (DoD 9/9 시각 검증 완료) |
| 1.8 | 2026-05-27 | Claude Code | **Addendum — Sprint 12 EP-MASTER-UI 100% 마감 (5 Story / 19 Task / 3 commits / ~4.5 SP). 마스터 UI 5 페이지 활성 (Hub + 사용자/우선순위/KD CRUD + 품번 47 조회). master 모듈 audit::aop 의존성 추가. Sprint 10 TK-AUTH-7 carry-over 흡수. DoD 8/12 ✅ + 4 carry-over (본 PC 시각). Sprint 13 EP-OC-FULL 진입 게이트 충족** |
