# Epic Overview — [EP-18] 다중 후보 ranking (C-01)

**Sprint**: S5 | **Priority**: Could | **SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Epic 목적

> WBS §8 EP-18 인용: "ST-18-1 N개 후보 ranking (기한·교체·균형)"
> SRS REQ-FUNC-XT-001: "Allocator는 ≥ 3 후보를 ranking 점수와 함께 반환. 기준: 납기 여유·셋팅 교체 수·라인 균형."

본 Epic은 Sprint 2 Allocator 결과를 **다중 후보**로 확장. 단일 greedy 결과 대신 N개 시나리오 (e.g., yield 우선·셋업 최소·납기 여유 우선) 평가 후 Planner가 선택. UI 후보 비교 화면.

**Why Sprint 5 Could**:
- 정확성 가치 vs 복잡도 — 정상 운영은 단일 후보로 충분, 충돌 시 가치
- Allocator 변경 비-trivial — 시간 cost
- 향후 ML 추천으로 자동화 검토 (Phase 2+)

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-18-1](ST-18-1/_Story_Overview.md) | N개 후보 ranking (기한·교체·균형) | 3 | ~2.1 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **`CandidateRankingService`** — 3 점수 (기한 여유·셋업 수·라인 균형) 계산
- [ ] **`AllocatorMultiCandidate.allocate()`** — N=3 후보 생성 후 ranking 정렬
- [ ] **API**: `POST /api/v1/schedule/vc/candidates` — N개 반환
- [ ] **UI 후보 비교** — Table 3 row + Planner 선택
- [ ] **회귀 ≥ 3 후보 반환** (TC-XT-001)
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §8 EP-18
- **PDD**: C-01 다중 후보 ranking
- **SRS REQ-FUNC**: REQ-FUNC-XT-001
- **TestPlan**: TC-XT-001
- **선행**: EP-05, EP-09

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
