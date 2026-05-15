# Story Overview — [EP-12] ST-12-2 압출 시트명 `*월*일(압출)` 매트릭스 export

**Sprint**: S4 | **Epic**: EP-12 엑셀 역-Export | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §7 EP-12 ST-12-2: "TK-12-2-1 매트릭스 뷰 → 시트 변환, TK-12-2-2 정규식 `\d+월\d+일\(압출\)` 일치, TK-12-2-3 BR-E09 정합"
> SRS REQ-FUNC-EX-018 / BR-E09: "압출 스케줄은 일자별 매트릭스 (일자×shift×라인) 형식으로 export. 시트명은 `{월}월{일}일(압출)`."

본 Story는 압출 스케줄을 현장 시트 양식으로 export. 일자별 시트 + 시트 안에 shift×라인 매트릭스. 시트명 정규식 강제 — Sprint 1 Parser가 이 시트들을 다시 import 시 동일 시트명 인식.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-12-2-1](TK-12-2-1.md) | 매트릭스 뷰 → 시트 변환 (`ExtrusionMatrixExporter`) | 0.6 | Backend | T-U + T-I | ☐ |
| [TK-12-2-2](TK-12-2-2.md) | 정규식 `\d+월\d+일(압출)` 시트명 강제 | 0.4 | Backend | T-U | ☐ |
| [TK-12-2-3](TK-12-2-3.md) | BR-E09 정합 회귀 (매트릭스 형식 + 라인 capa) | 0.4 | QA + Backend | T-I + A | ☐ |

> **선행**: [ST-12-1](../ST-12-1/_Story_Overview.md), [EP-09](../../EP-09/)
> **후행**: Sprint 5 UI 다운로드

---

## Story 레벨 DoD

- [ ] **`ExtrusionMatrixExporter`** — 일자별 시트 (호라이즌 분량)
- [ ] **시트명 정규식**: `\d+월\d+일\(압출\)` 100% 일치
- [ ] **매트릭스 구조**: row=shift code, column=line ID, cell=hose_id+yield
- [ ] **API `GET /api/v1/export/extrusion-matrix?from=&to=`** — 인증 사용자
- [ ] **BR-E09 정합**: 라인 capa 누계 ≤ effective_min
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-12 ST-12-2
- **SRS REQ-FUNC**: REQ-FUNC-EX-018
- **BR**: BR-E09
- **TestPlan**: TC-EX-018

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
