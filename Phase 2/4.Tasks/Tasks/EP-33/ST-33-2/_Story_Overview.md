# Story Overview — [EP-33] ST-33-2 pg_basebackup + WAL archiving + 분기 복원 드릴

**Sprint**: S0~S5 | **Epic**: EP-33 | **Priority**: Must ⭐⭐ | **SP**: 2 | **PD**: ~1.4 PD

## Story 목적
> WBS §10 EP-33 ST-33-2 / SRS REQ-NF-REL-005: "일 1회 풀백업 + WAL continuous + 분기 PITR 드릴"

**EP-41 ST-41-4 와 정합** — 본 Story가 인프라 정의 측, EP-41이 NFR 측정 측.

## 포함 Task 목록

| Task | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-33-2-1](TK-33-2-1.md) | 야간 02:00 KST 풀백업 (systemd timer) | 0.5 | DevOps |
| [TK-33-2-2](TK-33-2-2.md) | WAL continuous archiving | 0.4 | DevOps |
| [TK-33-2-3](TK-33-2-3.md) | STG PITR 드릴 (분기 1회) | 0.5 | DevOps + QA |

> 선행: [ST-33-1](../ST-33-1/_Story_Overview.md)
> 후행: EP-41 ST-41-4 (NFR 측정), EP-46 (운영 비용)

## Story 레벨 DoD
- [ ] systemd timer 02:00 KST + pg_basebackup
- [ ] WAL archive_command S3·NAS
- [ ] 분기 PITR 드릴 + RTO 측정
- [ ] 운영 가이드 (`docs/operations/backup-restore.md`)

## References
- WBS §10 EP-33 ST-33-2, SRS REL-005, ADR-013
- 선행: [TK-33-1-3](../ST-33-1/TK-33-1-3.md) (env secrets)
- 정합: [TK-41-4-1·2·3](../../EP-41/ST-41-4/TK-41-4-1.md) (NFR 측정 측)

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
