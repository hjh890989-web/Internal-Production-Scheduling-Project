# Epic Overview — [EP-10] 사용자 확정 게이트 (M-10)

**Sprint**: S4 (거버넌스·최적화·당일 락) | **Priority**: Must ⭐ | **SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Epic 목적

> WBS §7 EP-10 인용: "ST-10-1 Candidate → Confirmed 전이 게이트 (VC), ST-10-2 확정 게이트 (EX)"
> SRS REQ-FUNC-VC-019·EX-019 / BR-X01 / CON-07: "수주·VC·EX 스케줄의 모든 상태 전이는 Planner role의 명시적 확정을 거쳐야 한다. 직접 DB 쓰기 금지."

본 Epic은 Sprint 4의 거버넌스 기반 — 모든 변경은 (Draft → Candidate → Confirmed) 상태 머신을 거치고, Confirmed 전이는 Planner role 사용자의 명시적 행위로만 가능. DB 직접 쓰기·API 우회 모두 차단. EP-11 (Audit)·EP-13 (당일 락) 모두 본 Epic의 commit 시점 hook.

**Why Sprint 4 핵심**:
- **BR-X01 hard 제약** — 사용자 책임/추적성 기반
- **EP-11 Audit 통합 트리거** — Confirmed 전이가 audit 생성 시점
- **EP-EX13 진입 전제** — `vc.changed` 이벤트는 Confirmed 상태에서만 발생

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-10-1](ST-10-1/_Story_Overview.md) | Candidate → Confirmed 전이 게이트 (VC) | 3 | ~2.1 | T-U + T-I + A | ☐ |
| [ST-10-2](ST-10-2/_Story_Overview.md) | 확정 게이트 (EX) | 2 | ~1.4 | T-U + T-I | ☐ |

---

## Epic 레벨 DoD

- [ ] **상태 머신**: `Draft → Candidate → Confirmed` (VC·EX 양쪽)
- [ ] **Planner role RBAC** — Confirmed 전이만 Planner 허용
- [ ] **직접 DB 쓰기 차단** — `app_user` role은 SELECT/INSERT만 (UPDATE는 트리거 통한 상태 전이만)
- [ ] **EX 확정 게이트** — VC 동일 패턴 적용
- [ ] **negative 테스트**: 직접 UPDATE/DELETE 시도 차단 검증
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §7 EP-10
- **PDD-04**: M-10 사용자 확정
- **SRS REQ-FUNC**: REQ-FUNC-VC-019, REQ-FUNC-EX-019
- **BR**: BR-X01 (사용자 확정 게이트)
- **CON**: CON-07 (Planner role 단독 확정)
- **TestPlan**: TC-VC-019, TC-EX-019
- **선행**: EP-05 (VC 후보), EP-09 (EX 후보)
- **후행**: EP-11 (Audit), EP-EX13 (vc.changed 트리거)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-10 + REQ-FUNC-VC-019·EX-019 + BR-X01 |
