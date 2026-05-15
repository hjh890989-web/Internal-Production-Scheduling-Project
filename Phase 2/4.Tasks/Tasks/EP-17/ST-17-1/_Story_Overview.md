# Story Overview — [EP-17] ST-17-1 매트릭스 뷰 (AG Grid) + Gantt

**Sprint**: S5 | **Epic**: EP-17 매트릭스 뷰 | **Priority**: Should ⭐
**SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-17-1-1](TK-17-1-1.md) | Frappe Gantt 통합 (압출 timeline) | 1.2 | Frontend | T-U + T-I | ☐ |
| [TK-17-1-2](TK-17-1-2.md) | AG Grid 매트릭스 (shift×라인×일자) | 1.5 | Frontend | T-U + T-I | ☐ |
| [TK-17-1-3](TK-17-1-3.md) | Export 시트명 정규식 일치 회귀 | 0.8 | QA + Frontend | T-I + A | ☐ |

> **선행**: [EP-12](../../EP-12/), [EP-EX14](../../EP-EX14/), [EP-15](../../EP-15/) (패턴)
> **후행**: EP-E2E

---

## Story 레벨 DoD

- [ ] **`ExtrusionMatrixPage`** — 탭 (매트릭스·Gantt)
- [ ] **AG Grid**: row=shift label, column=일자, cell=`{line, hose, yield}[]`
- [ ] **Frappe Gantt**: 작업별 bar (start_time, duration_min, color=settingGroup)
- [ ] **`/extrusion-matrix` 라우트** STK_USER + PLANNER
- [ ] **`Download Excel` 버튼** — EP-12 export endpoint 호출
- [ ] **export 다운로드 후 시트명 검증**: `\d+월\d+일\(압출\)` 정규식 일치
- [ ] **WebSocket 갱신**: EP-EX14 채널 구독

---

## References

- **WBS**: §8 EP-17 ST-17-1
- **SRS REQ-FUNC**: REQ-FUNC-EX-018
- **BR**: BR-E09

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
