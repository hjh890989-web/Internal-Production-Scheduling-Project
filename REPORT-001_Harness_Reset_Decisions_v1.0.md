# REPORT-001 AI Harness 재정립 — 의사결정 권고 보고서 v1.0

**작성일**: 2026-05-16 | **버전**: 1.0 | **상태**: 사용자 검토용
**선행 문서**: PLAN-002 AI Harness 재정립 계획 v1.0

> **목적**: PLAN-002 §3 의사결정 사항 Q1~Q6 에 대한 **Claude 추천안 + 근거 + 대안 trade-off** 를 사용자가 한눈에 검토·승인 가능한 형태로 정리.

---

## 1. 요약 (1쪽 결재 양식)

| Q | 항목 | 추천 | 영향도 |
|:--:|---|---|:---:|
| **Q1** | 샘플 archive 위치 | **`docs/harness-samples/`** | 낮음 |
| **Q2** | 4 README 가이드 위치 | **`docs/harness/`** | 낮음 |
| **Q3** | 샘플 .claude·.cursor·.gemini 보존 여부 | **(a) 전체 archive** | 낮음 |
| **Q4** | Claude Code 권한 정책 | **(b) 표준 + deny 명시** | **높음** ⭐ |
| **Q5** | Cursor hooks 작성 시점 | **(b) P2 — Sprint 1+ 이후** | 중간 |
| **Q6** | Symlink vs 복사 동기화 | **(b) 복사 + 스크립트** (Windows) | 중간 |

**6 결정 종합 1줄**:
> Q1·Q2 `docs/` 하위로 정리 · Q3 전체 archive 보존 · Q4 표준 권한 + 위험 명령 명시적 deny · Q5 hooks는 Phase 3 안정화 후 · Q6 Windows 환경 → 복사 방식

---

## 2. Q1 — 샘플 archive 위치

### 옵션
- (a) `rules/_archive/` — 기존 `rules/` 폴더 유지하면서 하위에 archive
- (b) **`docs/harness-samples/`** — `rules/` 정리 + docs/ 하위 표준 위치

### 추천: (b) `docs/harness-samples/`

### 근거
1. **표준 디렉토리 관습** — `docs/` 는 모든 프로젝트의 문서 표준 위치
2. **`rules/` 폴더 잔존 회피** — `rules/_archive/` 선택 시 `rules/` 폴더가 계속 보임 → 의도 (archive·deprecated) 가 명확하지 않음
3. **신규 개발자 onboarding** — `docs/harness-samples/` 는 이름만으로 "Harness 샘플 참고용" 의미가 즉시 전달

### 대안 trade-off
| 선택 | 장점 | 단점 |
|---|---|---|
| (a) `rules/_archive/` | 변경량 적음 | `rules/` 잔존, 의도 불명확 |
| (b) `docs/harness-samples/` | 표준 + 명확 | 디렉토리 1개 이동 작업 |

### 영향도: **낮음** — 단순 파일 이동

---

## 3. Q2 — 4 README 가이드 위치

### 옵션
- (a) 프로젝트 루트 — `README-claude-harness.md` 등 4개를 루트에 평면 배치
- (b) **`docs/harness/`** — 4개 README 를 docs/harness/ 하위로 그룹화

### 추천: (b) `docs/harness/`

### 근거
1. **루트 단순화 원칙** — 프로젝트 루트는 `README.md` (메인 진입점) 만 두는 게 표준
2. **그룹화** — 4개 가이드가 한 폴더에 모이면 의도 (AI 도구 harness 가이드 묶음) 명확
3. **확장성** — 향후 가이드 추가 (예: `README-codex-harness.md`) 시 동일 위치로 일관

### 대안 trade-off
| 선택 | 장점 | 단점 |
|---|---|---|
| (a) 루트 | 접근 빠름 (1 click) | 루트 어수선, 4 파일 노이즈 |
| (b) `docs/harness/` | 정리 + 표준 | 1 depth 추가 |

### 영향도: **낮음** — 단순 그룹화

---

## 4. Q3 — 샘플 .claude·.cursor·.gemini 보존 여부

### 옵션
- (a) **전체 archive** — `docs/harness-samples/_original-rules/` 에 모두 보존
- (b) 일부만 — 유용한 것만 선별 보존
- (c) 삭제 — 가이드만 남기고 샘플 폐기

### 추천: (a) 전체 archive

### 근거
1. **샘플 가치**:
   - Cursor 17 skill — `100-error-handling`·`200-git-flow` 등 번호 체계 학습 자료
   - Claude Code 8 agent — `java-spring`·`jpa-querydsl` 등 도메인 패턴 참고
   - Gemini 1 agent (readme-architect) — README 자동 생성 패턴
2. **디스크 부담 0** — 전체가 markdown 파일, 총 < 1 MB
3. **복구 비용 ↑** — 삭제 후 재작성 시 4~8시간 소요. 보존 시 0초
4. **참조 가치** — Phase 3 진행 중 새 skill·agent 작성 시 베이스 패턴 활용 가능

### 대안 trade-off
| 선택 | 장점 | 단점 |
|---|---|---|
| (a) 전체 archive | 참조 가치 + 복구 비용 0 | git tracked +20 파일 |
| (b) 선별 | 노이즈 절감 | 선별 기준 모호, 추후 후회 가능 |
| (c) 삭제 | 깔끔 | 학습 자료 손실, 복구 4~8h |

### 영향도: **낮음** — git 추적만 결정

---

## 5. Q4 — Claude Code 권한 정책 (⭐ 핵심)

### 옵션
- (a) 보수적 — 현재 (`PowerShell(git push origin main)` 1건만)
- (b) **표준** — 일반 개발 ops 허용, 위험 명령 명시적 deny
- (c) 광범위 — `rm -rf`·`docker system prune`·force push 까지 허용

### 추천: (b) 표준 + deny 명시

### 근거
1. **(a) 보수적의 비용**:
   - 매 `git add`·`docker compose up`·`./gradlew build` 마다 사용자 승인 필요
   - 단독 + Claude page-by-page 모드 → 분당 5~10 회 interruption 예상
   - 개발 속도 50%+ 저하

2. **(c) 광범위의 위험**:
   - `rm -rf src/` 같은 명령 1회 실행 시 복구 불가
   - `git push --force` 시 origin/main 손상 가능
   - 단독 개발이라 backup checkpoint 사용자 본인만 가능

3. **(b) 표준의 균형점**:
   - 일반 개발 ops (build·test·git add·docker compose up) 자동 → 속도 ✓
   - 위험 명령 (rm·prune·force) 명시 deny → 사고 차단 ✓
   - 새 명령은 그때 그때 사용자 승인 (보수적 fallback) ✓

### Q4 표준 권한 상세

```jsonc
// .claude/settings.local.json
{
  "permissions": {
    "allow": [
      // ─── Git: safe operations ───
      "Bash(git add:*)",
      "Bash(git commit:*)",
      "Bash(git status)",
      "Bash(git diff:*)",
      "Bash(git log:*)",
      "Bash(git fetch:*)",
      "Bash(git pull:*)",
      "Bash(git checkout:*)",
      "Bash(git branch:*)",
      "Bash(git merge:*)",
      "PowerShell(git push origin main)",  // 기존
      "PowerShell(git push origin:*)",     // feature 브랜치 push 허용

      // ─── Build: Spring Boot ───
      "Bash(./gradlew build)",
      "Bash(./gradlew test)",
      "Bash(./gradlew check)",
      "Bash(./gradlew bootRun)",
      "Bash(./gradlew clean)",
      "Bash(./gradlew flywayMigrate)",

      // ─── Build: React / Node ───
      "Bash(npm install:*)",
      "Bash(npm ci)",
      "Bash(npm run:*)",
      "Bash(npm test)",
      "Bash(npx playwright test:*)",
      "Bash(npx vite:*)",

      // ─── Docker: safe ops only ───
      "Bash(docker compose up:*)",
      "Bash(docker compose down)",
      "Bash(docker compose logs:*)",
      "Bash(docker compose ps)",
      "Bash(docker compose restart:*)",
      "Bash(docker compose exec:*)",

      // ─── Test / QA ───
      "Bash(./gradlew sonarqube)",
      "Bash(npm run lint)",
      "Bash(npm run type-check)",

      // ─── Scripts ───
      "Bash(python scripts/*.py)",

      // ─── GitHub CLI: safe ops ───
      "Bash(gh pr view:*)",
      "Bash(gh pr list:*)",
      "Bash(gh pr comment:*)",
      "Bash(gh issue:*)",
      "Bash(gh repo view)"
    ],
    "deny": [
      // ─── Destructive: 명시 차단 ───
      "Bash(rm -rf:*)",
      "Bash(rm -fr:*)",
      "Bash(docker system prune:*)",
      "Bash(docker volume prune:*)",
      "Bash(git push --force:*)",
      "Bash(git push -f:*)",
      "Bash(git reset --hard:*)",
      "Bash(git clean -fd:*)",
      "PowerShell(Remove-Item:*)",
      "PowerShell(rm:*)"
    ]
  }
}
```

### 영향도: **높음 ⭐** — 전체 개발 속도 + 사고 위험 결정

---

## 6. Q5 — Cursor hooks 작성 시점

### 옵션
- (a) P1 — Sprint 0 진입 시 즉시 작성
- (b) **P2 — Sprint 1+ 안정 후**
- (c) 생략 — hooks 미사용

### 추천: (b) P2 — Sprint 1+ 이후

### 근거
1. **Hooks 복잡도** — `beforeShellExecution`·`afterFileEdit` 등 이벤트별 정교한 룰 필요
2. **디버깅 비용** — 잘못된 hook 은 모든 command 차단 → 개발 정지
3. **Phase 3 초기 우선순위**: 작동하는 코드 작성 > 검증·자동화 — hooks 는 안정 후 추가
4. **대안 존재** — Q4 deny 정책으로 기본 위험 차단 가능 — hooks 없어도 안전

### 대안 trade-off
| 선택 | 장점 | 단점 |
|---|---|---|
| (a) P1 즉시 | 처음부터 자동 검증 | 초기 디버깅 부담 |
| (b) P2 이후 | 안정 후 도입 | Sprint 0 자동화 없음 |
| (c) 생략 | 단순 | 추후 자동화 기회 손실 |

### 영향도: **중간** — Phase 3 후반 결정 다시 가능

---

## 7. Q6 — Symlink vs 복사 동기화

### 옵션
- (a) Symlink — `.agents/skills/` ↔ `.claude/skills/` 링크
- (b) **복사 + 동기화 스크립트** — 파일 복제 + sync 스크립트
- (c) 분리 — 각 도구별 독자 작성

### 추천: (b) 복사 + 동기화 스크립트

### 근거
1. **Windows NTFS 한계**:
   - junction (`mklink /J`) 은 권한 issue — admin 권한 필요
   - symlink (`mklink /D`) 은 git 호환성 떨어짐 (Linux ↔ Windows 충돌)
   - PowerShell `New-Item -ItemType SymbolicLink` 도 동일 issue

2. **파일 크기 작음** — 전체 `.agents/skills/`·`.claude/skills/`·`.cursor/skills/`·`.gemini/agents/` 합쳐도 < 100 KB

3. **동기화 스크립트 단순**:
   ```python
   # scripts/sync_harness.py
   import shutil
   shutil.copytree('.agents/skills/', '.claude/skills/', dirs_exist_ok=True)
   shutil.copytree('.agents/skills/', '.cursor/skills/', dirs_exist_ok=True)
   ```
   pre-commit hook 으로 자동 실행

4. **git diff 명확** — symlink 는 git에서 single line, 복사는 실 파일로 diff 가능

### 대안 trade-off
| 선택 | 장점 | 단점 |
|---|---|---|
| (a) Symlink | 단일 진실 (DRY) | Windows 호환성 ↓, git 충돌 |
| (b) 복사 + 스크립트 | Windows OK, git diff 명확 | 동기화 누락 위험 (스크립트로 완화) |
| (c) 분리 | 도구별 독자 | DRY 위반, 유지보수 비용 ↑ |

### 영향도: **중간** — 운영 비용 결정

---

## 8. 6 결정 종합 — 적용 시 예상 결과

### 디렉토리 최종 구조

```
프로젝트 루트/
├── README.md                          ← 신규 (B.0.3)
├── CLAUDE.md                          ← 신규 (B.0.1, Claude Code 자동 로드)
├── AGENTS.md                          ← 신규 (B.0.2, 공통 — 모든 AI 도구 로드)
├── PLAN-002_AI_Harness_Reset_v1.0.md  ← 계획서 (보존)
├── REPORT-001_Harness_Reset_Decisions_v1.0.md  ← 본 보고서
│
├── .claude/                           ← Claude Code 인식
│   ├── settings.local.json            ← Q4 표준 권한
│   ├── skills/                        ← P1 (5종)
│   └── agents/                        ← P1 (3종)
├── .cursor/                           ← Cursor 인식
│   ├── rules/                         ← P1 (4종 .mdc)
│   ├── skills/                        ← P2
│   └── agents/                        ← P2
├── .gemini/                           ← Gemini 인식
│   └── agents/                        ← P1 (2종)
├── .agents/                           ← 공통 (모든 도구 참조)
│   └── skills/                        ← P1 (3종 공통)
│
├── 0.Prompt/                          ← 보존 (대화기록)
├── Phase 1/, Phase 2/                 ← 보존 (분석·설계)
├── scripts/                           ← 보존 + 신규 sync_harness.py
│
└── docs/
    ├── harness/                       ← Q2 4 README 이동
    │   ├── README-claude-harness.md
    │   ├── README-common-harness.md
    │   ├── README-cursor-harness.md
    │   └── README-gemini-harness.md
    └── harness-samples/               ← Q1·Q3 archive
        └── _original-rules/
            ├── CLAUDE.md.template
            ├── AGENTS.md.template
            ├── .agents/
            ├── .claude/
            ├── .cursor/
            └── .gemini/
```

### 영향 요약

| 항목 | 결과 |
|---|---|
| 신규 파일 | ~25개 (P0 4 + P1 17 + 가이드 이동 4) |
| 이동 파일 | 4 README + `rules/` 전체 |
| 삭제 | 없음 (모두 보존·archive) |
| Claude 권한 | 1건 → 약 40건 allow + 11건 deny |
| Phase 3 진입 차단 해소 | ✅ |
| 개발 속도 (Q4 영향) | +50%+ (interruption 감소) |

---

## 9. 사용자 결재

### 옵션
- **결재 A**: 6 추천 모두 승인 → 즉시 PLAN-002 Stage A·B.P0 진입
- **결재 B**: 일부 조정 후 승인 → 조정 사항 명시
- **결재 C**: 보류·재검토 → 추가 정보 요청 사항 명시

### 권고
**결재 A — 6 추천 모두 승인**.

본 6 결정은 trade-off 가 명확하고 후속 조정 가능성이 모두 보존 (예: Q4 권한은 후속 추가·삭제 자유, Q5 hooks 도 P2 시점에 다시 결정 가능). 보류 시 Phase 3 진입 지연만 발생.

---

## 10. 다음 단계

1. **본 보고서 검토** (사용자, 10~30분)
2. **결재 A·B·C 선택**
3. (A 선택 시) **PLAN-002 Stage A 즉시 실행** — 재배치 (~1시간)
4. **Stage B.P0 실행** — CLAUDE·AGENTS·README·권한 (~4시간)
5. **Stage C 검증** — Claude Code 재시작 + 자동 로드 확인
6. **Phase 3 D1 진입 가능** ✓

---

## 11. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-16 | (작성자) | 초안 — PLAN-002 §3 Q1~Q6 권고 정리 |
