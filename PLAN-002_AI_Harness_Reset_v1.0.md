# PLAN-002 AI Harness 재정립 계획 v1.0

**작성일**: 2026-05-16 | **버전**: 1.0 | **상태**: 사용자 승인 대기

> **목표**: SRS-001 v1.4 (1,904 줄) + PDD-MASTER v1.6 (1,508 줄) 기반으로 4 AI 도구 (Claude Code · Cursor · Gemini Code Assist · 공통 AGENTS.md) 의 Harness 를 템플릿 수준에서 **프로젝트 실제 정의 수준**으로 재정립.

---

## 1. 배경 및 현황 (사전 조사 요약)

### 1.1 기반 문서 (Source of Truth)
- **SRS-001 v1.4** — 60 NFR + 100+ REQ-FUNC + 18 IS + 5 OS 정의, ISO/IEC/IEEE 29148 준수
- **PDD-MASTER v1.6** — PDD-01·02·03 통합본, IEEE 12207 + BPMN 2.0, End-to-End 역산 아키텍처

### 1.2 현재 Harness 위치 (핵심 issue)

```
프로젝트 루트/
├── .claude/                      ← Claude Code 자동 인식 (✓)
│   └── settings.local.json       ← 권한 1건만 (git push)
├── 0.Prompt/                     ← 대화기록 (보존 대상)
├── Phase 1/, Phase 2/            ← 분석·설계 (보존 대상)
├── scripts/                      ← Python 변환기 (보존 대상)
└── rules/                        ← ⚠️ 샘플 위치 — AI 도구가 자동 인식 안 함
    ├── CLAUDE.md                 ← 템플릿 수준
    ├── AGENTS.md                 ← 템플릿 수준
    ├── README-{claude·common·cursor·gemini}-harness.md  ← 가이드 4종
    ├── .agents/                  ← 샘플 3 rules + 2 skills
    ├── .claude/                  ← 샘플 8 agents + 3 commands
    ├── .cursor/                  ← 샘플 1 agent + 17 skills
    └── .gemini/                  ← 샘플 1 agent
```

**핵심 issue**:
1. **`rules/` 하위 컨텐츠가 자동 인식 안 됨** — Claude Code 등은 프로젝트 루트의 `.claude/`·`CLAUDE.md` 등만 인식
2. **모든 컨텐츠가 템플릿** — 본 프로젝트 도메인(공정 스케줄링)·기술 스택(Spring·React)·핵심 규칙(BR-V07 당일 락·BR-E05 yield=2531 등) 반영 0건
3. **샘플은 다른 프로젝트 흔적** — Kafka·Flutter·QueryDSL 등 본 프로젝트 SAD에 명시되지 않은 기술 스택 포함

### 1.3 가이드 4 README 핵심 권장사항

| 도구 | 컨텐츠 (지식) | 제어 (행동) | 자동 로드 |
|---|---|---|:---:|
| Claude Code | `CLAUDE.md` + `.claude/skills/` | `.claude/agents/` + `settings.local.json` 권한 | ✓ 프로젝트 루트 |
| Cursor | `AGENTS.md` + `.cursor/rules/*.mdc` | `.cursor/agents/` + `.cursor/hooks.json` | ✓ 프로젝트 루트 |
| Gemini | `AGENTS.md` + `.agents/skills/` | `.gemini/agents/` | ✓ 프로젝트 루트 |
| 공통 | `AGENTS.md` 단일 진실 | (각 도구별 분산) | — |

**전략**: 지식 중앙화 (`AGENTS.md`·`.agents/skills/`) + 제어 분산 (`.claude/`·`.cursor/`·`.gemini/`) + Symlink

---

## 2. 작업 분해 (3 Stage)

### Stage A — 정리·재배치 (1~2시간)

**목표**: `rules/` 디렉토리 정리 + 실 위치로 재배치 결정

| Task | 작업 | 소요 |
|---|---|:--:|
| A.1 | `rules/README-*-harness.md` 4종 → 프로젝트 루트로 이동 (또는 `docs/harness/` 신규 폴더) | 10분 |
| A.2 | `rules/CLAUDE.md`·`AGENTS.md` 템플릿 → 프로젝트 루트로 이동 + **백업** (`rules/_archive/`) | 10분 |
| A.3 | `rules/.agents/`·`.claude/`·`.cursor/`·`.gemini/` 샘플 → `rules/_archive/`로 백업 (참조용 보존) | 10분 |
| A.4 | 프로젝트 루트에 **실 적용용** `.agents/`·`.cursor/`·`.gemini/` 폴더 신규 생성 (빈 골격) | 10분 |
| A.5 | `.gitignore` 검토 — 신규 위치 반영 | 10분 |

### Stage B — 컨텐츠 작성 (P0·P1·P2 분할)

**목표**: SRS·PDD 기반 실제 프로젝트 컨텐츠 작성

#### P0 — 즉시 (Phase 3 진입 전 필수, ~4시간)

| Task | 산출물 | 핵심 내용 | 소요 |
|---|---|---|:--:|
| B.0.1 | **`CLAUDE.md`** (프로젝트 루트) | 프로젝트 비전 + 18 IS 요약 + 60 NFR 카테고리 + 핵심 BR (X01·X02·X07·V07·E05) + Tech Stack + Phase 진행 상황 | 90분 |
| B.0.2 | **`AGENTS.md`** (프로젝트 루트, 공통) | 도메인 용어 정의 (저압가류기·IC가류기·슬롯·앵글·회전 등) + 역산 아키텍처 + 모듈 경계 (order·vc·ex·master·audit·notify·common) | 60분 |
| B.0.3 | **`README.md`** (프로젝트 루트) | 프로젝트 개요 + Phase 1·2·3 진행 상황 + Harness 가이드 링크 + 빠른 시작 | 60분 |
| B.0.4 | **`.claude/settings.local.json`** 확장 | 권한 추가 (Bash docker compose, gradle, npm, gh, git add/commit; PowerShell 동일) | 30분 |

#### P1 — Sprint 0 (Phase 3 D1~D5, ~6시간)

| Task | 산출물 | 핵심 내용 | 소요 |
|---|---|---|:--:|
| B.1.1 | **`.claude/skills/`** 핵심 5종 (SKILL.md) | (1) docker-compose-up · (2) spring-modulith-arch · (3) excel-poi-parse · (4) flyway-migration · (5) jpa-querydsl | 90분 |
| B.1.2 | **`.claude/agents/`** 도메인 3종 | (1) vc-allocator-dev · (2) ex-allocator-dev · (3) test-fixture-builder | 60분 |
| B.1.3 | **`.cursor/rules/*.mdc`** 핵심 4종 | spring-boot.mdc · react-vite.mdc · postgresql.mdc · architecture.mdc (`globs` 패턴 매칭) | 90분 |
| B.1.4 | **`.gemini/agents/`** 2종 + workflow | scheduling-architect · srs-traceability + workflow `phase3-task-execution` | 60분 |
| B.1.5 | **`.agents/skills/`** 공통 3종 | sprint-task-execution · git-conventional-commit · test-coverage-report | 60분 |

#### P2 — Sprint 1 이후 점진 (개발 중 보강)

| Task | 산출물 | 핵심 내용 |
|---|---|---|
| B.2.1 | 도메인별 .md 추가 (Sprint 2 진입 시) | vc-scheduling-rules·ex-formula-br-e05·intra-day-lock-br-v07 |
| B.2.2 | Hooks (`.cursor/hooks.json`) | beforeShellExecution — `rm -rf` 차단, afterFileEdit — Flyway migration 자동 검증 |
| B.2.3 | Skill 마이그레이션 | `rules/.claude/commands/` 3종 (fix-error·gitflow-commit·setup-env) → `.claude/skills/` 표준화 |
| B.2.4 | Plugins 패키징 (선택) | Phase 3 종료 시점 — `.claude/plugin/` 형태로 재사용 |

### Stage C — 검증·문서화 (1시간)

| Task | 작업 |
|---|---|
| C.1 | Claude Code 재시작 후 `CLAUDE.md` 자동 로드 검증 |
| C.2 | `/help` 또는 사용 가능 skill·agent 목록 확인 |
| C.3 | 첫 Task (TK-00-1-1 docker-compose) 시범 실행으로 권한·skill 동작 확인 |
| C.4 | `README.md`에 변경 사항 반영 + commit |

---

## 3. 의사결정 사항 (사용자 확정 필요)

| # | 항목 | 옵션 | 권장 |
|:--:|---|---|---|
| Q1 | `rules/_archive/` 또는 `docs/harness-samples/` 위치 | 후자가 의도 명확 | `docs/harness-samples/` |
| Q2 | 4 README-*-harness.md 위치 | (a) 프로젝트 루트 (b) `docs/harness/` | (b) `docs/harness/` |
| Q3 | 샘플 .claude·.cursor·.gemini 보존 여부 | (a) 전체 archive (b) 일부만 (c) 삭제 | (a) 전체 archive — 참조 가치 |
| Q4 | Claude Code 권한 정책 | (a) 보수적 (현재 git push만) (b) 표준 (build·test·git add) (c) 광범위 (rm·docker prune까지) | (b) 표준 권장 |
| Q5 | `.cursor/hooks.json` 작성 시점 | (a) P1 (Sprint 0) (b) P2 (Sprint 1+) | (b) — Hooks는 안정 후 |
| Q6 | Symlink 사용 여부 (`.agents/skills/` ↔ 각 도구) | (a) Symlink (b) 복사·동기화 스크립트 | (a) Windows는 권한 issue → 복사 권장 |

---

## 4. 일정 추정 (단독 + Claude page-by-page)

| Stage | 작업량 | 사용자 의사결정 | Claude 작성 |
|---|:--:|:--:|:--:|
| **Stage A** (재배치) | 1시간 | 10분 (Q1·Q2·Q3 결정) | 50분 |
| **Stage B.P0** (CLAUDE·AGENTS·README·권한) | 4시간 | 30분 (검토·승인) | 3.5시간 |
| **Stage B.P1** (skills·agents·rules) | 6시간 | 1시간 (Q4 결정·검수) | 5시간 |
| **Stage C** (검증) | 1시간 | 30분 (동작 확인) | 30분 |
| **합계** | **12시간** | **~2시간** | **~10시간** |

**현실 일정**: P0만 완료 시 4시간 → Phase 3 D1 진입 가능. P1·P2는 Phase 3 진행 중 보강.

---

## 5. 산출물 목록 (예상)

```
프로젝트 루트/
├── README.md                                    ← 신규 (B.0.3)
├── CLAUDE.md                                    ← 신규 (B.0.1)
├── AGENTS.md                                    ← 신규 (B.0.2)
├── .claude/
│   ├── settings.local.json                      ← 확장 (B.0.4)
│   ├── skills/                                  ← P1 (B.1.1) 5종
│   └── agents/                                  ← P1 (B.1.2) 3종
├── .cursor/
│   ├── rules/                                   ← P1 (B.1.3) 4종 .mdc
│   ├── skills/                                  ← P2 추후
│   └── agents/                                  ← P2 추후
├── .gemini/
│   └── agents/                                  ← P1 (B.1.4) 2종
├── .agents/
│   └── skills/                                  ← P1 (B.1.5) 3종 공통
├── docs/
│   └── harness/                                 ← Stage A
│       ├── README-claude-harness.md
│       ├── README-common-harness.md
│       ├── README-cursor-harness.md
│       └── README-gemini-harness.md
└── docs/harness-samples/                        ← Stage A (archive)
    ├── _original-rules/
    └── samples-readme.md                        ← 참조 메모
```

**예상 신규 파일**: ~25개 (P0 4개 + P1 17개 + 가이드 이동 4개)
**예상 신규 line**: ~3,000 줄 (각 SKILL.md·rule·agent 평균 100~200 줄)

---

## 6. 위험·완화

| Risk | 영향 | 완화 |
|---|:---:|---|
| Claude Code 자동 로드 누락 | 높음 | 재배치 후 즉시 검증 (Stage C.1) |
| 권한 과도 부여로 사고 | 중간 | Q4 표준 옵션 + 위험 명령 (rm·prune) 제외 |
| 샘플 archive 손실 | 낮음 | Stage A에서 git tracked 보존 |
| `rules/` 의도 변경 (다른 의도였을 경우) | 낮음 | 사용자 확정 (Q1·Q2·Q3) |
| Symlink Windows 호환성 | 중간 | Q6에서 복사 방식 권장 |
| Phase 3 일정 지연 | 중간 | P0만 우선 (4시간), P1·P2는 병행 |

---

## 7. 승인 후 진행 순서

1. **사용자 의사결정** Q1~Q6 답변
2. **Stage A** 즉시 실행 (1시간)
3. **Stage B.P0** 실행 (4시간) — CLAUDE.md·AGENTS.md·README·권한
4. **검증 (Stage C)** — Claude Code 재시작 후 동작 확인
5. **Phase 3 진입 가능 상태 확정** (P0 완료 시점)
6. **Stage B.P1·P2** — Phase 3 D1~D5 병행 진행

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-16 | (작성자) | 초안 — Phase 3 진입 전 Harness 재정립 |
