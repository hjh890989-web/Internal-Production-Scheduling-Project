# Story Overview — [EP-11] ST-11-2 Audit 불변성 (UPDATE/DELETE 거부)

**Sprint**: S4 | **Epic**: EP-11 Audit 기록 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

---

## Story 목적

> WBS §7 EP-11 ST-11-2: "TK-11-2-1 `REVOKE UPDATE, DELETE ON audit.*`, TK-11-2-2 negative 테스트, TK-11-2-3 audit role 분리"
> SRS REQ-FUNC-CO-005 / NFR-SEC-004: "audit 테이블은 INSERT/SELECT만 허용. UPDATE/DELETE 시도 시 permission denied."

본 Story는 audit 데이터의 **법적 무결성** 보장. 트리거가 자동 INSERT 하지만, 사용자/admin이 audit를 변조할 수 없음. `audit_reader`·`audit_admin` role 계층 — 조회는 audit_reader, 관리(파티션 추가)는 audit_admin, 모두 UPDATE/DELETE 불가.

---

## 포함 Task 목록

| Task ID | 제목 | PD | Owner | 검증 | 상태 |
|---|---|:--:|:--:|:--:|:--:|
| [TK-11-2-1](TK-11-2-1.md) | `REVOKE UPDATE, DELETE ON audit.*` | 0.5 | Backend | T-I + A | ☐ |
| [TK-11-2-2](TK-11-2-2.md) | Audit 불변성 negative 테스트 회귀 | 0.5 | QA + Backend | T-I + A | ☐ |
| [TK-11-2-3](TK-11-2-3.md) | audit_reader·audit_admin role 분리 + 보존 정책 | 0.4 | Backend | T-I | ☐ |

> **선행**: [ST-11-1](../ST-11-1/_Story_Overview.md)
> **후행**: 없음 (Epic 마지막)

---

## Story 레벨 DoD

- [ ] **`audit.*` UPDATE/DELETE 권한 REVOKE** (app_user·planner_role·all)
- [ ] **`audit_reader` role** 신설 — SELECT만
- [ ] **DBA/superuser도 별도 정책** — `pg_audit_immutable` 검토 (Phase 2+ 옵션)
- [ ] **negative 테스트**: UPDATE/DELETE 시도 → permission denied 100%
- [ ] **TRUNCATE 차단**: `REVOKE TRUNCATE` 명시
- [ ] 통합 + negative 테스트 ≥ 5 케이스

---

## References

- **WBS**: §7 EP-11 ST-11-2
- **SRS REQ-FUNC**: REQ-FUNC-CO-005
- **NFR**: NFR-SEC-004

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §7 EP-11 ST-11-2 |
