# Story Overview — [EP-41] ST-41-5 WebSocket 5초 재연결

**Sprint**: S4 | **Epic**: EP-41 | **SP**: 1

## Story 목적
> SRS REL-006: "끊긴 현장 패드는 5초 이내 재연결·재동기화"

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-41-5-1](TK-41-5-1.md) | 연결 테스트 자동화 | 0.3 |
| [TK-41-5-2](TK-41-5-2.md) | 재동기화 로직 | 0.3 |
| [TK-41-5-3](TK-41-5-3.md) | 회귀 | 0.2 |

## DoD
- [ ] STOMP `reconnectDelay: 5000` 설정
- [ ] 재연결 후 query invalidate 자동
- [ ] Playwright 회귀 (network offline 시뮬)

## References
- WBS §8.5 EP-41 ST-41-5, SRS REL-006

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
