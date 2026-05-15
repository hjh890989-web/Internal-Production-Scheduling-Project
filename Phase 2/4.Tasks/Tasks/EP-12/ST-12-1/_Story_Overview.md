# Story Overview — [EP-12] ST-12-1 통합 마스터 → 원본 포맷 워크북 export

**Sprint**: S4 | **Epic**: EP-12 엑셀 역-Export | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §7 EP-12 ST-12-1: "TK-12-1-1 POI XSSF writer, TK-12-1-2 수식 보존, TK-12-1-3 셀-수준 차이 ≤ 2% 회귀"
> SRS REQ-FUNC-OC-013: "통합 마스터 → MASTER.xlsx export. 원본 포맷 호환."

본 Story는 PostgreSQL 통합 마스터 → POI XSSF 사용 원본 양식 엑셀 생성. 원본 파일의 수식·서식·열 너비·셀 병합 그대로 보존. EP-01 Parser와 round-trip 호환 (export → re-import 시 데이터 동일).

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-12-1-1](TK-12-1-1.md) | POI XSSF Writer + `GET /api/v1/export/master` | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-12-1-2](TK-12-1-2.md) | 수식 보존 (formula·cellType·styling) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-12-1-3](TK-12-1-3.md) | 셀-수준 차이 ≤ 2% 회귀 | 0.7 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-01](../../EP-01/)
> **후행**: ST-12-2

---

## Story 레벨 DoD

- [ ] **`MasterExcelExporter.export()`** — XSSFWorkbook 생성
- [ ] **API**: `GET /api/v1/export/master` (인증 사용자), Streaming ResponseEntity
- [ ] **원본 템플릿 활용** — 빈 워크북 from scratch 아닌 template.xlsx 로드 → 데이터 채움
- [ ] **수식 평가** — `XSSFFormulaEvaluator` 사용해 cached value 동기화
- [ ] **회귀 100 cells** — 원본 ↔ export 셀 비교 ≥ 98% 일치
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-12 ST-12-1
- **SRS REQ-FUNC**: REQ-FUNC-OC-013
- **TestPlan**: TC-OC-013

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
