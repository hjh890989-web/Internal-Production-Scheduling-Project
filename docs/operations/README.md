# docs/operations/ — 운영 매뉴얼 + 베타 시나리오 + 페르소나 가이드

**Phase**: 3 (개발 종료) → 4 (베타 운영 진입) | **갱신**: 2026-05-23

> Phase 4-A STG 부팅 후 운영팀 + Planner + STK_USER + IT_OPS + READ_ONLY 4 페르소나
> 인계 자산. 베타 시나리오 5건 + 일상 운영 + DR + 보안.

---

## 1. 배포·인프라 매뉴얼

| 파일 | 용도 |
|---|---|
| [stg-deploy.md](stg-deploy.md) | STG 환경 배포·운영 (§11 Sprint 6 변경사항 부록) |
| [prod-deploy.md](prod-deploy.md) | PROD Blue/Green 배포 (Phase 5 진입 시) |
| [backup-restore.md](backup-restore.md) | pg_basebackup + WAL + PITR |
| [secrets-management.md](secrets-management.md) | vault secret 관리 |
| [idp-failover.md](idp-failover.md) | Keycloak IdP 페일오버 |
| [idp-federation-setup.md](idp-federation-setup.md) | LDAP/AD federation |
| [slack_channels.md](slack_channels.md) | 운영 Slack 채널 매핑 |

---

## 2. 베타 시나리오 SOP — Phase 4-B (5 + 1 후보)

| ID | 파일 | 페르소나 | 핵심 |
|---|---|---|---|
| BS-01 | [01-normal-week-horizon.md](beta-scenarios/01-normal-week-horizon.md) | Planner | 정상 1주 horizon 운영 |
| BS-02 | [02-conflict-and-ranking.md](beta-scenarios/02-conflict-and-ranking.md) | Planner + STK_USER | 충돌 + ≥3 ranking + swap |
| BS-03 | [03-vc-changed-cascade.md](beta-scenarios/03-vc-changed-cascade.md) | Planner | VC override → cascade chain (BR-X03) |
| BS-04 | [04-master-restore.md](beta-scenarios/04-master-restore.md) | IT_OPS + Planner | audit forensic + 시점 복원 |
| BS-05 | [05-intraday-lock-override.md](beta-scenarios/05-intraday-lock-override.md) | Planner | BR-V07 일중 락 override |
| **BS-06** 🆕 | [06-capacity-overflow-kd-supplement.md](beta-scenarios/06-capacity-overflow-kd-supplement.md) | Planner + IT_OPS | **BR-V12·V13 capa 큐 + KD 보충** (DI-07/08 활성 후 후보) |

---

## 3. 4 페르소나 가이드 — Phase 4-C

| Role | 파일 | 권한 |
|---|---|---|
| PLANNER | [01-planner.md](persona/01-planner.md) | confirm + accept + override (P1) |
| STK_USER | [02-stk-user.md](persona/02-stk-user.md) | swap 제안 (P3) |
| IT_OPS | [03-it-ops.md](persona/03-it-ops.md) | 모든 화면 + Grafana + Phase 4 체크리스트 (P2) |
| READ_ONLY | [04-read-only.md](persona/04-read-only.md) | 조회 + KPI + audit forensic (P4) |

---

## 4. Phase 4 진입 흐름

```
Phase 4-A STG 부팅 (Week 1) — stg-deploy.md §11 + seed-stg-beta-data.sh
  ↓
Phase 4-B 베타 시나리오 5건 (Week 2) — beta-scenarios/01~05
  ↓
Phase 4-C 사용자 교육 (Week 3) — persona/01~04 × Q&A
  ↓
Phase 4-D DR + 보안 (Week 4) — backup-restore + idp-failover + Alertmanager
  ↓
Phase 4-E PROD cutover 게이트 (Week 5) — Phase-4_EntryPlan §7 11 KPI 통과
  ↓
Phase 5 PROD cutover — prod-deploy.md (Blue/Green)
```

---

## 5. 상위 참조

- [Phase-3_Completion_v1.1.md](../../Phase%203/2.Phase-Completion/Phase-3_Completion_v1.1.md) (전판 [v1.0](../../Phase%203/2.Phase-Completion/Phase-3_Completion_v1.0.md))
- [Phase-4_EntryPlan_v1.1.md](../../Phase%204/Phase-4_EntryPlan_v1.1.md) (전판 [v1.0](../../Phase%204/Phase-4_EntryPlan_v1.0.md))

---

## 6. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — 운영 매뉴얼 + 베타 시나리오 5 + 페르소나 4 index |
| 1.1 | 2026-05-23 | Claude Code | Sprint 7 carry-over 풀 스택 반영 — BS-06 후보 (V12·V13) + Phase-3/4 v1.1 링크 갱신 |
