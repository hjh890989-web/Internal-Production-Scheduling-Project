# Harness Samples (Read-Only Archive)

본 디렉토리는 **외부 sample / archive** 보관소입니다. 본 프로젝트의 실제 harness 활성 설정은 루트 [.claude/](../../.claude/), [.cursor/](../../.cursor/), [.gemini/](../../.gemini/), [.agents/](../../.agents/) 에 있습니다.

## Contents

| 경로 | 출처 | 활성 여부 | 비고 |
|---|---|---|---|
| [_skills-archive/](_skills-archive/) | Matt Pocock 공개 skills 패키지 | ❌ 비활성 (archive) | 14 → 11 등록 (REPORT-002 R2) |

## 활성 Skills 위치

본 프로젝트의 실제 사용 skills 는 [.claude/skills/](../../.claude/skills/) 에 있습니다.

- backend/ — Spring·JPA·Flyway·Modulith·Security·Redis·Postgres·Docker
- wrapper/ — Matt Pocock skill 위 본 프로젝트 도메인 확장 (tdd-java·diagnose-spring·architecture-modulith)

## 왜 archive 인가

REPORT-002 (skills/ 백엔드 적합성 감사) §6 권고에 따라 외부 sample 을 본 프로젝트와 분리. 부적합 8개 (plugin.json 제거) + 부분 적합 11개 (wrapper 가 본 프로젝트 적용) + 9개 신규 (.claude/skills/backend/).

- R1 — 분리 (본 디렉토리)
- R2 — plugin.json 등록 14→11
- R5 — nested `.git/` 제거 후 본 git 단일화

---

**Maintainer**: 본 프로젝트
**참조**: [REPORT-002](../../REPORT-002_Skills_Backend_Audit_v1.0.md)
