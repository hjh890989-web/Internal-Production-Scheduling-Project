# Story Overview — [EP-44] ST-44-6 Audit 쿼리 ≤ 5초 (3년)

**Sprint**: S4 | **Epic**: EP-44 | **SP**: 1

## Story 목적
> SRS OPS-006: "최근 3년 audit를 actor·기간·entity로 p95 ≤ 5초 쿼리"

EP-19 (마스터 복원 UI) 와 정합 — 5년 부하 회귀.

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-44-6-1](TK-44-6-1.md) | audit 인덱스 최적화 | 0.3 |
| [TK-44-6-2](TK-44-6-2.md) | actor·기간·entity 쿼리 | 0.3 |
| [TK-44-6-3](TK-44-6-3.md) | p95 ≤ 5s 측정 | 0.2 |

## DoD
- [ ] audit 인덱스 (actor, occurred_at, target_id)
- [ ] 쿼리 API
- [ ] p95 ≤ 5,000ms (3년치)

## References
- WBS §8.5 EP-44 ST-44-6, SRS OPS-006

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
