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

## RCPM 일자 자동 계산 (11)

`09-fill-date-fields.ps1` 은 Sprint 단위 동일 일자만 채워서 Roadmap 겹침 + cross-owner 의존성·자원 한계 모두 무시.
→ Resource-Constrained Critical Path Method 기반 Task별 일자 계산 + Project 일자 필드 갱신.

```powershell
# Python 3.10+, gh CLI 인증 (read:project,project scope 필요)
python scripts\github-sync\11-fill-task-level-dates.py --dry-run                       # 계산만, GitHub 미연동
python scripts\github-sync\11-fill-task-level-dates.py --report task_dates_report.md   # 마크다운 리포트
python scripts\github-sync\11-fill-task-level-dates.py                                 # 실제 update
```

알고리즘:
1. **파싱** — `Phase 2/4.Tasks/Tasks/EP-*/ST-*/TK-*.md` 320 Task 전체:
   - `sprint:`·`epic:`·`story:`·`owner:` 라벨, `**추정**: X.X PD`
   - `**연관**: 선행 [TK-…](url)` / `**선행**: [TK-…](url)` 4종 표기 모두 처리 → predecessors 집합
2. **DAG 구축** — 존재하지 않는 ID·자기 자신 drop. cycle 감지 시 경고 + Sprint 순 강제 추가.
3. **Topological sort** — Kahn's algorithm, tie-break = Sprint 순서 → Epic → Task ID
4. **Forward pass** (RCPM):
   ```
   earliest_start = max(
       모든 predecessor.end + 1 영업일,
       owner_busy_until[primary_owner],
       sprint_start
   )
   end = add_business_days(start, ceil(PD) - 1)
   owner_busy_until[primary_owner] = end + 1 영업일
   ```
   ※ multi-owner (`backend+qa`) 는 첫 owner 만 자원 점유 (단순화 가정).
5. **Backward pass** → slack=0 Task = Critical Path → 리포트 ⭐ 표시
6. **GitHub Project update** — 두 필드 쌍 모두 (Roadmap이 어느 필드 쓰든 동작):
   - `Start date` / `Target date` — Project v2 표준 (Roadmap 기본)
   - `Start` / `Target`           — 기존 커스텀 (`09-*` 산출물 호환)

샘플 결과 (320 Task):
- Root (선행 없음) 21건 → Sprint 시작일에 owner별 병렬 출발
- 선행 보유 299건 → 의존성 + owner 자원 따라 sequential
- Critical Path 23건 → Beta GO blocking chain (대부분 S5 NFR + E2E 검증)

⚠ **알려진 한계**:
- Multi-owner 첫 owner 점유 가정 → 보조 owner 자원 낭비 가능
- 같은 owner role 의 실제 인력 N명 ≠ 1 (현재 단일 슬롯 모델) — N병렬 지원하려면 owner_busy 큐 확장
- 1주 standup 기반 보정 권장 (DAG 정밀도가 ±1일 수준이라 실행 일정은 매주 갱신)
- DAG cycle 발견 시 경고 — Task 파일의 선행/후행 cross-reference 점검 필요

⚠ GraphQL rate limit: 5000 points/시간. 320 Task × 2 쌍 = ~1280 호출 → 여유 충분하지만,
   하루 여러 번 반복 실행은 피할 것 (item-list 등 부가 호출 포함 시 한도 도달 가능).
