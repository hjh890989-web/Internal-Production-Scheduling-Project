# Epic Overview — [EP-41] 신뢰성·가용성 NFR (Reliability)

**Sprint**: S0+S4+S5 | **Priority**: Must ⭐ | **SP 합계**: 10 | **PD 추정**: ~7 PD

## Epic 목적

> SRS §4.2.2 / REQ-NF-REL-001~006: 가용성 99.5%, ACID, MES 폴백, 백업·RPO·RTO, WebSocket 재연결.

5 Story로 분해 — 영업시간 가용성·ACID·MES 장애 회복·백업·재연결.

## Story 목록

| Story | 제목 | SP | NFR |
|---|---|:--:|:--:|
| [ST-41-1](ST-41-1/_Story_Overview.md) | 영업시간 가용성 ≥ 99.5% | 2 | REL-001 |
| [ST-41-2](ST-41-2/_Story_Overview.md) | ACID + 오류율 ≤ 0.1% | 2 | REL-002·003 |
| [ST-41-3](ST-41-3/_Story_Overview.md) | MES 장애 1-shift 회복 | 3 | REL-004 / BR-X06 |
| [ST-41-4](ST-41-4/_Story_Overview.md) | 백업·RPO 24h·RTO 4h | 2 | REL-005 |
| [ST-41-5](ST-41-5/_Story_Overview.md) | WebSocket 5초 재연결 | 1 | REL-006 |

## DoD
- [ ] 합성 프로브 + Grafana SLO 대시보드
- [ ] Sentry 에러 트래커 + 부분 커밋 negative 테스트
- [ ] MES 카오스 테스트 (1-shift 미수신 시뮬)
- [ ] pg_basebackup 자동화 + WAL 아카이브 + STG DR 드릴
- [ ] WebSocket 끊김 → 5초 재연결 검증

## References
- WBS §8.5 EP-41
- SRS REQ-NF-REL-001~006
- 선행: EP-00, EP-31, EP-33

## 개정 이력
| 1.0 | 2026-05-15 | 초안 |
