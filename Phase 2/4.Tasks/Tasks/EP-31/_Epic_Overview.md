# Epic Overview — [EP-31] 관측성 (Prometheus + Loki + Grafana — ADR-014) ⭐

**Sprint**: S0~S2 분산 | **Priority**: Must ⭐⭐ (Phase 3 진입 게이트) | **SP**: 5 | **PD**: ~3.5 PD

---

## Epic 목적

> WBS §10 EP-31 인용: "Prometheus + Spring Actuator 메트릭 + Loki 로그 + Grafana 통합 + Slack 알림"
> SAD ADR-014 / SRS REQ-NF-OPS-001~007: "모든 요청·이벤트 metric·log·trace + 17 KPI 대시보드 + Slack 알림 60초"

본 Epic은 **Phase 3 진입 결정적 인프라**. NFR EP-40 (성능)·EP-41 (신뢰성)·EP-44 (운영) 모두 선행 의존. Prometheus + Loki + Grafana 통합 스택. 17 KPI 대시보드 골격.

**Why P1 Critical**:
- **EP-40 PER-001~008** — Prometheus metric 의존
- **EP-41 REL-001** — Blackbox Exporter + alerting 의존
- **EP-44 OPS-001~007** — Loki 90일 + Grafana 대시보드 + Slack 알림 모두 의존

---

## Story 목록

| Story | 제목 | SP | PD | 검증 |
|---|---|:--:|:--:|:--:|
| [ST-31-1](ST-31-1/_Story_Overview.md) | Prometheus + Spring Actuator 메트릭 | 3 | ~2.1 | T-I + A |
| [ST-31-2](ST-31-2/_Story_Overview.md) | Loki 로그 + Grafana 통합 + Slack 알림 | 2 | ~1.4 | T-I + A |

---

## Epic 레벨 DoD

- [ ] **Prometheus + Spring Actuator** — 메트릭 scrape (HTTP·JVM·DB·custom)
- [ ] **Loki + Promtail** — 모든 컨테이너 JSON 로그 수집 (90일 보존)
- [ ] **Grafana 골격** — Prometheus·Loki·Tempo datasource + 17 KPI 대시보드 placeholder
- [ ] **AlertManager + Slack** — Critical 알림 1분 SLA
- [ ] **NFR-OPS-001~007 정합** — 모든 항목 측정 가능

---

## References

- **WBS**: `Phase 2/4.Tasks/TASK-001_WBS_v1.2.md` §10 EP-31
- **SAD ADR-014**: Prometheus + Loki + Grafana 채택
- **SRS REQ-NF**: OPS-001~007 (전 항목)
- **선행**: EP-00 (Docker Compose)
- **후행**: **EP-40 (성능)**, **EP-41 (신뢰성)**, **EP-44 (운영)** 모두 본 Epic 선행 의존

---

## 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-15 | (작성자) | 초안 — WBS §10 EP-31, ADR-014 + REQ-NF-OPS-001~007 |
