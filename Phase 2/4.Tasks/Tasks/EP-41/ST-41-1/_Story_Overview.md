# Story Overview — [EP-41] ST-41-1 영업시간 가용성 ≥ 99.5%

**Sprint**: S0+S4 | **Epic**: EP-41 | **Priority**: Must ⭐ | **SP**: 2

## Story 목적
> SRS REQ-NF-REL-001: "영업시간(월~금 07:00–22:00 KST) 가용성 ≥ 99.5%"

합성 프로브 + Grafana SLO 추적.

## 포함 Task

| Task | 제목 | PD | Owner |
|---|---|:--:|:--:|
| [TK-41-1-1](TK-41-1-1.md) | 합성 프로브 (Blackbox Exporter) | 0.5 | DevOps |
| [TK-41-1-2](TK-41-1-2.md) | 모니터링 알람 + Slack | 0.4 | DevOps |
| [TK-41-1-3](TK-41-1-3.md) | SLO 추적 대시보드 (99.5% 목표) | 0.4 | DevOps + QA |

## DoD
- [ ] Blackbox Exporter — `/health` 30초 마다
- [ ] Grafana SLO 추적 (월간 사용 가능 시간 / 영업시간)
- [ ] 99.5% 미달 시 Slack 알림

## References
- WBS §8.5 EP-41 ST-41-1, SRS REL-001

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
