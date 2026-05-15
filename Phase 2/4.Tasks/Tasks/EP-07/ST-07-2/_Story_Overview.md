# Story Overview — [EP-07] ST-07-2 영업일 캘린더 (월~금) — 압출 적용 + 주말 회귀

**Sprint**: S3 | **Epic**: EP-07 D-1 자동 역산 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §6 EP-07 ST-07-2: "TK-07-2-1 토·일 제외 캘린더, TK-07-2-2 주말 기한 → 금요일 이전 회귀, TK-07-2-3 단위 테스트"
> SRS REQ-FUNC-EX-002 / BR-E02 / CON-10: "압출 영업일은 월~금. EP-06과 동일 캘린더 단일."

본 Story는 EP-06 `WorkingCalendarService`를 압출 컨텍스트에 통합 + 압출 특화 회귀 (주말 vc_date → 금요일 이전 deadline) 검증. **재구현 금지** — CON-10에 따라 캘린더 마스터는 단일. EP-06이 정식 구현했으므로 본 Story는 통합·회귀·문서화 중심.

**Why Must**:
- 압출 D-1 deadline 계산의 정확성은 캘린더 의존 — 회귀로 명시적 검증
- CON-10 단일 캘린더 원칙 — 압출 모듈이 별도 캘린더 정의 금지 (ArchUnit 강제)
- 주말 vc_date edge case는 압출에서 더 빈번 (성형이 평일만 가동해도 압출은 1일 차감 시 주말 통과)

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-07-2-1](TK-07-2-1.md) | 압출 모듈 캘린더 통합 (EP-06 재사용 + ArchUnit) | 0.4 | Backend | T-U + T-I | ☐ |
| [TK-07-2-2](TK-07-2-2.md) | 주말 vc_date → 금요일 이전 deadline 회귀 | 0.5 | QA + Backend | T-I + A | ☐ |
| [TK-07-2-3](TK-07-2-3.md) | 단위 테스트 (압출 영업일 검증 ≥ 12) | 0.5 | Backend + QA | T-U | ☐ |

> **선행**: [EP-06 ST-06-1](../../EP-06/ST-06-1/_Story_Overview.md)
> **후행**: EP-08, EP-09

---

## Story 레벨 DoD

- [ ] **압출 모듈 캘린더 통합** — `WorkingCalendarService` 주입 + ArchUnit (압출이 별도 캘린더 정의 금지)
- [ ] **주말 vc_date 회귀**: vc_date 2026-03-07(토)·8(일) → deadline = 2026-03-06(금)
- [ ] **연휴 직후 vc_date 회귀**: 설날·추석 직후 vc_date deadline 정확
- [ ] **단위 테스트 ≥ 12 케이스** — 영업일 7요일 × 휴일 보유 여부 mix
- [ ] **CON-10 원칙 준수** — 압출 컨텍스트에서 별도 캘린더 정의 0건

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §6 EP-07 ST-07-2
- **PDD-03**: M-07 §4 A1
- **SRS REQ-FUNC**: REQ-FUNC-EX-002
- **BR**: BR-E02 (월~금)
- **CON**: CON-10 (캘린더 단일 마스터)
- **TestPlan**: TC-EX-002
- **선행**: EP-06 ST-06-1

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §6 EP-07 ST-07-2 |
