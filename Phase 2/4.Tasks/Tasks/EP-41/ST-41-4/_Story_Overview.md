# Story Overview — [EP-41] ST-41-4 백업·RPO 24h·RTO 4h

**Sprint**: S0+S5 | **Epic**: EP-41 | **SP**: 2

## Story 목적
> SRS REL-005: "일 1회 백업 ≥ 30일 보존, RPO ≤ 24h, RTO ≤ 4h"

pg_basebackup + WAL 아카이브 + STG 분기 DR 드릴.

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-41-4-1](TK-41-4-1.md) | pg_basebackup 자동화 (cron 02:00) | 0.6 |
| [TK-41-4-2](TK-41-4-2.md) | WAL 아카이브 (continuous) | 0.4 |
| [TK-41-4-3](TK-41-4-3.md) | STG 분기 DR 드릴 | 0.4 |

## DoD
- [ ] pg_basebackup 야간 02:00 KST 자동
- [ ] 백업 30일 보존
- [ ] WAL archive_command 설정
- [ ] STG 환경 PITR 드릴 (분기 1회)
- [ ] RTO ≤ 4h 달성

## References
- WBS §8.5 EP-41 ST-41-4, SRS REL-005, ADR-013

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
