# Story Overview — [EP-13] ST-13-1 DB UNIQUE 제약 (당일 락 가드레일)

**Sprint**: S4 | **Epic**: EP-13 당일 락 | **Priority**: Must ⭐
**SP 합계**: 3 | **PD 추정**: ~2.1 PD

---

## Story 목적

> WBS §7 EP-13 ST-13-1: "TK-13-1-1 `VC_SCHEDULE UNIQUE (machine_id, slot_position, production_date, hose_id) DEFERRABLE INITIALLY DEFERRED`, TK-13-1-2 마이그레이션 시 사전 점검 SQL, TK-13-1-3 violation 시 사용자 친화 에러 매핑"
> SRS REQ-FUNC-VC-012 / ADR-016 / BR-V07: "당일 가류기 슬롯에 두 hose 동시 배치 차단 — DB 수준."

본 Story는 ADR-016의 4-layer 강제 중 1번째 (DB UNIQUE). RuleEngine bypass·수동 SQL·MES 동기화 등 모든 입력 경로에 대해 가드레일. DEFERRABLE INITIALLY DEFERRED — transaction 내 임시 위반 허용, commit 시점에 검증 (Allocator의 row swap 시나리오 대응).

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-13-1-1](TK-13-1-1.md) | UNIQUE 제약 마이그레이션 (DEFERRABLE) | 0.7 | Backend | T-I + A | ☐ |
| [TK-13-1-2](TK-13-1-2.md) | 마이그레이션 사전 점검 SQL (기존 데이터 위반 검출) | 0.7 | Backend | T-U + I | ☐ |
| [TK-13-1-3](TK-13-1-3.md) | UNIQUE violation 시 사용자 친화 에러 매핑 | 0.7 | Backend | T-U + T-I | ☐ |

> **선행**: [EP-05](../../EP-05/)
> **후행**: ST-13-2

---

## Story 레벨 DoD

- [ ] **V4_5 ALTER TABLE** — `UNIQUE (machine_id, slot_position, production_date, hose_id) DEFERRABLE INITIALLY DEFERRED`
- [ ] **사전 점검 SQL** — `SELECT ... HAVING COUNT(*) > 1` violation 검출
- [ ] **`@ControllerAdvice`** — `PSQLException` SQL state 23505 → `IntraDayConflictException` 변환
- [ ] **사용자 메시지**: "동일 가류기·슬롯·날짜에 다른 호스 배치 불가 (BR-V07)"
- [ ] **회귀**: 일중 교체 시도 → DB error 일관
- [ ] 단위 + 통합 테스트 ≥ 80%

---

## References

- **WBS**: §7 EP-13 ST-13-1
- **SRS REQ-FUNC**: REQ-FUNC-VC-012
- **SAD**: ADR-016
- **BR**: BR-V07
- **TestPlan**: TC-VC-012

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-13 ST-13-1 |
