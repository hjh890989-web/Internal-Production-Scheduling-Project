# Story Overview — [EP-18] ST-18-1 N개 후보 ranking

**Sprint**: S5 | **Epic**: EP-18 다중 후보 ranking | **Priority**: Could
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-18-1-1](TK-18-1-1.md) | ranking 함수 (기한·교체·균형) | 0.8 | Backend | T-U + T-I | ☐ |
| [TK-18-1-2](TK-18-1-2.md) | ≥ 3 후보 반환 회귀 + API | 0.7 | QA + Backend | T-I + A | ☐ |
| [TK-18-1-3](TK-18-1-3.md) | UI 후보 선택 (Table 비교) | 0.6 | Frontend | T-U + T-I | ☐ |

> **선행**: [EP-05](../../EP-05/), [EP-09](../../EP-09/)
> **후행**: 없음

---

## Story 레벨 DoD

- [ ] **`RankingScore`** record (deadlineMargin·setupCount·lineBalance) + 합산 점수
- [ ] **`MultiCandidateAllocator`** — 3 시나리오 실행 후 ranking
- [ ] **`POST /api/v1/schedule/vc/candidates`** N=3 반환
- [ ] **UI Table**: 후보별 yield 합·conflict 수·점수 + "선택" 버튼
- [ ] **회귀 ≥ 3 distinct 후보** (XT-001)

---

## References

- **WBS**: §8 EP-18 ST-18-1
- **SRS REQ-FUNC**: REQ-FUNC-XT-001
- **TestPlan**: TC-XT-001

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
