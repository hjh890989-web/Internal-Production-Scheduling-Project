# Epic Overview — [EP-11] Audit 기록 (M-11)

**Sprint**: S4 | **Priority**: Must ⭐ | **SP 합계**: 5 | **PD 추정**: ~3.5 PD

---

## Epic 목적

> WBS §7 EP-11 인용: "ST-11-1 DB 트리거 기반 audit 강제 (모든 변경), ST-11-2 Audit 불변성 (UPDATE/DELETE 거부)"
> SRS REQ-FUNC-VC-020·EX-020·CO-005·006 / BR-X02 / NFR-SEC-004: "모든 schedule·order 변경은 audit row 생성, audit는 UPDATE/DELETE 불가."

본 Epic은 거버넌스의 두 번째 축 (EP-10 사용자 확정 ↔ EP-11 audit 추적성). DB 트리거로 application-level bypass 차단. AOP `@Auditable` 결합으로 actor/reason 자동 캡쳐. audit 테이블 자체는 INSERT/SELECT만 허용 (3년 보존, NFR-SEC-004).

**Why Sprint 4 핵심**:
- **BR-X02 hard** — audit 없는 변경 100% 차단
- **NFR-SEC-004** — 규제 대응 (3년 보존, 무결성)
- **EP-13 (당일 락)·EP-EX13 (트리거)** 모두 audit 입력 — 신뢰성 기반

---

## Story 목록

| Story | 제목 | SP | PD | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [ST-11-1](ST-11-1/_Story_Overview.md) | DB 트리거 기반 audit 강제 (모든 변경) | 3 | ~2.1 | T-U + T-I + A | ☐ |
| [ST-11-2](ST-11-2/_Story_Overview.md) | Audit 불변성 (UPDATE/DELETE 거부) | 2 | ~1.4 | T-I + A | ☐ |

---

## Epic 레벨 DoD

- [ ] **트리거 함수**: `audit_vc_schedule()`·`audit_ex_schedule()`·`audit_order()` 3종
- [ ] **`audit.*` 테이블**: 변경 종류 (INSERT/UPDATE/DELETE)·old/new JSONB·actor·reason
- [ ] **`@Auditable` AOP** — Spring controller 호출 시 reason 자동 캡쳐
- [ ] **audit 없는 커밋 차단** — 트리거 위반 시 SQL exception
- [ ] **UPDATE/DELETE 거부** — `REVOKE` + `audit_role` 분리
- [ ] **3년 보존** — 파티셔닝 (월별)
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-11
- **PDD-04**: M-11 Audit
- **SRS REQ-FUNC**: REQ-FUNC-VC-020·EX-020·CO-005·006
- **BR**: BR-X02
- **NFR**: NFR-SEC-004 (3년 보존·무결성)
- **TestPlan**: TC-VC-020·EX-020·CO-005
- **선행**: EP-10
- **후행**: EP-12·13·EX13 (모두 audit 사용)

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-11 |
