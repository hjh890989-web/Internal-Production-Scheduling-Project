# Sprint 21 진입 계획 — EP-CRUD-MASTER-2 (5 entity CRUD UI 완성) v1.0

**작성일**: 2026-05-28 | **버전**: 1.0 | **상태**: Phase 4 두 번째 sprint 진입 권고안 (S20 와 병렬 진행 가능)

> **참조**: [PHASE-4_STABILIZATION_v1.0 §3 S21](PHASE-4_STABILIZATION_v1.0.md) + [WBS v1.15 §6 carry-over](TASK-001_WBS_v1.15.md) + [PLAN-SPRINT-12_EP-MASTER-UI_v1.0](PLAN-SPRINT-12_EP-MASTER-UI_v1.0.md) (Sprint 12 baseline)

---

## 1. 목적

**Sprint 12 EP-MASTER-UI 의 3 entity 완성 (UserAdmin + ProductPriority + KdOrder) 위에 나머지 2 disabled entity + 2 신규 entity CRUD UI 추가 — 마스터 데이터 전체 IT_OPS 자체 운영 가능.**

| Entity | Sprint 12 ~ 20 상태 | Sprint 21 활성 |
|---|---|---|
| user_account | ✅ UserAdminPage 완성 | (유지) |
| product_priority | ✅ ProductPriorityPage 완성 | (유지) |
| kd_order | ✅ KdOrderPage 완성 | (유지) |
| product (spec) | ✅ ProductSpecPage 완성 (read) | (유지) — Sprint 22+ write 검토 |
| **vc_machine** | ❌ MasterHubPage disabled | ✅ **VcMachineAdminPage** (LP-01~04 + IC-01 CRUD + machine_type / total_slots / day/night_rotations / active toggle) |
| **setting_group** | ❌ MasterHubPage disabled | ✅ **SettingGroupAdminPage** (setting_group 1~8 CRUD + active toggle) |
| **vc_constraint** (composite_count = 합금형) | ✅ Sprint 4 read only | ✅ **VcConstraintAdminPage** — composite_count + slot 가용성 + mold_qty CRUD (BR-V14 합금형 1·2·3·6) |
| **line** (line_type + line_product_compat) | ❌ UI 미존재 | ✅ **LineAdminPage** — line_type CRUD + product 호환 매핑 |
| **holiday** | ❌ UI 미존재 (V006 seed 만) | ✅ **HolidayAdminPage** — master.holiday CRUD (연도별 갱신, BR-X04 KST 정합) |

**Pre-Phase 의존 (S20 와 병렬, 사내 IT 협의 무관):**
- S20 (Slack/Kakao webhook) 의존 없음 — 본 sprint 는 독립 병행 가능

**활성 후 효과:**
- IT_OPS 가 마스터 데이터 5 entity 전체 자체 갱신 (개발팀 개입 0)
- 신년 holiday 추가, 가류기 교체 (LP-05 신규 도입), 신규 합금형 도입 등 운영 변경 즉시 반영
- ArchUnit Modulith 경계 정합 — 모든 페이지 master.api NamedInterface 만 의존

---

## 2. Sprint 21 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-CRUD-1 VcMachineAdminPage (LP/IC + active toggle) | 1.5 | 0.7 |
| ST-CRUD-2 SettingGroupAdminPage (1~8) | 1.0 | 0.5 |
| ST-CRUD-3 VcConstraintAdminPage (composite_count + slot 가용성 + mold_qty, BR-V14 합금형) | 1.0 | 0.5 |
| ST-CRUD-4 LineAdminPage (line_type + product 호환) | 1.0 | 0.5 |
| ST-CRUD-5 HolidayAdminPage (연도별 갱신) | 0.5 | 0.3 |
| ST-CRUD-6 MasterHubPage 5 카드 활성 + IT 회귀 | 0.5 | 0.2 |
| **합계** | **~5 SP** | **~2.7 PD** |

> **PHASE-4 §3 S21 계획 5 SP 정합** ("alloyMold" 는 별도 entity 가 아닌 vc_constraint.composite_count 속성 — VcConstraintAdminPage 로 통합).

---

## 3. 의존성 DAG

```
S20 EP-EXT-WEBHOOK (병렬, 의존 없음)

Sprint 21 (독립 병렬 시작 가능)
  ST-CRUD-1 (VcMachine) ──┐
  ST-CRUD-2 (Setting) ────┤
  ST-CRUD-3 (Constraint) ─┤  (각 entity 페이지 독립)
  ST-CRUD-4 (Line) ───────┤
  ST-CRUD-5 (Holiday) ────┘
                          ↓
              ST-CRUD-6 (MasterHubPage 활성 + 회귀)
```

**병렬 윈도우:** 5 entity 페이지 전부 독립 — Day 1~2 동시 작업 가능.

---

## 4. Story · Task 매트릭스

### ST-CRUD-1 — VcMachineAdminPage (1.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-CRUD-1-1 | Backend `VcMachineAdminController` — POST/PUT/DELETE `/api/v1/master/vc-machines/{id}` (IT_OPS only RBAC) + @Auditable | 0.4 |
| TK-CRUD-1-2 | Backend IT — RBAC (STK_USER 403) + PUT total_slots 변경 → audit_log row 영속 + 비활성 LP-04 시 vc_schedule 의존 row 차단 검증 | 0.3 |
| TK-CRUD-1-3 | Frontend VcMachineAdminPage — Ant Design Table (5 row) + Edit drawer (machine_type / total_slots / day_rotations / night_rotations / active) + 저장 시 invalidateQueries | 0.5 |
| TK-CRUD-1-4 | Frontend test — vitest 4 cases (READ / EDIT 저장 / 비활성 toggle / 권한 없음 403 안내) | 0.3 |

### ST-CRUD-2 — SettingGroupAdminPage (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-CRUD-2-1 | Backend `SettingGroupAdminController` — POST/PUT/DELETE `/api/v1/master/setting-groups/{id}` (IT_OPS only) | 0.3 |
| TK-CRUD-2-2 | Backend IT — setting_group 1~8 범위 강제 + BR-V12/V13 cross-reference (product_setting_group 연결 row 검증) | 0.2 |
| TK-CRUD-2-3 | Frontend SettingGroupAdminPage — Table + Drawer (setting_group_id / display_name / active) | 0.3 |
| TK-CRUD-2-4 | Frontend vitest 3 cases (READ / EDIT / active toggle) | 0.2 |

### ST-CRUD-3 — VcConstraintAdminPage (1.0 SP, 합금형 통합)

| Task | 내용 | SP |
|---|---|:--:|
| TK-CRUD-3-1 | Backend `VcConstraintAdminController` — POST/PUT `/api/v1/master/vc-constraints/{hoseId}` (IT_OPS only). 기존 read controller 위에 write 추가 | 0.3 |
| TK-CRUD-3-2 | Backend IT — composite_count 1·2·3·6 강제 (BR-V14) + mold_qty ≥ 0 + slot 7 가용성 boolean 7 컬럼 | 0.3 |
| TK-CRUD-3-3 | Frontend VcConstraintAdminPage — Table (47 hose) + Drawer (composite_count + lp/ic mold_qty + 7 slot eligibility checkboxes) | 0.3 |
| TK-CRUD-3-4 | Frontend vitest 2 cases (composite_count 4 입력 → 거부 / 정상 1·2·3·6 저장) | 0.1 |

### ST-CRUD-4 — LineAdminPage (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-CRUD-4-1 | Backend `LineAdminController` — line_type + line_product_compatibility CRUD (IT_OPS only) | 0.3 |
| TK-CRUD-4-2 | Backend IT — 신규 line 추가 + product 호환 매핑 + 비활성 시 다중 schedule 의존 차단 | 0.2 |
| TK-CRUD-4-3 | Frontend LineAdminPage — Table (line_type) + 호환 product 다중 선택 (Transfer 컴포넌트) | 0.3 |
| TK-CRUD-4-4 | Frontend vitest 2 cases | 0.2 |

### ST-CRUD-5 — HolidayAdminPage (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-CRUD-5-1 | Backend `HolidayAdminController` — POST/DELETE `/api/v1/master/holidays/{date}` (IT_OPS only) — 기존 HolidayController 위에 write | 0.2 |
| TK-CRUD-5-2 | Backend IT — 신규 holiday 추가 → WorkingCalendarService cache invalidate + 다음 build CapacityLedger 영업일 제외 검증 | 0.2 |
| TK-CRUD-5-3 | Frontend HolidayAdminPage — Calendar 컴포넌트 + 클릭 시 holiday 추가/삭제 (연도별 navigation) | 0.1 |

### ST-CRUD-6 — MasterHubPage 5 카드 활성 + 회귀 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-CRUD-6-1 | MasterHubPage — 5 entity 카드 disabled 해제 + nav 정합 (Sprint 12 패턴) | 0.2 |
| TK-CRUD-6-2 | 사용자 매뉴얼 v1.2 §3 IT_OPS — 5 entity 운영 절차 추가 (vc_machine 교체, holiday 신년 갱신 등) | 0.2 |
| TK-CRUD-6-3 | 회귀 IT — Sprint 19/20 백엔드 IT GREEN + 신규 5 controller IT 통합 | 0.1 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ VcMachineAdminPage — LP/IC 5대 CRUD + 비활성 toggle (IT_OPS 만)
2. ✅ SettingGroupAdminPage — 1~8 CRUD
3. ✅ VcConstraintAdminPage — 47 hose composite_count + slot 가용성 CRUD (BR-V14)
4. ✅ LineAdminPage — line_type + product 호환 매핑 CRUD
5. ✅ HolidayAdminPage — Calendar UI + holiday 추가/삭제 + WorkingCalendar invalidate
6. ✅ MasterHubPage 5 카드 모두 활성
7. ✅ RBAC IT_OPS only — 다른 role 403
8. ✅ 사용자 매뉴얼 v1.2 §3 IT_OPS 5 entity 운영 절차

**비기능 DoD:**
1. ✅ ArchUnit GREEN — Modulith 경계 (master.api NamedInterface 만 외부 의존)
2. ✅ Backend IT 신규 5+ controller + 회귀 303+ GREEN
3. ✅ TypeScript compile + vitest 신규 11+ cases + 기존 82 GREEN
4. ✅ @Auditable 5 entity 모두 적용 — IT_OPS 모든 mutation audit_log 영속

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| vc_machine 비활성 시 schedule 의존 row 다수 차단 (LP-04 비활성 → 기존 schedule 800+ row) | 운영 차단 위험 | active=false 가 신규 INSERT 차단만 — 기존 row 보존 (vc_schedule.machine_id FK 유지). Sprint 22+ partition/archive 정책 별도 |
| holiday 추가 후 기존 schedule 의존 row 충돌 (영업일 → 휴일 전환) | 일부 schedule 영업일 위반 | HolidayAdminPage 안 영향 row 미리 표시 + confirm 모달 |
| 47 hose × composite_count UI scope 누적 (Table 페이지네이션) | 화면 로딩 느림 | Ant Design Table virtualScroll (200+ row 정상) + 페이지 20 / 검색 필터 |
| LineAdminPage Transfer 컴포넌트 복잡도 (다중 product 호환) | 작업 지연 | 1차 baseline 은 단순 multi-select Dropdown + Phase 5+ Drag-Drop 개선 |
| 5 entity 동시 작업 — Modulith 경계 위반 가능성 | ArchUnit fail | 각 page 가 master.api 만 의존하도록 controller 라우팅 + 회귀 IT |
| HolidayAdminPage Calendar 화면 width 부족 (사이드바 정합) | UX 미흡 | Drawer 안 Calendar + 월별 navigation + 연도 selector |

---

## 7. 작업 순서 추천

**Day 1** — Backend Controller + IT (5 entity 병렬):
1. TK-CRUD-1-1·2, 2-1·2, 3-1·2, 4-1·2, 5-1·2 (Controller + IT 각 entity)

**Day 2** — Frontend 5 page + 단위 test:
2. TK-CRUD-1-3·4, 2-3·4, 3-3·4, 4-3·4, 5-3 (Page + vitest 각 entity)

**Day 3** — Hub + 매뉴얼 + DoD:
3. TK-CRUD-6-1·2·3 (MasterHubPage + 매뉴얼 + 회귀)
4. **DoD 본 PC 시각 검증** — IT_OPS 로 5 entity 1회씩 CRUD 시도

**총 ~2.7 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Controller | VcMachineAdminController + SettingGroupAdminController + VcConstraintAdminController + LineAdminController + HolidayAdminController (write 추가) |
| Backend Service | VcMachineAdminService + SettingGroupAdminService + (VcConstraint 는 기존 repo 위 + Holiday 는 기존 service 위 write 추가) |
| Backend IT | VcMachineAdminIT + SettingGroupAdminIT + VcConstraintAdminIT + LineAdminIT + HolidayAdminIT (총 5 신규) |
| Frontend | VcMachineAdminPage + SettingGroupAdminPage + VcConstraintAdminPage + LineAdminPage + HolidayAdminPage + MasterHubPage (5 카드 활성) |
| Frontend Test | vitest 신규 11+ cases (4 + 3 + 2 + 2 + 0) |
| Docs | USER_MANUAL_v1.2.md (§3 IT_OPS 5 entity 절차) |

---

## 9. Sprint 21 후 다음 단계

**Sprint 22 (EP-SEC-HARDEN) 진입 조건:**
- ✅ DoD 12/12 충족
- ✅ IT_OPS 사용자가 본 PC 에서 5 entity 1회씩 CRUD 시각 검증
- ⏳ S20 (EP-EXT-WEBHOOK) 도 완료 — Phase 4 진행률 33% (2/6)

**Sprint 22 첫 작업** — PLAN-SPRINT-22 작성 (Spring Security 6.1+ AuthenticationManager builder + PIN 강제 변경 30일).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 두 번째 sprint EP-CRUD-MASTER-2 6 Story / 22 Task / ~5 SP 분해. PHASE-4 plan §3 S21 정합 (단, "AlloyMold" 는 별도 entity 가 아닌 vc_constraint.composite_count 속성 → VcConstraintAdminPage 로 통합). 5 entity 전부 IT_OPS 자체 운영 가능. S20 와 병렬 진행 가능 (의존 없음). DoD 12 + 리스크 6 + 3-Day. Modulith 경계 정합 (master.api NamedInterface 의존 강제). |
