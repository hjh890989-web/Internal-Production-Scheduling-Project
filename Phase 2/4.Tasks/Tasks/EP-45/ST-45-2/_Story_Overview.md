# Story Overview — [EP-45] ST-45-2 5년 데이터 볼륨 (≤ 10M row)

**Sprint**: S5 | **Epic**: EP-45 | **SP**: 2

## Story 목적
> SRS COM-002: "5년치 수주·스케줄·실적 ≤ 10M row 보존"

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-45-2-1](TK-45-2-1.md) | 용량 시뮬레이션 | 0.6 |
| [TK-45-2-2](TK-45-2-2.md) | 5년치 시드 | 0.5 |
| [TK-45-2-3](TK-45-2-3.md) | 파티션 정책 검증 | 0.3 |

## DoD
- [ ] 5년치 데이터 시뮬 → 10M row 미만
- [ ] orders·vc_schedule·ex_schedule partition by month
- [ ] Vacuum·analyze 자동

## References
- WBS §8.5 EP-45 ST-45-2, SRS COM-002

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
