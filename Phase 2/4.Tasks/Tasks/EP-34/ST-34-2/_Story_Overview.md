# Story Overview — [EP-34] ST-34-2 MES 실적 수신 + 장애 폴백 (BR-X06)

**Sprint**: S3~S4 | **Epic**: EP-34 | **SP**: 2 | **PD**: ~1.4 PD

## Story 목적
> BR-X06: "MES 실적 회전·shift 단위 수신 + 1 shift 미수신 시 임시 카운트 폴백 + 자동 재조정"

EP-41 ST-41-3 (MES 장애 카오스)와 정합 — 본 Story가 구현 측, EP-41이 검증 측.

## 포함 Task

| Task | 제목 | PD |
|---|---|:--:|
| [TK-34-2-1](TK-34-2-1.md) | 회전·shift 실적 수신 API | 0.5 |
| [TK-34-2-2](TK-34-2-2.md) | 1 shift 미수신 시 임시값 (Fallback) | 0.5 |
| [TK-34-2-3](TK-34-2-3.md) | 정상 수신 후 자동 재조정 | 0.4 |

## DoD
- [ ] `MesActualReceiver` REST endpoint
- [ ] `mes_actual` 테이블 + status (RECEIVED·ESTIMATED·MISSING)
- [ ] 1 shift 미수신 → 직전 shift 평균 폴백
- [ ] 정상 수신 후 ESTIMATED → 실제값 자동 갱신
- [ ] @Auditable

## References
- WBS §10 EP-34 ST-34-2, BR-X06, REQ-FUNC-CO-005
- 정합: [TK-41-3-1·2·3](../../EP-41/ST-41-3/TK-41-3-1.md) (NFR 측면)

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
