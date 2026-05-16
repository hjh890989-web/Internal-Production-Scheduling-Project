# GitHub Project Sync Scripts

Phase 2 산출물 → GitHub Issue + Project 일괄 등록.

## 사용 순서

```powershell
# 0. 사전 — gh CLI 인증 (브라우저 OAuth)
gh auth login --web --scopes "repo,project,read:org"
gh auth status    # 확인

# 1. Repo 라벨 일괄 생성 (~30개)
.\01-create-labels.ps1

# 2. Sprint Milestone 생성 (7개)
.\02-create-milestones.ps1

# 3. Issue 일괄 등록 (Epic 35 + Task 320 = ~355개, 약 15분)
.\03-create-issues.ps1 -DryRun     # 먼저 dry run
.\03-create-issues.ps1             # 실제 등록

# 4. GitHub Projects v2 셋업 + 모든 이슈 등록
.\04-setup-project.ps1
```

## 옵션

`03-create-issues.ps1`:
- `-DryRun` — 실제 이슈 생성 없이 시뮬레이션
- `-MaxIssues 10` — 최대 N개만 (테스트용)
- `-Repo "user/repo"` — repo 변경

## 라벨 체계 (01)

| 카테고리 | 라벨 | 색 |
|---|---|---|
| Sprint | `sprint:S0` ~ `sprint:S5` | 파스텔 |
| Type | `type:epic`·`task`·`backend`·`frontend`·`infra`·`test`·`docs`·`nfr` | 다양 |
| Priority | `priority:must`·`should`·`could` | 빨강 → 노랑 |
| Owner | `owner:solo`·`devops`·`qa` | 보라/시안 |
| Cross-cutting | `cross-cutting` | 회색 |
| BR | `br:V07`·`E05`·`X01`·`X02`·`X04`·`X05`·`X06`·`X07` | 노랑 |
| Flags | `critical-path`·`v1.4-new` | 강조 |

## Milestone (02) — v2.0 AI 가속판

| Milestone | Due | 비고 |
|---|---|---|
| Sprint 0 - Foundation | 2026-05-19 | W1 D1~D2 |
| Sprint 1 - Order Integration | 2026-05-22 | W1 D3~D5 |
| Sprint 2 - VC Scheduling | 2026-05-29 | W2 ⭐ v1.4 |
| Sprint 3 - EX Scheduling | 2026-06-05 | W3 |
| Sprint 4 - Governance | 2026-06-12 | W4 ⭐ v1.4 |
| Sprint 5 - UI + E2E | 2026-06-19 | W5 |
| Beta GO | 2026-06-19 | Phase 1.0 출시 |

## Project Custom Fields (04)

| Field | Type | 값 |
|---|---|---|
| Sprint | Single select | S0·S1·S2·S3·S4·S5·Beta |
| Epic | Text | EP-XX |
| SP | Number | Story Points |
| PD | Number | AI 가속 = SP × 0.3 |
| Start | Date | — |
| Target | Date | — |
| Priority | Single select | Must·Should·Could |
| BR | Text | BR-* references |

## 권장 Project Views (web UI 수동 생성)

1. **Roadmap by Sprint** — Board, group by Sprint
2. **Critical Path** — Table, filter `label:critical-path`
3. **NFR Epics** — Table, filter `label:type:nfr`
4. **Timeline (Roadmap)** — Roadmap, Start/Target, group by Sprint
5. **v1.4 신규** — Table, filter `label:v1.4-new` ⭐
