# Internal Production Scheduling Project

자동차 고무 호스 제조사 사내 생산 스케줄링 시스템.

| 항목 | 값 |
|---|---|
| **Status** | Phase 3 (개발) — Sprint 0 완료 / Sprint 1 진행 중 |
| **Phase 2 산출** | 465 파일 · 253 SP · ~177 PD · 20 ADR · SRS v1.5 |
| **언어·런타임** | Java 21 LTS · Spring Boot 3.5 · Spring Modulith 1.4 · React 18 |
| **운영 규모** | 47품번 · LP 4대 + IC 1대 + 압출 (1주 horizon · 1500 row) |
| **사용자** | ~10명 (사내 한정, 사번 8자리 + PIN 4자리 NFR-SEC-007 v1.5) |

---

## 빠른 시작

Phase 3 Sprint 0 완료 (인프라·인증·관측·CI/CD·백업·KST·마스터). Sprint 1 EP-01 (수주통합) 진행 중.

### Phase 2 산출물 탐색
- 요구사항 — [Phase 1/](Phase%201/)
- 설계 — [Phase 2/](Phase%202/)
- WBS · ADR · Sprint Task — `Phase 2/4.Tasks/`
- Sprint 0 진입 계획 — [PLAN-001](Phase%202/4.Tasks/PLAN-001_Sprint0_Entry_Plan_v1.0.md)

### AI Harness
| 도구 | 설정 |
|---|---|
| Claude Code | [CLAUDE.md](CLAUDE.md) · [.claude/](.claude/) |
| Cursor · Gemini · Antigravity | [AGENTS.md](AGENTS.md) · [.cursor/](.cursor/) · [.gemini/](.gemini/) · [.agents/](.agents/) |
| 가이드 | [docs/harness/](docs/harness/) |
| 외부 sample archive | [docs/harness-samples/](docs/harness-samples/) |

### Harness 재정립 보고 (docs/harness/)
- [PLAN-002](docs/harness/PLAN-002_AI_Harness_Reset_v1.0.md) — 3 Stage 계획
- [REPORT-001](docs/harness/REPORT-001_Harness_Reset_Decisions_v1.0.md) — Q1~Q6 의사결정 (결재 A 승인)
- [REPORT-002](docs/harness/REPORT-002_Skills_Backend_Audit_v1.0.md) — skills/ 백엔드 적합성 감사
- [REPORT-003](docs/harness/REPORT-003_Harness_Phase2_Alignment_v1.0.md) — Phase 2 정합성 점검

---

## 비즈니스 룰 (핵심)

| BR | 내용 |
|---|---|
| BR-V07 | 당일 (D-0) 락 |
| BR-E05 | `29673-2R060` reference yield = 2531 |
| BR-X01 | 확정 게이트 (D-2 ~ D-1 수정 가능) |
| BR-X02 | 모든 mutation audit 강제 |
| BR-X04 | 시간대 `Asia/Seoul` 통일 (Spring + DB + UI) |
| BR-X05 | Dual-review (작성자 ≠ 승인자) |
| BR-X06 | MES 실패 시 Excel 폴백 |
| BR-X07 | D-2 hard 제약 |
| REQ-NF-SEC-004 | Audit ≥ 3년 보존 · INSERT-only (UPDATE/DELETE 금지) |

전체 BR 목록 — `Phase 2/2.SRS/`.

---

## 라이센스 · 운영

- 사내 한정 운영 (외부 배포 없음 · 영림원 ERP 통합 범위 외)
- 단독 의사결정 + Claude 와 page-by-page 공동개발
- 문서는 새 파일 (`*_v1.x.md`) 로 버전 분리
