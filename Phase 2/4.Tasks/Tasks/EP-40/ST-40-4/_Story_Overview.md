# Story Overview — [EP-40] ST-40-4 UI·드래그앤드롭·인지 RT SLO

**Sprint**: S5 | **Epic**: EP-40 | **Priority**: Must ⭐
**SP 합계**: 2 | **PD 추정**: ~1.4 PD

## Story 목적
> SRS REQ-NF-PER-005·006·008: UI p95 ≤ 1초, 드래그앤드롭 ≤ 1초 위반 피드백, 인지 RT p95 ≤ 1초

EP-15·17·18 (UI Epic) 의 RUM (Real User Monitoring) 측정.

## 포함 Task 목록

| Task ID | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-40-4-1](TK-40-4-1.md) | RUM 통합 (Sentry Performance / Datadog RUM) | 0.5 | Frontend + DevOps |
| [TK-40-4-2](TK-40-4-2.md) | UI p95 ≤ 1초 측정 + 회귀 | 0.4 | QA |
| [TK-40-4-3](TK-40-4-3.md) | 드래그앤드롭 ≤ 1초 위반 피드백 | 0.3 | Frontend + QA |
| [TK-40-4-4](TK-40-4-4.md) | 인지 RT ≤ 1초 (FCP·LCP) | 0.2 | QA |

> 선행: [EP-15](../../EP-15/), [EP-17](../../EP-17/)

## DoD
- [ ] RUM 통합 (Sentry 또는 Datadog)
- [ ] UI 페이지별 p95 ≤ 1,000ms
- [ ] 드래그앤드롭 응답 ≤ 1,000ms
- [ ] FCP·LCP ≤ 1,000ms (Core Web Vitals)
- [ ] Grafana 패널

## References
- WBS §8.5 EP-40 ST-40-4, SRS PER-005·006·008

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
