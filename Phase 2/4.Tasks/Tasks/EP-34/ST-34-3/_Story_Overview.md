# Story Overview — [EP-34] ST-34-3 KST 시간 기준 통일 (BR-X04)

**Sprint**: S2 | **Epic**: EP-34 | **SP**: 1 | **PD**: ~0.7 PD

## Story 목적
> BR-X04: "모든 timestamp는 KST (Asia/Seoul) 기준"

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-34-3-1](TK-34-3-1.md) | 모든 timestamp KST (Spring·DB·UI) | 0.4 |
| [TK-34-3-2](TK-34-3-2.md) | 경계 일자 단위 테스트 + ArchUnit | 0.3 |

## DoD
- [ ] Spring `LocaleResolver` + `TimeZone` Asia/Seoul 강제
- [ ] PostgreSQL `timezone = 'Asia/Seoul'`
- [ ] Frontend `Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul' })`
- [ ] ArchUnit 규칙 — `Instant.now()` 사용 금지 (clock injection 강제)
- [ ] 경계 일자 (영업일 boundary·납기 D-Day) 단위 테스트

## References
- WBS §10 EP-34 ST-34-3, BR-X04, REQ-FUNC-CO-007

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
