# Story Overview — [EP-31] ST-31-2 Loki 로그 + Grafana 통합 + Slack 알림

**Sprint**: S0~S2 | **Epic**: EP-31 | **Priority**: Must ⭐⭐ | **SP**: 2 | **PD**: ~1.4 PD

## Story 목적
> WBS §10 EP-31 ST-31-2 / SRS OPS-001·003: "Loki promtail + Grafana datasource + Slack webhook 알림 룰"

## 포함 Task 목록

| Task | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-31-2-1](TK-31-2-1.md) | Loki + promtail (90일 보존) | 0.6 | DevOps |
| [TK-31-2-2](TK-31-2-2.md) | Grafana datasource Loki·Prometheus | 0.4 | DevOps |
| [TK-31-2-3](TK-31-2-3.md) | Slack webhook 알림 룰 (AlertManager) | 0.4 | DevOps |

> 선행: [ST-31-1](../ST-31-1/_Story_Overview.md)
> 후행: EP-44 (운영 NFR)

## Story 레벨 DoD
- [ ] Loki 컨테이너 + retention 90일
- [ ] Promtail Spring Boot 컨테이너 로그 수집
- [ ] Grafana datasource Loki·Prometheus·Tempo 통합
- [ ] AlertManager + Slack webhook
- [ ] 첫 alert rule (probe failure) PASS

## References
- WBS §10 EP-31 ST-31-2, SRS OPS-001·003, ADR-014

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
