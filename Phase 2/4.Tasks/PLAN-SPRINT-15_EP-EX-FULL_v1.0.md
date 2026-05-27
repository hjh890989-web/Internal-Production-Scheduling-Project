# Sprint 15 진입 계획 — EP-EX-FULL (압출 PDD-03 완성) v1.0

**작성일**: 2026-05-27 | **버전**: 1.0 | **상태**: Sprint 15 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 15 Roadmap](TASK-001_WBS_v1.5.md) + [WBS v1.10 §6 carry-over](TASK-001_WBS_v1.10.md) + REF-PDD-03 v1.1 (압출 프로세스 + BR-E01/E05/E11) + [PLAN-SPRINT-14_EP-VC-FULL_v1.0](PLAN-SPRINT-14_EP-VC-FULL_v1.0.md)

---

## 1. 목적

**Sprint 14 EP-VC-FULL 직후 진입** — VC schedule SSoT 확정된 상태에서 **압출 PDD-03 워크플로우 완성**.

**현황 인벤토리 — Sprint 1~6 누적 자산 매우 풍부:**
- ✅ Backend 40 java files / 15 sub-package (api/confirm/conflict/deadline/event/events/export/gate/grouping/ranking/required/routing/schedule/yield)
- ✅ 4 Controller — CandidateRankingController (EP-18) + ExMatrixQueryController (EP-17) + ExMatrixExportController (EP-12) + ExConfirmController (확정)
- ✅ **VcConfirmedListener + VcChangedListener** — Sprint 6 EP-EX13/14 chain 이미 완성 (VC 확정/변경 → ex 자동 partial replan)
- ✅ ExReplanCompletedEvent + ExReplanPushListener — Sprint 6 STOMP push 이미 완성
- ✅ BackwardExtrusionCalculator (BR-E01 D-1 역산) + ExtrusionScheduleService
- ✅ Frontend ExMatrixPage + ExMatrixGrid + CandidateRankingTable + useExMatrix + useExUpdates + STOMP push + Excel 다운로드

**Sprint 15 의 진짜 gap:**
1. **시각 검증 sample seed** — Sprint 14 V039 패턴 따라 ex_schedule_candidate V040 sample (99999-SAMPLE-EX-* namespace)
2. **VcConfirmedListener chain IT 추가** — 본 PC 검증용 (이미 작동 — IT 만)
3. **BR-E01 (D-1 역산) + BR-E05 (수율 2531 reference) 회귀 IT** — 이미 일부 있을 가능성, 누락 보강
4. **ExMatrixPage 안내 메시지** — 빈 grid 시 시드 데이터 안내

**활성 후 효과:**
- PLANNER 가 시뮬뷰 (Sprint 14) 에서 confirm → 자동 ex 매트릭스 입력 (chain 완성)
- STK 가 압출 매트릭스 자동 갱신 받음 (STOMP push, Sprint 6 EP-EX14)
- 사용자가 압출 매트릭스 빈 grid 가 아닌 sample 데이터 시각 확인
- Sprint 16 EP-CONFIRM 진입 게이트 — VC + EX schedule SSoT 확정 흐름 완비

---

## 2. Sprint 15 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-EX-1 VcConfirmedListener + VcChangedListener 검증 IT 추가 | 0.5 | 0.3 |
| ST-EX-2 V040 ex_schedule_candidate sample seed + 안내 | 1.0 | 0.5 |
| ST-EX-3 BR-E01 / BR-E05 회귀 IT 보강 | 1.0 | 0.5 |
| ST-EX-4 ExMatrixPage 안내 메시지 + Capa link (Sprint 14 패턴) | 0.5 | 0.3 |
| ST-EX-5 다중 후보 ranking 시각 검증 (EP-18, 이미 완성) | 0.3 | 0.2 |
| ST-EX-6 EX-FULL 회귀 IT 총합 + DoD | 0.7 | 0.4 |
| **합계** | **~4 SP** | **~2.2 PD** |

> **WBS v1.5 계획 5 SP 대비 -1 SP** (Sprint 1~6 누적 자산 활용으로 신규 작업 적음).

---

## 3. 의존성 DAG

```
ST-EX-1 (chain IT)
    ↓
ST-EX-2 (V040 seed) ──┐
                      │
ST-EX-3 (BR-E01/E05) ─┤
                      ↓
ST-EX-4 (ExMatrixPage UX) ─→ ST-EX-5 (Ranking 검증)
                                    ↓
                             ST-EX-6 (회귀 + DoD)
```

**병렬 윈도우** — ST-EX-1·2·3 거의 독립 (Listener IT + seed + 회귀).

---

## 4. Story · Task 매트릭스

### ST-EX-1 — VcConfirmedListener + VcChangedListener 검증 IT

| Task | 내용 | SP |
|---|---|:--:|
| TK-EX-1-1 | VcConfirmedToExListenerIT — TransactionTemplate 으로 VcConfirmedEvent publish → ex.event.VcConfirmedListener 도달 + ExtrusionScheduleService 호출 검증 (Mockito timeout) | 0.3 |
| TK-EX-1-2 | VcChangedToExPartialReplanIT — VcChangedEvent publish → ex.event.VcChangedListener → PartialReplanService.replanWithContext → ExReplanCompletedEvent 발행 검증 | 0.2 |

### ST-EX-2 — V040 sample ex_schedule_candidate seed

| Task | 내용 | SP |
|---|---|:--:|
| TK-EX-2-1 | V040__seed_ex_candidate_sample.sql — 99999-SAMPLE-EX-* namespace + 4-shift × 75% 가용 (oxford 가용량) sample 10 row + V039 와 같은 1주 horizon | 0.6 |
| TK-EX-2-2 | WHERE NOT EXISTS 패턴 (Sprint 8 V028 + Sprint 14 V039 교훈) | 0 |
| TK-EX-2-3 | FK 충족 확인 — ex_constraint / line_type 시드 필요 시 동봉 | 0.4 |

### ST-EX-3 — BR-E01 / BR-E05 회귀 IT 보강

| Task | 내용 | SP |
|---|---|:--:|
| TK-EX-3-1 | 기존 BR-E01 (D-1 역산) IT 확인 (`DeadlineRegressionIT` 등) + 누락 case 보강 | 0.4 |
| TK-EX-3-2 | BR-E05 (수율 reference 2531, `29673-2R060`) 회귀 IT — `YieldAndDemandIT` 확인 + 신규 case 추가 | 0.4 |
| TK-EX-3-3 | 통합 — 시드 후 BR 룰 적용 확인 | 0.2 |

### ST-EX-4 — ExMatrixPage 안내 메시지

| Task | 내용 | SP |
|---|---|:--:|
| TK-EX-4-1 | ExMatrixPage 빈 grid 시 Alert info — "VC 확정 후 자동 입력 (chain). 현재 V040 시드 99999-SAMPLE-EX-* 표시" | 0.3 |
| TK-EX-4-2 | 상단 [성형 시뮬뷰 ←] 버튼 (navigate /vc/simview) — Sprint 14 패턴 정합 | 0.2 |

### ST-EX-5 — 다중 후보 ranking (EP-18) 시각 검증

| Task | 내용 | SP |
|---|---|:--:|
| TK-EX-5-1 | CandidateRankingTable 시각 확인 (이미 완성) + 빈 데이터 안내 강화 | 0.3 |

### ST-EX-6 — EX-FULL 회귀 IT + DoD

| Task | 내용 | SP |
|---|---|:--:|
| TK-EX-6-1 | 신규 IT 통합 회귀 (VcConfirmedToExListener + VcChangedToExPartialReplan + BR-E01/E05) — 모두 GREEN | 0.5 |
| TK-EX-6-2 | ArchUnit + 전체 IT 회귀 0건 | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ PLANNER 가 VC schedule confirm → ex.VcConfirmedListener LOG + ExtrusionScheduleService 호출
2. ✅ VC schedule 변경 → ex partial replan → ExReplanCompletedEvent → STOMP push
3. ✅ 압출 매트릭스 1주 horizon sample (V040 seed 10 row) 표시
4. ✅ BR-E01 D-1 역산 적용 검증 (기존 IT GREEN)
5. ✅ BR-E05 수율 2531 reference 적용 (기존 IT GREEN)
6. ✅ Excel 다운로드 (EP-12) 정상 (이미 완성)
7. ✅ 다중 후보 ranking (EP-18) 표시 (이미 완성)
8. ✅ ExMatrixPage 우상단 STOMP 연결 Tag + 빈 grid 안내

**비기능 DoD:**
1. ✅ ArchUnit GREEN
2. ✅ Backend 신규 IT 4+ + 회귀 0
3. ✅ TypeScript compile

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| V040 seed FK 의존성 (ex_constraint / line_type 미시드 가능성) | Flyway fail | V039 vc_machine 패턴 따라 동봉 시드 — WHERE NOT EXISTS idempotent |
| VcConfirmedListener 가 실 ExtrusionScheduleService 호출 시 부수효과 | IT context pollution | @MockitoBean ExtrusionScheduleService 또는 격리 IT |
| BR-E01/E05 기존 IT 정합성 — 이미 GREEN 인지 확인 | 시간 낭비 | TK-EX-3-1 inventory 우선 (실 신규 작업 식별) |
| ex_schedule_candidate 와 vc_schedule 의 chain coherence (production_date 정합) | 실 데이터 시각 어색 | V040 seed 의 vc_schedule_id 를 V039 row 와 매핑 |
| 99999-SAMPLE-EX-* 와 99999-SAMPLE-* (Sprint 14) 의 PROD cleanup | 운영 시 trash | 양쪽 namespace prefix 일관 + Sprint 19 cutover script 통합 |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — Backend Listener IT + Seed:
1. TK-EX-1-1~2 (Chain IT)
2. TK-EX-2-1~3 (V040 seed)

**Day 2** — BR + UI + 회귀:
3. TK-EX-3-1~3 (BR-E01/E05)
4. TK-EX-4-1~2 + TK-EX-5-1 (Frontend UX)
5. TK-EX-6-1~2 (회귀 + DoD)

**총 ~2.2 PD (1인 AI 가속)** — 2 영업일 여유 (3-Day 보다 짧음).

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend IT | `VcConfirmedToExListenerIT.java` + `VcChangedToExPartialReplanIT.java` + (기존 BR-E01/E05 IT 보강) |
| Backend Migration | V040 (ex_schedule_candidate + FK 시드 동봉) |
| Frontend | `ExMatrixPage.tsx` (안내 메시지 + 성형 시뮬뷰 link 추가) |
| Docs | rbac-matrix.md v1.2 갱신 불필요 (ex endpoint 이미 등재) |

---

## 9. Sprint 15 후 다음 단계

**Sprint 16 (EP-CONFIRM) 진입 조건:**
- ✅ DoD 11/11 충족
- ✅ VC + EX schedule SSoT 흐름 완비
- ✅ 본 PC sample 시각 확인 (V039 vc + V040 ex 양쪽 매트릭스 표시)

**Sprint 16 첫 작업** — PLAN-SPRINT-16 작성 (확정 게이트 BR-X01·X05·X07 — D-2 ~ D-1 락 + dual-review + immutable trigger).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-27 | Claude Code | 초안 — EP-EX-FULL 6 Story / 14 Task / ~4 SP 분해 + 의존성 DAG + DoD 11 + 2-Day 작업 순서. Sprint 1~6 누적 자산 (4 Controller + 15 sub-package + VcConfirmedListener + VcChangedListener + Frontend ExMatrixPage + STOMP 통합) 활용 → 신규 작업 최소 (시각 검증 sample seed + chain IT + BR 회귀 보강). |
