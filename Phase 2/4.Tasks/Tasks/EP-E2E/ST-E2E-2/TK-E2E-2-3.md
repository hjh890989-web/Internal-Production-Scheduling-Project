---
name: PM Task (1-Week Parallel Operation Guide)
about: 단일 작업 단위 — 0.6 PD
title: "[TK-E2E-2-3] 1주 병행 운영 가이드"
labels: 'sprint:S5, epic:EP-E2E, story:ST-E2E-2, type:docs, priority:must, owner:pm+운영'
assignees: ''
---

## :dart: Task Summary
- **Task ID**: TK-E2E-2-3
- **소속**: EP-E2E / ST-E2E-2 / Sprint S5
- **우선순위**: Must / **추정**: 0.6 PD (~4.8h) / **Owner**: PM + 운영팀
- **작업 요약**: 1주 베타 기간 운영 가이드 — 기존 Excel 워크플로우 + 새 시스템 양쪽 사용. 일별 일정 (월~금) + 역할별 (Planner·STK) Task list + 비상 대응 (시스템 장애 시 Excel fallback) + Daily standup 양식.

---

## :link: References
- **상위 Story**: [`_Story_Overview.md`](_Story_Overview.md)
- **WBS**: §8 EP-E2E ST-E2E-2 (Task 3)
- **연관**: 선행 [TK-E2E-2-1](TK-E2E-2-1.md), [TK-E2E-2-2](TK-E2E-2-2.md), 후행 Phase 3 시작

---

## :hammer_and_wrench: Implementation Plan

```
docs/beta/
  beta_parallel_operation_guide.md
  daily_standup_template.md
  emergency_rollback_procedure.md
```

### `beta_parallel_operation_guide.md`

```markdown
# 베타 1주 병행 운영 가이드 (2026-05-22 ~ 05-28)

## 목표
- 기존 Excel 워크플로우 유지 (production)
- 새 시스템 베타 사용 + 피드백 수집
- 양쪽 결과 비교 → 시스템 신뢰성 확보

## 일별 일정

### 월요일 (5/22) - 시작일
- 09:00 — 베타 사용자 4명 onboarding (1시간)
  - 로그인 + 기본 UI 투어
  - 첫 수주 import (병행 — Excel + 시스템 양쪽)
- 14:00 — 첫 1주 후보 생성 시연 (Planner 2명)
  - Excel: 기존 방식
  - 시스템: 후보 비교 페이지 → 1 후보 선택
- 17:00 — Day 1 회고 (15분)

### 화~목 (5/23~5/27)
- 09:00 — Daily Standup (15분)
  - 어제 작업 / 오늘 작업 / 막힌 점
  - 시스템 vs Excel 비교 결과 공유
- 일과 중 — 양쪽 시스템 사용
- 17:30 — Day N 회고 + Slack 피드백

### 금요일 (5/28) - 마무리
- 09:00 — NS-02 사후 설문 작성 (개별 30분)
- 14:00 — 1주 정리 보고 + Phase 3 입력 정리
- 15:00 — Sprint Review 데모 (전체 팀)

## 역할별 Task

### Planner (2명)
- [ ] 수주 import — Excel + 시스템 양쪽 (대조 검증)
- [ ] VC 후보 생성 (3 시나리오 비교) → 1 선택 확정
- [ ] 충돌 발견 시 대안 검토 (≥ 3 distinct)
- [ ] 변경 발생 시 자동 cascade 확인
- [ ] Daily 피드백 — 어떤 기능이 도움 됐는지

### STK 현장 (2명)
- [ ] 시뮬뷰 페이지 — Candidate 확인
- [ ] 순서 변경 제안 (≥ 5건 실험)
- [ ] 매트릭스 뷰 — 압출 시각화
- [ ] Excel 다운로드 (원본 양식 동일성 확인)
- [ ] STK-03 패드 환경 — UI 가독성·조작성 피드백

## 비상 대응

### 시스템 장애 발생 시
1. 즉시 Slack #scheduling-beta 보고
2. Excel 워크플로우 단독 사용 (production 보장)
3. DevOps 24시간 대응 (담당: ___)

### 잘못된 스케줄 감지 시
1. 시스템 사용 즉시 중단
2. 베타 응답자가 Slack 보고
3. PM이 즉시 검증 + 원인 분석
4. 24시간 내 fix or 베타 일시 중단 결정

## KPI 목표 (1주 베타 종료 시)
- 베타 사용 빈도: ≥ 1회/일 (전원)
- 시스템 ↔ Excel 결과 일치율 ≥ 95%
- NS-02 만족도 ≥ 3.5/5 (NS-01 대비 향상)
- 피드백 ≥ 20 distinct items
```

### `daily_standup_template.md`

```markdown
# Daily Standup — 베타 Day N (YYYY-MM-DD)

## 어제 (5분)
- 시스템 사용 시간: __ 분
- Excel 사용 시간: __ 분
- 발견 issue: ____

## 오늘 계획 (5분)
- 시스템 사용 예정 작업: ____
- 비교 검증 항목: ____

## 막힌 점 (5분)
- 어떤 기능 사용법: ____
- 어떤 결과 의문: ____
- 도움 필요 요청: ____
```

### `emergency_rollback_procedure.md`

```markdown
# 베타 → Excel-only Rollback 절차

## Trigger 조건
- 잘못된 스케줄 데이터 production에 영향 (잠재)
- 시스템 장애 > 30분
- 베타 사용자 ≥ 2명 동시 issue 보고

## 절차 (15분 이내)
1. PM이 #scheduling-beta에 ROLLBACK 선언
2. 베타 사용자 4명 시스템 사용 즉시 중단
3. Excel 워크플로우 100% 사용
4. DevOps가 시스템 read-only mode 전환 (data corruption 방지)
5. 24시간 내 원인 분석 + 결정 (re-start vs 베타 종료)
```

---

## :test_tube: Acceptance Criteria

**검증**: UAT

- [ ] **운영 가이드** 완성 — 일별 일정·역할 Task·비상 대응
- [ ] **Daily Standup 양식**
- [ ] **Rollback 절차** — PM·DevOps 검토 + 사인
- [ ] **베타 4명 가이드 숙지** (Day 1 onboarding 시)
- [ ] **PM·운영팀 승인**

---

## :checkered_flag: Definition of Done

- [ ] 위 측정 기준 통과
- [ ] 3 문서 작성 (가이드·standup·rollback)
- [ ] **Sprint Review 데모**: 베타 시작 준비 완료 발표
- [ ] PM·운영팀 사인

---

## :construction: Dependencies

- **선행**: [TK-E2E-2-1](TK-E2E-2-1.md), [TK-E2E-2-2](TK-E2E-2-2.md), [TK-E2E-1-3](../ST-E2E-1/TK-E2E-1-3.md) (시뮬레이션 PASS)
- **후행**: Phase 3 진입 (베타 종료 후)
- **Critical Path**: ⭐⭐⭐ (Phase 2 → Phase 3 transition)

---

## :memo: Implementation Notes

- 가이드는 markdown 또는 Notion — 베타 사용자 접근성 우선
- Daily standup은 Zoom + Slack thread 결합
- Rollback은 보수적 — 데이터 무결성 최우선
