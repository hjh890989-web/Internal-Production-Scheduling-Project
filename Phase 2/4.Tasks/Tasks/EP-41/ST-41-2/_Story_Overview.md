# Story Overview — [EP-41] ST-41-2 ACID + 오류율 ≤ 0.1%

**Sprint**: S0+S4 | **Epic**: EP-41 | **SP**: 2

## Story 목적
> SRS REL-002·003: "모든 커밋 ACID, 부분 커밋 0건; 오류율 ≤ 0.1%"

Sentry 에러 트래커 + 부분 커밋 negative 테스트.

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-41-2-1](TK-41-2-1.md) | 트랜잭션 경계 검증 | 0.5 |
| [TK-41-2-2](TK-41-2-2.md) | Sentry 에러 트래커 통합 | 0.5 |
| [TK-41-2-3](TK-41-2-3.md) | 부분 커밋 negative 테스트 | 0.4 |

## DoD
- [ ] `@Transactional` 모든 변경 메서드
- [ ] Sentry 에러 capture + Slack 알림
- [ ] 부분 커밋 시도 → rollback 검증

## References
- WBS §8.5 EP-41 ST-41-2, SRS REL-002·003

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
