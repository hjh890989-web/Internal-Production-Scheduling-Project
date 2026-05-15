# Epic Overview — [EP-44] 운영·관측성 NFR (Operations)

**Sprint**: S0~S2 + S4 | **Priority**: Must ⭐ | **SP**: 12 | **PD**: ~8.4 PD

## Epic 목적
> SRS §4.2.5 / REQ-NF-OPS-001~007: 구조화 로그·17 KPI 대시보드·Slack 알림·에스컬레이션·APM·audit 쿼리·NS-01 설문

## Story 목록

| Story | 제목 | SP | NFR |
|---|---|:--:|:--:|
| [ST-44-1](ST-44-1/_Story_Overview.md) | 구조화 JSON 로깅 + 90일 보존 | 2 | OPS-001 |
| [ST-44-2](ST-44-2/_Story_Overview.md) | 17 KPI + NS-01 대시보드 | 3 | OPS-002 |
| [ST-44-3](ST-44-3/_Story_Overview.md) | Slack 시스템 에러 알림 ≤ 60초 | 2 | OPS-003 |
| [ST-44-4](ST-44-4/_Story_Overview.md) | 에스컬레이션 정책 자동 | 2 | OPS-004 |
| [ST-44-5](ST-44-5/_Story_Overview.md) | 룰 엔진 APM | 1 | OPS-005 |
| [ST-44-6](ST-44-6/_Story_Overview.md) | Audit 쿼리 ≤ 5초 (3년) | 1 | OPS-006 |
| [ST-44-7](ST-44-7/_Story_Overview.md) | NS-01 분기 설문 계측 | 1 | OPS-007 |

## DoD
- [ ] logback JSON + Loki 90일
- [ ] Grafana 17 KPI + NS-01 대시보드
- [ ] Slack 인시던트 알림
- [ ] OpenTelemetry APM
- [ ] audit p95 ≤ 5초

## References
- WBS §8.5 EP-44, SRS OPS-001~007, ADR-014

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
