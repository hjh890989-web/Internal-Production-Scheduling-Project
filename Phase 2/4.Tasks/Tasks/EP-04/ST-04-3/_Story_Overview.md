# Story Overview — [EP-04] ST-04-3 드래그앤드롭 위반 가드 (UI)

**Sprint**: S2 | **Epic**: EP-04 슬롯 O/X 검증 (M-04) | **Priority**: Must
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §5.2 EP-04 인용: "ST-04-3 — TK-04-3-1 dnd-kit 통합, TK-04-3-2 ≤1초 경고 + 저장 차단, TK-04-3-3 UAT 시나리오"
> SRS REQ-FUNC-VC-004 / REQ-NF-PER-006 인용: "플래너 웹 UI는 사용자가 비적합 (hose_id, 슬롯) 조합을 드래그할 때 1초 이내 경고 아이콘을 표시하고 저장을 차단해야 한다. UI 응답 중앙값 ≤1초, 감지율 100%."

본 Story는 **EP-04의 사용자 가시 마감재**. 플래너 웹 UI(P1·P4)의 간트차트·매트릭스 뷰에서 사용자가 마우스 드래그로 (품번, 슬롯) 배치 시도 → 매트릭스 사전 검증 → 비적합 시 즉시 경고 + 저장 차단. INT-4 사건(P4 대리의 IC/저압 혼동)의 1차 방어선.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-04-3-1](TK-04-3-1.md) | dnd-kit 통합 + 매트릭스 클라이언트 캐시 | 0.5 | Frontend | T-I + I | ☐ |
| [TK-04-3-2](TK-04-3-2.md) | ≤1초 경고 + 저장 차단 + 사유 모달 | 0.6 | Frontend | T-UAT + T-L | ☐ |
| [TK-04-3-3](TK-04-3-3.md) | UAT 시나리오 + Playwright E2E | 0.3 | QA + Frontend | T-UAT | ☐ |

> **선행 의존**: [TK-04-1-3](../ST-04-1/TK-04-1-3.md) (compat API), [ST-00-3](../../EP-00/ST-00-3/_Story_Overview.md) (React Vite)
> **후행 차단**: 없음 (UI 최종)

---

## Story 레벨 DoD

- [ ] **dnd-kit 드래그앤드롭** 정상 작동
- [ ] **비적합 드래그 ≤1초 차단** (REQ-NF-PER-006)
- [ ] **위반 감지율 100%** — 매트릭스 모든 false 셀
- [ ] **override 모달** — 사유 입력 강제 (REQ-FUNC-CO-010)
- [ ] **한국어 메시지** 100% (NFR-USA-003)
- [ ] Playwright E2E PASS
- [ ] UAT 시나리오 (P4 페르소나) PASS

---

## References

- **WBS**: §5.2 EP-04 ST-04-3
- **SAD**: §5.2 Frontend — dnd-kit
- **SRS REQ-FUNC**: REQ-FUNC-VC-004
- **SRS REQ-NF**: REQ-NF-PER-006 (≤1초), REQ-NF-USA-002·003
- **TestPlan**: TC-VC-004 (드래그 차단)
- **연관**: 선행 [TK-04-1-3](../ST-04-1/TK-04-1-3.md), [ST-00-3](../../EP-00/ST-00-3/_Story_Overview.md)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §5.2 EP-04 ST-04-3 + REQ-FUNC-VC-004 + REQ-NF-PER-006 |
