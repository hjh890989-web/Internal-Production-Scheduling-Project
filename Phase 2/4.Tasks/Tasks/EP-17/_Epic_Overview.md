# Epic Overview — [EP-17] 일자×shift×라인 매트릭스 뷰 (S-05)

**Sprint**: S5 | **Priority**: Should ⭐ | **SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Epic 목적

> WBS §8 EP-17 인용: "ST-17-1 매트릭스 뷰 (AG Grid) + Gantt. TK-17-1-1 Frappe Gantt 통합, TK-17-1-2 AG Grid 매트릭스, TK-17-1-3 export 시트명 정규식 일치"
> SRS REQ-FUNC-EX-018 / BR-E09: "압출 스케줄 (일자×shift×라인) 매트릭스 뷰 + 시트명 정규식 `\d+월\d+일(압출)` 일치."

본 Epic은 **압출 데일리 작업지시** 통합 뷰. EP-12 엑셀 export와 동일 형식 — 4 shift × N 라인 × N 영업일. Frappe Gantt로 시간축 시각화, AG Grid로 매트릭스. EP-15와 페어 (성형은 회전 매트릭스, 압출은 shift 매트릭스).

**Why Sprint 5**:
- **REQ-FUNC-EX-018 / BR-E09** 정합 — Export 양식과 UI 일치
- **현장 작업지시 UI** — 압출 작업자도 시뮬뷰 접근 (STK-03 압출 패드)
- **시각화 다양성** — 매트릭스 + Gantt (시간축 직관성)

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-17-1](ST-17-1/_Story_Overview.md) | 매트릭스 뷰 (AG Grid) + Gantt | 5 | ~3.5 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **`ExtrusionMatrixPage`** — AG Grid 매트릭스 + Frappe Gantt
- [ ] **`/extrusion-matrix` 라우트** (STK_USER + PLANNER)
- [ ] **AG Grid**: row=shift, column=영업일, cell=라인별 hose+yield
- [ ] **Frappe Gantt**: 작업별 timeline (수평 막대) — shift 단위
- [ ] **export 시트명 정규식 일치** — `\d+월\d+일(압출)` (EP-12 정합)
- [ ] **WebSocket 갱신** — EP-EX14 패턴
- [ ] 단위 + 통합 + 시각 회귀 테스트 ≥ 80%

---

## References

- **WBS**: §8 EP-17
- **PDD**: S-05 매트릭스 뷰
- **SRS REQ-FUNC**: REQ-FUNC-EX-018
- **BR**: BR-E09
- **TestPlan**: TC-EX-018
- **선행**: EP-12 (Export 양식 정합), EP-EX14 (PUSH)
- **후행**: EP-E2E

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
