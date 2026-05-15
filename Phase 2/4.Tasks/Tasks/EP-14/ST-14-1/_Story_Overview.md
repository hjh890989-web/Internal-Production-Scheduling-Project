# Story Overview — [EP-14] ST-14-1 신규 우선 → 포드 폴백 라우팅

**Sprint**: S4 | **Epic**: EP-14 신규 라인 우선 라우팅 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §7 EP-14 ST-14-1: "TK-14-1-1 라우팅 정책 (신규 90%↑), TK-14-1-2 포드 전용 품번 차단 (zero 오라우팅), TK-14-1-3 라인 capa accounting"
> SRS REQ-FUNC-EX-008·009 / BR-E08

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-14-1-1](TK-14-1-1.md) | 라우팅 정책 (신규 90%↑) + line_type 마스터 | 0.8 | Backend | T-U + T-I | ☐ |
| [TK-14-1-2](TK-14-1-2.md) | 포드 전용 품번 차단 (zero 오라우팅) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-14-1-3](TK-14-1-3.md) | 라인 capa accounting + 회귀 (NS-S09 ≥ 90%) | 0.6 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-09](../../EP-09/)
> **후행**: 없음

---

## Story 레벨 DoD

- [ ] **`master.line_type`** — `line_id`, `type` (NEW/FORD), `priority`, `is_active`
- [ ] **`master.line_product_compatibility`** — 포드 전용 품번 매핑
- [ ] **`LineRoutingPolicy`** — 후보 라인 우선순위 결정
- [ ] **포드 전용 품번 → 신규 시도 0건** (Rule 차단)
- [ ] **NS-S09**: 1주 호라이즌 신규 라인 사용률 ≥ 90%
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-14 ST-14-1
- **SRS REQ-FUNC**: REQ-FUNC-EX-008·009
- **BR**: BR-E08
- **NS-S09**: 신규 라인 사용률

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 |
