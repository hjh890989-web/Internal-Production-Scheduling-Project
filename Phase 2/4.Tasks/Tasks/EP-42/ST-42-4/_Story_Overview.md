# Story Overview — [EP-42] ST-42-4 Audit 3년 보존·불변성

**Sprint**: S4 | **Epic**: EP-42 | **SP**: 2

## Story 목적
> SRS SEC-004: "audit ≥ 3년 보존, UPDATE/DELETE 금지"

EP-11 ST-11-2 (Audit 불변성) 정합 + 3년 partition 정책.

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-42-4-1](TK-42-4-1.md) | REVOKE UPDATE/DELETE (이미 EP-11에 부분 구현) | 0.4 |
| [TK-42-4-2](TK-42-4-2.md) | Audit role 분리 (audit_reader·audit_admin) | 0.4 |
| [TK-42-4-3](TK-42-4-3.md) | 3년 파티션 정책 (EP-11 ST-11-2 참조) | 0.4 |

## DoD
- [ ] EP-11 ST-11-2 정합 — REVOKE 모든 audit table
- [ ] audit_reader·audit_admin role
- [ ] 3년 partition (PARTITION BY RANGE)
- [ ] 보안팀 감사 PASS

## References
- WBS §8.5 EP-42 ST-42-4, SRS SEC-004
- 선행: EP-11 ST-11-2 (이미 구현, 본 Epic 검증 측면)

## 개정 이력
| 1.0 | 2026-05-15 | 초안 (EP-11 정합 검증) |
