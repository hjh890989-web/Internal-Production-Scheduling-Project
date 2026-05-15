# Story Overview — [EP-44] ST-44-1 구조화 JSON 로깅 + 90일 보존

**Sprint**: S0 | **Epic**: EP-44 | **SP**: 2

## Story 목적
> SRS OPS-001: "모든 요청·도메인 이벤트 JSON 구조화 로그, ≥ 90일 보존"

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-44-1-1](TK-44-1-1.md) | logback JSON 패턴 | 0.5 |
| [TK-44-1-2](TK-44-1-2.md) | Loki 90일 보존 | 0.5 |
| [TK-44-1-3](TK-44-1-3.md) | 스키마 리뷰 | 0.4 |

## DoD
- [ ] logback-spring.xml JSON encoder
- [ ] Loki retention 90일
- [ ] 로그 스키마 표준화 (traceId·spanId·userId·event)

## References
- WBS §8.5 EP-44 ST-44-1, SRS OPS-001
