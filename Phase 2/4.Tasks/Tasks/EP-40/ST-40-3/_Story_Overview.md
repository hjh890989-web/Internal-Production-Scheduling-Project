# Story Overview — [EP-40] ST-40-3 WebSocket PUSH·Critical 알림 SLO

**Sprint**: S4 | **Epic**: EP-40 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

## Story 목적

> SRS REQ-NF-PER-004: "Critical PUSH p99 ≤ 60초, WebSocket p95 ≤ 2초"

EP-EX14·EP-03 (Critical 알림) 통합 부하 — 24h soak 테스트.

## 포함 Task 목록

| Task ID | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-40-3-1](TK-40-3-1.md) | Soak 테스트 환경 (24h) | 1.0 | DevOps + QA |
| [TK-40-3-2](TK-40-3-2.md) | 24h soak 실행 + 측정 | 0.7 | QA |
| [TK-40-3-3](TK-40-3-3.md) | Critical p99 ≤ 60s · WebSocket p95 ≤ 2s 검증 | 0.4 | QA |

> 선행: [EP-EX14](../../EP-EX14/), [EP-03](../../EP-03/)
> 후행: ST-40-4

## DoD
- [ ] 24h soak 환경 구성
- [ ] Critical 알림 (HIGH priority) p99 ≤ 60,000ms
- [ ] WebSocket PUSH p95 ≤ 2,000ms
- [ ] Slack 알림 자동 (위반 시)

## References
- WBS §8.5 EP-40 ST-40-3, SRS PER-004, TestPlan T-S

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
