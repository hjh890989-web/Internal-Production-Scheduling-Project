# Story Overview — [EP-15] ST-15-1 Candidate → 시뮬뷰 ≤ 2초 발행

**Sprint**: S5 | **Epic**: EP-15 성형 현장 시뮬뷰 | **Priority**: Should ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §8 EP-15 ST-15-1: "TK-15-1-1 회전 단위 세분도 뷰, TK-15-1-2 STK-03 전용 페이지, TK-15-1-3 발행 SLA 부하 테스트"
> SRS REQ-FUNC-VC-017: "Candidate 확정 후 STK-03 시뮬뷰 페이지에 ≤ 2초 내 PUSH."

본 Story는 EP-05 GreedyRotationAllocator 결과를 React UI에 시각화. 회전 단위 (주간 8 + 야간 10 = 18회/일) × 1주 영업일 × LP 8슬롯 매트릭스. WebSocket으로 갱신 PUSH (EP-EX14 패턴 재사용).

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-15-1-1](TK-15-1-1.md) | 회전 단위 세분도 뷰 (React + AG Grid) | 1.0 | Frontend | T-U + T-I | ☐ |
| [TK-15-1-2](TK-15-1-2.md) | STK-03 전용 페이지 (`/simview`) + WebSocket 구독 | 0.7 | Frontend + Backend | T-U + T-I | ☐ |
| [TK-15-1-3](TK-15-1-3.md) | 발행 SLA p95 ≤ 2초 부하 테스트 | 0.5 | QA + Frontend | T-P + A | ☐ |

> **선행**: [EP-10](../../EP-10/), [EP-EX14](../../EP-EX14/) (WebSocket 패턴)
> **후행**: ST-15-2

---

## Story 레벨 DoD

- [ ] **`SimViewPage` React 컴포넌트** — AG Grid 매트릭스
- [ ] **회전 단위 grid**: row=회전 (주간1~8·야간1~10), column=일자, cell=hose_id + yield
- [ ] **`/simview` 라우트** — Keycloak 인증 + STK_USER role
- [ ] **WebSocket `/ws/vulcanization` 구독** — Candidate 확정 → grid 자동 갱신
- [ ] **발행 SLA**: confirm → UI display p95 ≤ 2초
- [ ] **부하 테스트** — 1주 분량 (≈ 1,500 row) 렌더링 검증
- [ ] 단위 + 통합 + 성능 테스트 ≥ 80%

---

## References

- **WBS**: §8 EP-15 ST-15-1
- **SRS REQ-FUNC**: REQ-FUNC-VC-017
- **SRS REQ-NF**: REQ-NF-PER-005 (UI p95 ≤ 1초)
- **TestPlan**: TC-VC-017

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
