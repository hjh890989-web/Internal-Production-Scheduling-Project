# Story Overview — [EP-41] ST-41-3 MES 장애 1-shift 회복

**Sprint**: S4 | **Epic**: EP-41 | **SP**: 3

## Story 목적
> SRS REL-004 / BR-X06: "MES 장애 1 shift 후 다음 shift 내 자동 재조정"

카오스 테스트 — MES 미수신 시뮬레이션 + 폴백.

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-41-3-1](TK-41-3-1.md) | 카오스 테스트 (MES 미수신 시뮬) | 1.0 |
| [TK-41-3-2](TK-41-3-2.md) | 임시 카운트 fallback | 0.8 |
| [TK-41-3-3](TK-41-3-3.md) | 자동 재조정 | 0.6 |

## DoD
- [ ] MES API mock + 의도적 장애
- [ ] 1 shift 미수신 시 임시값 사용
- [ ] 다음 shift 정상 수신 시 자동 보정
- [ ] BR-X06 회귀

## References
- WBS §8.5 EP-41 ST-41-3, SRS REL-004, BR-X06

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
