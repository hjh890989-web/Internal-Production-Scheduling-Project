# Epic Overview — [EP-12] 엑셀 역-Export (M-12)

**Sprint**: S4 | **Priority**: Must ⭐ | **SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Epic 목적

> WBS §7 EP-12 인용: "ST-12-1 통합 마스터 → 원본 포맷 워크북 export, ST-12-2 압출 시트명 `*월*일(압출)` 매트릭스 export"
> SRS REQ-FUNC-OC-013·EX-018 / BR-E09: "스케줄 데이터를 원본 엑셀 포맷(MASTER 통합 + 압출 매트릭스 시트명 정규식)으로 export. 셀-수준 차이 ≤ 2%."

본 Epic은 현장 사용자가 익숙한 엑셀 양식으로 다시 export — Excel-first 운영 환경 호환성 핵심. POI XSSF 사용. 원본 수식·서식 보존. 압출 매트릭스는 `\d+월\d+일(압출)` 시트명 정규식 일치.

**Why Sprint 4 핵심**:
- **현장 호환성** — Sprint 1 (EP-01) Parser 입력 ↔ 본 Epic Export 양방향
- **REQ-FUNC-OC-013·EX-018** — Excel 양식 정확성
- **BR-E09** — 압출 시트명 규약 준수

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-12-1](ST-12-1/_Story_Overview.md) | 통합 마스터 → 원본 포맷 워크북 export | 3 | ~2.1 | T-U + T-I + A | ☐ |
| [ST-12-2](ST-12-2/_Story_Overview.md) | 압출 시트명 `*월*일(압출)` 매트릭스 export | 2 | ~1.4 | T-U + T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **POI XSSF Writer** — 통합 마스터 → MASTER.xlsx
- [ ] **수식 보존** — 원본 파일의 수식 (`=A1+B1`) 그대로 유지
- [ ] **셀-수준 차이 ≤ 2%** — 원본 ↔ export 비교 회귀
- [ ] **압출 시트명** `\d+월\d+일(압출)` 정규식 일치
- [ ] **BR-E09 정합** — 매트릭스 형식 (일자×shift×라인)
- [ ] **API** `GET /api/v1/export/master`·`/api/v1/export/extrusion-matrix`
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-12
- **PDD-04**: M-12 Excel Export
- **SRS REQ-FUNC**: OC-013, EX-018
- **BR**: BR-E09 (압출 시트명 규약)
- **TestPlan**: TC-OC-013, TC-EX-018
- **선행**: EP-01 (Parser — input 양식 참조), EP-05 (VC 스케줄), EP-09 (EX 스케줄)
- **후행**: Sprint 5 (UI 다운로드 통합)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-12 |
