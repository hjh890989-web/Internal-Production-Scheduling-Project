---
name: PM Task (NS-01 Pre-Survey)
about: 단일 작업 단위 — 0.4 PD
title: "[TK-E2E-2-2] NS-01 사전 설문 (만족도 baseline)"
labels: 'sprint:S5, epic:EP-E2E, story:ST-E2E-2, type:docs, priority:must, owner:pm+qa'
assignees: ''
---

## :dart: Task Summary
- **Task ID**: TK-E2E-2-2
- **소속**: EP-E2E / ST-E2E-2 / Sprint S5
- **우선순위**: Must / **추정**: 0.4 PD (~3.2h) / **Owner**: PM + QA
- **작업 요약**: 베타 4명 사전 설문 (NS-01) — 현재 Excel-only 워크플로우 만족도. Google Forms 또는 SurveyMonkey. 5점 척도 + 자유 답변. Phase 3 NS-02 (post-survey) 비교 baseline.

---

## :link: References
- **상위 Story**: [`_Story_Overview.md`](_Story_Overview.md)
- **WBS**: §8 EP-E2E ST-E2E-2 (Task 2)
- **NS-01**: 사용자 만족도 baseline

---

## :hammer_and_wrench: Implementation Plan

```
docs/beta/survey/
  ns-01-pre-survey-questions.md                  [설문 문항]
  ns-01-results-baseline.md                      [응답 정리]
```

### `ns-01-pre-survey-questions.md`

```markdown
# NS-01 사전 설문 — 현재 Excel-only 워크플로우 만족도

## 응답자 정보
- 이름: ____
- 역할: [생산계획팀 / 현장 STK]
- 경력: __년

## 1. 현재 만족도 (5점 척도: 1=매우 불만, 5=매우 만족)

| 항목 | 점수 |
|---|---|
| 수주 처리 속도 (Excel 양식 입력·검증) | __/5 |
| 스케줄 작성 정확성 (성형·압출 매칭) | __/5 |
| 충돌 발견·해결 능력 | __/5 |
| 현장 공유 (출력·전달) 효율 | __/5 |
| 납기 D-Day 충족 능력 | __/5 |
| **종합 만족도** | __/5 |

## 2. 페인 포인트 (자유 응답)
- 가장 답답한 부분: ____
- 가장 시간 많이 쓰는 작업: ____
- Excel 사용 중 실수가 자주 일어나는 곳: ____

## 3. 기대 사항 (베타 시스템에 대해)
- 가장 기대하는 기능: ____
- 우려되는 부분: ____

## 4. 운영 데이터 (선택)
- 일주일 평균 충돌 발생 건수: __
- 일주일 평균 납기 지연 건수: __
```

### `ns-01-results-baseline.md`

```markdown
# NS-01 응답 결과 (베타 시작 전)

## 응답자: 4명 (Planner 2, STK 2)

## 종합 점수 평균
- 수주 처리: 2.5/5
- 스케줄 정확성: 2.8/5
- 충돌 발견: 2.3/5
- 현장 공유: 3.0/5
- 납기 D-Day: 2.7/5
- **종합: 2.7/5**

## 주요 페인 포인트 (빈도순)
1. 충돌 발견이 사후 (3/4 응답) — 사례: INT-1 (300개 납기 지연)
2. 수주 → 스케줄 변환 시간 (3/4) — 평균 2~3시간 소요
3. 47품번 셋팅 호환성 매트릭스 외움 (2/4)

## Phase 3 NS-02 비교 baseline
- 본 결과를 1주 베타 종료 후 NS-02와 비교
- 목표: 종합 만족도 ≥ 3.5/5 (Phase 3 진입 KPI)
```

---

## :test_tube: Acceptance Criteria

**검증**: UAT

- [ ] **설문 문항 (NS-01)** 작성 완료
- [ ] **베타 4명 응답** ≥ 4건 (전원 응답)
- [ ] **baseline 결과 정리** — 점수 평균 + 페인 포인트 빈도
- [ ] **PM 검토** — Phase 3 NS-02 비교 기준 확정

---

## :checkered_flag: Definition of Done

- [ ] 위 측정 기준 통과
- [ ] 설문 + 응답 문서
- [ ] **Sprint Review 데모**: 페인 포인트 발표 + 베타 기대치
- [ ] PM 승인

---

## :construction: Dependencies

- **선행**: [TK-E2E-2-1](TK-E2E-2-1.md)
- **후행**: [TK-E2E-2-3](TK-E2E-2-3.md), Phase 3 NS-02
- **Critical Path**: ⭐

---

## :memo: Implementation Notes

- Google Forms 사용 — 응답 결과 자동 정리 + 응답률 추적
- 5점 척도 표준 (Likert) — 통계 분석 용이
- 자유 응답은 정성 분석 → 빈도 분류
