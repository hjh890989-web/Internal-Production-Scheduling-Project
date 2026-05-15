# Story Overview — [EP-11] ST-11-1 DB 트리거 기반 audit 강제

**Sprint**: S4 | **Epic**: EP-11 Audit 기록 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §7 EP-11 ST-11-1: "TK-11-1-1 trigger 함수, TK-11-1-2 `@Auditable` AOP, TK-11-1-3 audit 없는 커밋 차단 통합"
> SRS REQ-FUNC-VC-020·EX-020·CO-005·006 / BR-X02

본 Story는 audit 강제의 **자동화 두 layer** — (1) DB 트리거 — application bypass 시에도 audit 생성, (2) AOP `@Auditable` — actor·reason 자동 캡쳐. 트리거가 fallback (actor='system' if AOP context 없음).

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-11-1-1](TK-11-1-1.md) | `audit_vc_schedule()`/`audit_ex_schedule()`/`audit_order()` 트리거 함수 | 0.8 | Backend | T-U + I | ☐ |
| [TK-11-1-2](TK-11-1-2.md) | `@Auditable` AOP 결합 (actor·reason 자동 캡쳐) | 0.7 | Backend | T-U + T-I | ☐ |
| [TK-11-1-3](TK-11-1-3.md) | audit 없는 커밋 차단 통합 테스트 + 회귀 | 0.6 | QA + Backend | T-I + A | ☐ |

> **선행**: [EP-10](../../EP-10/)
> **후행**: ST-11-2

---

## Story 레벨 DoD

- [ ] **`audit.vc_schedule_log`·`audit.ex_schedule_log`·`audit.order_log`** 테이블 3종
- [ ] **트리거 함수** AFTER INSERT/UPDATE/DELETE — JSONB diff·actor·reason 기록
- [ ] **`@Auditable("reason")` AOP** — controller 호출 시 ThreadLocal context 주입
- [ ] **변경 100% audit row 생성** 검증 (1주 호라이즌 회귀)
- [ ] **actor fallback**: AOP context 없음 → 'system'
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-11 ST-11-1
- **SRS REQ-FUNC**: VC-020, EX-020, CO-005·006
- **BR**: BR-X02
- **TestPlan**: TC-VC-020·EX-020·CO-005

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-11 ST-11-1 |
