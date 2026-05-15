# Story Overview — [EP-E2E] ST-E2E-1 E2E 1주 분량 시뮬레이션

**Sprint**: S5 | **Epic**: EP-E2E | **Priority**: Must ⭐⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-E2E-1-1](TK-E2E-1-1.md) | 데이터 시뮬레이터 (1주 분량 수주 생성) | 0.7 | Backend + QA | T-U + T-I | ☐ |
| [TK-E2E-1-2](TK-E2E-1-2.md) | 수주 → 성형 → 압출 cascade 시나리오 | 0.7 | QA | T-E2E | ☐ |
| [TK-E2E-1-3](TK-E2E-1-3.md) | 모든 납기 D-Day 충족 검증 | 0.7 | QA | T-E2E + A | ☐ |

> **선행**: 모든 Sprint 1~5 Epic
> **후행**: ST-E2E-2

---

## Story 레벨 DoD

- [ ] **`E2EDataSimulator`** — 1주 = 47품번 × 평균 100개 = ≈ 4,700 row
- [ ] **Cascade**: import → VC allocator → confirm → EX cascade → 시뮬뷰·매트릭스
- [ ] **모든 D-Day 충족** (납기 D-Day vs production_date)
- [ ] **모든 BR 회귀 PASS** (BR-V07·V13·V17·E01·E05·E06·E07·X01·X02·X07)
- [ ] **Playwright E2E** — UI cascade 검증

---

## References

- **WBS**: §8 EP-E2E ST-E2E-1
- **EXP**: EXP-1·5

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
