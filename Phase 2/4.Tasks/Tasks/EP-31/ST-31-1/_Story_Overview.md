# Story Overview — [EP-31] ST-31-1 Prometheus + Spring Actuator 메트릭

**Sprint**: S0 | **Epic**: EP-31 | **Priority**: Must ⭐⭐ | **SP**: 3 | **PD**: ~2.1 PD

## Story 목적
> WBS §10 EP-31 ST-31-1 / SRS OPS-001·002: "Prometheus + Spring Actuator + 17 KPI 대시보드 골격"

## 포함 Task 목록

| Task | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-31-1-1](TK-31-1-1.md) | Spring Actuator metric 노출 (Prometheus format) | 0.7 | Backend |
| [TK-31-1-2](TK-31-1-2.md) | Prometheus scrape + retention | 0.7 | DevOps |
| [TK-31-1-3](TK-31-1-3.md) | 17 KPI Grafana 대시보드 골격 | 0.7 | DevOps + QA |

> 선행: [TK-00-1-1](../../EP-00/ST-00-1/TK-00-1-1.md)
> 후행: ST-31-2, EP-40·44

## Story 레벨 DoD
- [ ] Actuator `/actuator/prometheus` 노출
- [ ] Prometheus scrape interval 15초
- [ ] Retention 30일 (기본) — 장기는 Loki 활용
- [ ] 17 KPI 대시보드 골격 (panel 자리만)
- [ ] JVM·HTTP·DB·custom metric 모두 export

## References
- WBS §10 EP-31 ST-31-1, SRS OPS-001·002, ADR-014

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
