# Story Overview — [EP-34] ST-34-1 마스터 dual-review (BR-X05)

**Sprint**: S2 | **Epic**: EP-34 | **SP**: 2 | **PD**: ~1.4 PD

## Story 목적
> WBS §10 EP-34 ST-34-1 / BR-X05: "마스터 변경은 2명 승인자 — 동일 actor 거부"

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-34-1-1](TK-34-1-1.md) | 2명 승인자 검증 (`MasterChangeReviewService`) | 0.7 |
| [TK-34-1-2](TK-34-1-2.md) | 동일 actor 거부 + audit | 0.4 |
| [TK-34-1-3](TK-34-1-3.md) | 통합 테스트 (회귀) | 0.3 |

## DoD
- [ ] `master_change_request` 테이블 + 상태 머신 (PENDING → APPROVED·REJECTED)
- [ ] 2명 PLANNER 또는 MASTER_ADMIN 승인
- [ ] 동일 actor 거부 (`requester != approver`)
- [ ] @Auditable audit 기록

## References
- WBS §10 EP-34 ST-34-1, BR-X05, REQ-FUNC-CO-002

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
