# Epic Overview — [EP-15] 성형 현장 시뮬뷰 (S-03)

**Sprint**: S5 (UI·확장·E2E) | **Priority**: Should ⭐ | **SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Epic 목적

> WBS §8 EP-15 인용: "ST-15-1 Candidate → 시뮬뷰 ≤2초 발행, ST-15-2 현장 피드백 1클릭 수용 채널"
> SRS REQ-FUNC-VC-017·018: "Candidate 스케줄을 회전 단위로 현장 시뮬뷰 페이지에 ≤2초 발행. 현장 피드백 순서 조정 1클릭 수용 (총량 보존)."

본 Epic은 **사용자 ↔ 시스템 양방향 UX 핵심**. EP-05 회전 배치 결과를 현장 작업자가 시각화 + 1클릭으로 순서 조정 제안 가능. STK-03 (현장 패드) 전용 페이지 — 큰 화면 + 단순 조작.

**Why Sprint 5 핵심**:
- **사용자 인터뷰 INT-2 직접 대응** — "후보가 만들어졌는지 현장에서 즉시 확인"
- **REQ-FUNC-VC-017·018** — 시뮬뷰 + 피드백 양방향
- **NFR-USA-001·003** — UI p95 ≤ 1초, 한국어 UI 100%

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-15-1](ST-15-1/_Story_Overview.md) | Candidate → 시뮬뷰 ≤ 2초 발행 | 3 | ~2.1 | T-U + T-I + T-P | ☐ |
| [ST-15-2](ST-15-2/_Story_Overview.md) | 현장 피드백 1클릭 수용 채널 | 2 | ~1.4 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **회전 단위 세분도 뷰** — LP 8슬롯 × 회전·shift × 5 영업일 (1주 호라이즌)
- [ ] **STK-03 전용 페이지** — `/simview` (사용자/STK 인증)
- [ ] **발행 SLA p95 ≤ 2초** — Candidate confirmed → 뷰 갱신
- [ ] **순서 조정 UI** — 드래그앤드롭 또는 1클릭 swap
- [ ] **총량 보존** — 회전·shift별 합계 변경 0 (단순 순서만)
- [ ] 단위 + 통합 + 성능 테스트 ≥ 80%

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §8 EP-15
- **PDD-02 v1.2**: §5 S-03 시뮬뷰
- **SRS REQ-FUNC**: REQ-FUNC-VC-017·018
- **SRS REQ-NF**: REQ-NF-USA-001·003, REQ-NF-PER-005
- **TestPlan**: TC-VC-017·018
- **선행**: EP-10 (확정 게이트), EP-EX14 (WebSocket PUSH 패턴)
- **후행**: EP-E2E (시뮬레이션 입력)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §8 EP-15 + REQ-FUNC-VC-017·018 |
