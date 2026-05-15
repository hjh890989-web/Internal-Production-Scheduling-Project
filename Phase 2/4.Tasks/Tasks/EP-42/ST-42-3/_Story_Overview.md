# Story Overview — [EP-42] ST-42-3 RBAC 전 API 강제

**Sprint**: S0+S4 | **Epic**: EP-42 | **SP**: 3

## Story 목적
> SRS SEC-003: "RBAC 매트릭스를 모든 API에서 강제"

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-42-3-1](TK-42-3-1.md) | Spring Security 필터 | 0.8 |
| [TK-42-3-2](TK-42-3-2.md) | 모든 엔드포인트 403 negative | 0.8 |
| [TK-42-3-3](TK-42-3-3.md) | 침투 테스트 (OWASP ZAP) | 0.5 |

## DoD
- [ ] `@PreAuthorize` 모든 controller method
- [ ] ArchUnit 규칙 — RBAC 어노테이션 강제
- [ ] 모든 API negative 403 검증
- [ ] OWASP ZAP scan 통과

## References
- WBS §8.5 EP-42 ST-42-3, SRS SEC-003

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
