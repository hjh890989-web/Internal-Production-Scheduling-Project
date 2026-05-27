# Sprint 16 진입 계획 — EP-CONFIRM (확정 게이트 BR-X01·X05·X07) v1.0

**작성일**: 2026-05-27 | **버전**: 1.0 | **상태**: Sprint 16 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 16 Roadmap](TASK-001_WBS_v1.5.md) + [WBS v1.11 §5 carry-over](TASK-001_WBS_v1.11.md) + REF-SRS BR-X01/X05/X07 + [PLAN-SPRINT-15_EP-EX-FULL_v1.0](PLAN-SPRINT-15_EP-EX-FULL_v1.0.md)

---

## 1. 목적

**Sprint 15 EP-EX-FULL 직후 진입** — VC + EX schedule SSoT 흐름 완비된 상태에서 **확정 게이트 정책 강화**:

| BR | 정책 | 적용 단계 |
|---|---|---|
| **BR-X01** | 확정 게이트 — D-2 ~ D-1 만 수정 가능 | Confirmation Service + DB trigger |
| **BR-X05** | dual-review — 작성자 ≠ 승인자 (PLANNER role 내) | Confirmation Service 검증 + 거부 |
| **BR-X07** | D-2 hard 제약 — D-2 이후 신규 추가 차단 | DB trigger (BR-V07 패턴) |
| BR-V07 (기존) | 당일 (D-0) 락 — 수정 불가 | IntraDayLockRule + trg_vc_intra_day_lock |
| **immutable** | CONFIRMED 후 status 전이 외 변경 차단 | DB trigger |

**현황 인벤토리:**
- ✅ Sprint 4 EP-13 — BR-V07 당일 락 (IntraDayLockRule + trg_vc_intra_day_lock 이중 안전망)
- ✅ Sprint 4 EP-10 — VcScheduleConfirmationService + ExCandidateConfirmationService (단건 + batch + @Auditable)
- ✅ trg_vc_schedule_transition (status 전이 차단 — 일부)
- ⏳ **Sprint 16 신설** — BR-X01 D-2~D-1 게이트 + BR-X05 dual-review + BR-X07 D-2 hard + CONFIRMED immutable 통합

**활성 후 효과:**
- PLANNER 가 D-2 이후 신규 row 추가 시도 → 거절 (BR-X07 hard)
- 동일 PLANNER 가 본인 작성 row 확정 시도 → 거절 (BR-X05 dual-review)
- D-2~D-1 만 수정 (D-0 락 + D-3 이전 자유)
- Sprint 17 EP-DAY-LOCK 진입 게이트 — 확정 정책 baseline

---

## 2. Sprint 16 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-CONFIRM-1 BR-X07 D-2 hard 제약 (DB trigger + service guard) | 1.0 | 0.5 |
| ST-CONFIRM-2 BR-X05 dual-review 검증 (createdBy ≠ confirmedBy) | 1.0 | 0.5 |
| ST-CONFIRM-3 BR-X01 D-2~D-1 게이트 정책 통합 (intra-day + confirm chain) | 1.0 | 0.5 |
| ST-CONFIRM-4 CONFIRMED immutable trigger 보강 (V025/V034 패턴) | 0.5 | 0.3 |
| ST-CONFIRM-5 Frontend confirm UI dual-review 안내 + 거부 메시지 | 0.7 | 0.4 |
| ST-CONFIRM-6 EP-CONFIRM IT 6 cases + 회귀 | 0.8 | 0.4 |
| **합계** | **~5 SP** | **~2.6 PD** |

> **WBS v1.5 계획 5 SP 정합.**

---

## 3. 의존성 DAG

```
ST-CONFIRM-1 (BR-X07 D-2 hard)
    ↓
ST-CONFIRM-2 (BR-X05 dual-review) ──┐
                                    │
ST-CONFIRM-3 (BR-X01 게이트 통합)   │
                                    ↓
ST-CONFIRM-4 (CONFIRMED immutable) ─→ ST-CONFIRM-5 (Frontend UI)
                                            ↓
                                    ST-CONFIRM-6 (IT + DoD)
```

**병렬 윈도우:**
- **ST-CONFIRM-1 ↔ ST-CONFIRM-2** — trigger vs service-level guard 분리
- **ST-CONFIRM-4 ↔ ST-CONFIRM-5** — DB trigger vs Frontend UI 분리

---

## 4. Story · Task 매트릭스

### ST-CONFIRM-1 — BR-X07 D-2 hard 제약

| Task | 내용 | SP |
|---|---|:--:|
| TK-CONFIRM-1-1 | V041 — trg_vc_schedule_d2_hard trigger + fn — INSERT 시 production_date - now < 2 days 차단 (BR-V07 패턴 재사용). exception RAISE SQLSTATE 'P0001' | 0.4 |
| TK-CONFIRM-1-2 | VcScheduleService draft/INSERT 진입점에 service-level guard 추가 — DB trigger 발화 전 친화 메시지 응답 (ConflictException 변환) | 0.3 |
| TK-CONFIRM-1-3 | IT — D-3 INSERT 통과 / D-2 INSERT 차단 / D-1 INSERT 차단 / D+1 INSERT 통과 | 0.3 |

### ST-CONFIRM-2 — BR-X05 dual-review

| Task | 내용 | SP |
|---|---|:--:|
| TK-CONFIRM-2-1 | VcSchedule entity — created_by 컬럼 추가 (현재 없을 가능성 확인 — V042 schema 변경) | 0.3 |
| TK-CONFIRM-2-2 | VcScheduleConfirmationService.confirm() — createdBy == plannerId 시 IllegalStateException ("BR-X05 dual-review: 작성자 ≠ 승인자") | 0.4 |
| TK-CONFIRM-2-3 | IT — 같은 사번 작성+승인 → 403 (또는 409) / 다른 사번 → 200 | 0.3 |

### ST-CONFIRM-3 — BR-X01 D-2~D-1 게이트 통합

| Task | 내용 | SP |
|---|---|:--:|
| TK-CONFIRM-3-1 | V041 또는 별도 trigger — UPDATE 시 production_date == today (D-0) → BR-V07 trigger (이미 있음) + production_date - now < 0 (과거) → 차단 | 0.4 |
| TK-CONFIRM-3-2 | service guard — IntraDayLockRule + 신규 게이트 통합 (D-2~D-1 만 허용) | 0.4 |
| TK-CONFIRM-3-3 | IT — D-2 UPDATE OK / D-3 자유 / D-0 trigger 차단 (기존) / D+1 자유 | 0.2 |

### ST-CONFIRM-4 — CONFIRMED immutable

| Task | 내용 | SP |
|---|---|:--:|
| TK-CONFIRM-4-1 | trg_vc_schedule_transition 확인 (이미 있음) — status='CONFIRMED' 후 다른 컬럼 UPDATE 차단 (status 전이만 허용) | 0.3 |
| TK-CONFIRM-4-2 | 누락 시 V041 안에 동봉 또는 V043 hotfix | 0.2 |

### ST-CONFIRM-5 — Frontend confirm UI

| Task | 내용 | SP |
|---|---|:--:|
| TK-CONFIRM-5-1 | VcSimulationPage 확정 버튼 (PLANNER) — 클릭 시 dual-review 안내 Modal ("본인이 작성한 row 는 확정 불가 — 다른 PLANNER 에게 요청") | 0.3 |
| TK-CONFIRM-5-2 | 거부 응답 처리 — 409 (BR-X05) / 423 (BR-X07 D-2) / 400 (validation) 분기 메시지 | 0.3 |
| TK-CONFIRM-5-3 | unit test — Modal 분기 메시지 | 0.1 |

### ST-CONFIRM-6 — EP-CONFIRM IT 6 cases + 회귀

| Task | 내용 | SP |
|---|---|:--:|
| TK-CONFIRM-6-1 | EP-CONFIRM IT 6 — D-2 hard (BR-X07) + dual-review (BR-X05) + D-2~D-1 게이트 (BR-X01) + CONFIRMED immutable + 4 case 통합 | 0.6 |
| TK-CONFIRM-6-2 | 회귀 — VcScheduleConfirmationServiceIT + ExCandidateConfirmationServiceIT GREEN | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ D-2 이후 신규 row 추가 시도 → 423 + 한국어 메시지 (BR-X07)
2. ✅ 동일 PLANNER 가 본인 작성 row 확정 → 409 + dual-review 안내 (BR-X05)
3. ✅ D-2 ~ D-1 UPDATE 정상 (다른 PLANNER + BR-V07 정합)
4. ✅ D-0 UPDATE 차단 (기존 BR-V07 trg_vc_intra_day_lock)
5. ✅ CONFIRMED 후 status 전이만 허용 (status 외 column UPDATE 차단)
6. ✅ Frontend dual-review Modal 안내 + 거부 메시지 분기
7. ✅ 본 PC 시각 — PLANNER 1 (00000001) 가 작성한 row 를 PLANNER 1 이 확정 시도 → 거부 + PLANNER 2 (00000002) 가 확정 → 정상

**비기능 DoD:**
1. ✅ ArchUnit GREEN
2. ✅ Backend 신규 IT 6+ + 회귀 0
3. ✅ TypeScript compile + frontend unit tests GREEN
4. ✅ V041 migration 적용 (DEV/Testcontainers 부팅 정상)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| VcSchedule.created_by 컬럼 추가 → 기존 row migration 필요 | V039 sample row 영향 | V042 ALTER + DEFAULT 'system' + 기존 row 자동 채움 |
| BR-X05 dual-review 가 V039 sample row 차단 (created_by='V039-seed') | 본 PC 시각 검증 불가 | sample row 의 created_by 를 의도적으로 다른 값 (e.g., 'V039-seed') 로 둠 — 실 PLANNER 사번과 다름 → 모든 PLANNER 가 확정 가능 |
| D-2 trigger 가 V040 sample row INSERT 차단 (V039 sample 의 vc_production_date 가 CURRENT_DATE 기반이라 D-0/D-1/D-2 범위) | V040 Flyway fail | V040 ON BEFORE INSERT trigger 우회 — `INSERT ... WITH (skip_validation=true)` 또는 PROFILE 분리 (test/dev 시 trigger 비활성) — 또는 sample namespace 명시 우회 |
| BR-X07 trigger 가 PROD 진입 후 운영 데이터 INSERT 시 D-2 이전 신규 시도 차단 | 정상 동작 — 안내만 | 사용자 매뉴얼 명시 (Sprint 19 EP-BETA-LAUNCH) |
| CONFIRMED immutable trigger 가 status 전이 외 audit 컬럼 UPDATE 차단 | confirmed_at, confirmed_by 갱신 불가 | trigger 가 status + confirmed_at + confirmed_by 외에만 차단 (allowlist) |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — DB trigger + Service guard:
1. TK-CONFIRM-1-1~3 (BR-X07 D-2 hard)
2. TK-CONFIRM-2-1~3 (BR-X05 dual-review)

**Day 2** — 통합 + CONFIRMED immutable + Frontend:
3. TK-CONFIRM-3-1~3 (BR-X01 통합)
4. TK-CONFIRM-4-1~2 (CONFIRMED immutable)
5. TK-CONFIRM-5-1~3 (Frontend Modal)

**Day 3** — IT + DoD:
6. TK-CONFIRM-6-1~2 (EP-CONFIRM IT + 회귀)
7. **DoD 본 PC 시각 검증** — PLANNER 1/2 dual-review 흐름

**총 ~2.6 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Migration | V041 (D-2 hard trigger + CONFIRMED immutable trigger), V042 (VcSchedule.created_by 컬럼) |
| Backend Service | VcScheduleConfirmationService (dual-review 검증 추가), VcScheduleService.draft (D-2 guard) |
| Backend IT | `ConfirmGateIT.java` (6 cases) |
| Frontend | VcSimulationPage (확정 Modal + 거부 메시지), 또는 별도 ConfirmModal 컴포넌트 |
| Docs | rbac-matrix.md v1.2 부분 갱신 (BR-X05 dual-review 정책 명시) |

---

## 9. Sprint 16 후 다음 단계

**Sprint 17 (EP-DAY-LOCK) 진입 조건:**
- ✅ DoD 11/11 충족
- ✅ 본 PC dual-review 흐름 검증 (PLANNER 1·2 사번 시뮬)
- ✅ V041/V042 PROD 적용 안정성 검증

**Sprint 17 첫 작업** — PLAN-SPRINT-17 작성 (당일 락 BR-V07 + MES 폴백 BR-X06 — D-0 락 trigger + Excel degraded mode).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-27 | Claude Code | 초안 — EP-CONFIRM 6 Story / 18 Task / ~5 SP 분해 + 의존성 DAG + DoD 11 + 3-Day 작업 순서. Sprint 4 EP-10·13 자산 (Confirmation Service + IntraDayLockRule + trg_vc_intra_day_lock) 활용 + BR-X01·X05·X07 신규 통합 (D-2 hard trigger + dual-review service + CONFIRMED immutable + Frontend Modal). |
