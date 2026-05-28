# Sprint 23 진입 계획 — EP-MES-ADAPTER-1 (MES HTTP polling adapter Phase 1) v1.0

**작성일**: 2026-05-28 | **버전**: 1.0 | **상태**: Phase 4 네 번째 sprint 진입 권고안 (S22 완료 후)

> **참조**: [PHASE-4_STABILIZATION_v1.0 §3 S23](PHASE-4_STABILIZATION_v1.0.md) + [PLAN-SPRINT-17_EP-DAY-LOCK_v1.0](PLAN-SPRINT-17_EP-DAY-LOCK_v1.0.md) (MesShiftPort 인터페이스 baseline) + [SRS REQ-FUNC-CO-004](../../Phase%202/2.SRS/SRS-001_Production_Scheduling_System_v1.5.md) + [SRS REQ-NF-REL-004](../../Phase%202/2.SRS/SRS-001_Production_Scheduling_System_v1.5.md)

---

## 1. 목적

**Sprint 17 EP-DAY-LOCK 의 MesShiftPort stub 인터페이스 (JpaMesShiftPort) 위에 실 MES 시스템 HTTP polling adapter 추가 — Phase 5+ MQ/file adapter 도입 전 baseline 확보.**

| 항목 | Sprint 17 baseline | Sprint 23 활성 |
|---|---|---|
| MesShiftPort 구현체 | JpaMesShiftPort (stub, DB 만) | ✅ **HttpMesShiftAdapter** (REST GET `/api/mes/shift?...`) + JpaMesShiftPort 유지 (fallback) |
| MES polling | DegradedModeService snapshot 만 | ✅ **@Scheduled(fixedDelay=60s) MES polling** → reportProduction() 호출 |
| MES 실패 시뮬 | 없음 (mesEnabled=false default) | ✅ **WireMock IT 4 시나리오** (정상 / 5xx / timeout / 부분 응답) |
| 운영 가이드 | 없음 | ✅ **매뉴얼 v1.4 §3.5 IT_OPS MES adapter 설정 가이드** |

**Pre-Phase 의존 (Sprint 23 진입 전 필수):**
- 벤더 협의 — MES REST API spec 확보 (URL / 인증 / 응답 schema)
- 사내 IT — MES 네트워크 방화벽 허용 (사내 → MES 호출)

**활성 후 효과:**
- 실 MES 실적 자동 수신 → degraded mode 자동 해제 (Excel 폴백 빈도 ↓)
- Phase 5+ MQ adapter 진입 게이트 (HTTP baseline 위에서 adapter 교체)

---

## 2. Sprint 23 SP·기간

| Story | SP | 추정 PD |
|---|:--:|:--:|
| ST-MES-1 HttpMesShiftAdapter (REST GET + 인증 + DTO 파싱) | 2.0 | 1.0 |
| ST-MES-2 MES polling scheduler (@Scheduled 60s + reportProduction 호출) | 1.0 | 0.5 |
| ST-MES-3 WireMock IT 4 시나리오 (정상 / 5xx / timeout / 부분 응답) | 1.0 | 0.5 |
| ST-MES-4 DegradedModeService 통합 검증 (실 polling 시 해제) | 0.5 | 0.3 |
| ST-MES-5 매뉴얼 v1.4 §3.5 IT_OPS MES 가이드 | 0.5 | 0.2 |
| **합계** | **~5 SP** | **~2.5 PD** |

---

## 3. 의존성 DAG

```
Pre-Phase (벤더 API spec + 사내 방화벽)
    ↓
ST-MES-1 (HttpMesShiftAdapter) ──┐
                                 │
ST-MES-2 (Scheduled polling) ────┤
                                 ↓
ST-MES-3 (WireMock IT) ──────────→ ST-MES-4 (Degraded 통합)
                                              ↓
                                  ST-MES-5 (매뉴얼)
```

**병렬 윈도우:** ST-MES-1 ↔ ST-MES-2 (adapter vs scheduler 독립)

---

## 4. Story · Task 매트릭스

### ST-MES-1 — HttpMesShiftAdapter (2.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-1-1 | `application.yml` — `scheduling.mes.adapter=jpa\|http` config flag + `scheduling.mes.http.base-url=${MES_URL}` + `bearer-token=${MES_TOKEN}` + `timeout-seconds=10` | 0.3 |
| TK-MES-1-2 | HttpMesShiftAdapter implements MesShiftPort — RestClient GET `${base-url}/api/mes/shift?machine=&date=&shift_no=` + Bearer 헤더 + Resilience4j `@Retry(name=mes)` + `@CircuitBreaker(name=mes)` | 0.7 |
| TK-MES-1-3 | MesShiftResponse DTO record (machineId, shiftDate, shiftNo, plannedQty, actualQty, receivedAt) + Jackson 매핑 | 0.3 |
| TK-MES-1-4 | application.yml resilience4j — mes instance (3회 retry · 10초 timeout · 5회 실패 30s OPEN) | 0.2 |
| TK-MES-1-5 | Bean 분기 — `@ConditionalOnProperty(name = "scheduling.mes.adapter", havingValue = "http")` HttpMesShiftAdapter 활성, default jpa 유지 | 0.3 |
| TK-MES-1-6 | 통합 IT (MES disabled 시 fallback) — `scheduling.mes.adapter=jpa` 기본 동작 회귀 | 0.2 |

### ST-MES-2 — MES polling scheduler (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-2-1 | MesPollingService — `@Scheduled(fixedDelay=60s)` 모든 활성 vc_machine 머신별 GET 호출 → mesPort.reportProduction() | 0.5 |
| TK-MES-2-2 | polling 실패 시 LOG WARN + 다음 cycle 재시도 (circuit OPEN 동안 skip) | 0.2 |
| TK-MES-2-3 | IT — polling 1회 → mes_shift_event 1+ row 생성 (WireMock 정상 응답 시) | 0.3 |

### ST-MES-3 — WireMock IT 4 시나리오 (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-3-1 | WireMockMesIT 시나리오 1 — 정상 200 응답 → reportProduction() 호출 + mes_shift_event row 영속 | 0.3 |
| TK-MES-3-2 | 시나리오 2 — 5xx 5회 연속 → 3회 retry → fallback (mes_shift_event INSERT 없음) | 0.3 |
| TK-MES-3-3 | 시나리오 3 — timeout (sleep > 10s) → Resilience4j timeout → retry | 0.2 |
| TK-MES-3-4 | 시나리오 4 — 부분 응답 (machineId 없음) → DTO 파싱 fail → log + skip | 0.2 |

### ST-MES-4 — DegradedModeService 통합 검증 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-4-1 | IT — `scheduling.mes.adapter=http` + WireMock 정상 → MES polling 1회 → DegradedModeService snapshot 변화 (NORMAL → NORMAL stable) | 0.3 |
| TK-MES-4-2 | IT — WireMock 6h 응답 안 함 → DegradedModeService.pollAndPublish → MesDegradedModeChangedEvent 진입 publish | 0.2 |

### ST-MES-5 — 매뉴얼 v1.4 §3.5 IT_OPS MES 가이드 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-5-1 | USER_MANUAL_v1.4.md §3.5 — MES adapter 설정 (jpa vs http) + URL/Token 등록 + 트러블슈팅 (circuit OPEN 시 manual Excel 폴백) | 0.3 |
| TK-MES-5-2 | Grafana 패널 추가 — MES polling success rate / circuit state (notify-sprint18.json 확장) | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ `scheduling.mes.adapter=http` 설정 시 HttpMesShiftAdapter 활성
2. ✅ MesPollingService 60초 주기 자동 polling + WireMock 정상 응답 시 mes_shift_event INSERT
3. ✅ MES 5xx 실패 시 Resilience4j 3회 retry → circuit OPEN 30s → HALF_OPEN 자동 복귀
4. ✅ MES timeout (10s+) → polling skip + 다음 cycle 재시도
5. ✅ DegradedModeService 통합 — MES 정상 polling 시 NORMAL 유지 / 6h 미수신 시 진입 이벤트
6. ✅ 매뉴얼 v1.4 §3.5 IT_OPS MES 설정 절차

**비기능 DoD:**
1. ✅ ArchUnit GREEN (vc.mes 모듈 boundary)
2. ✅ Backend IT 신규 4 WireMock + 회귀 0
3. ✅ DEV/STG default `scheduling.mes.adapter=jpa` 유지 (PROD 만 http)
4. ✅ Resilience4j mes instance 별도 (Slack/Kakao 와 독립)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| 벤더 MES API spec 확정 지연 | S23 진입 불가 | Pre-Phase 1주 협의 시작. 미확정 시 mock API spec 으로 진행 (Phase 5+ 실 spec 적용 시 DTO 변경만) |
| 사내 방화벽 미허용 | MES 호출 실패 | IT_OPS 협의 — outbound port 허용 (HTTP 80/443 또는 사내 IP) |
| MES polling 60s 부하 (5 머신 × 4 shift = 20 호출/min) | DB connection pool 압박 | HikariCP active 모니터링 + 부하 시 polling interval 120s 조정 |
| WireMock 의존 충돌 (Sprint 20 도입 분) | IT 회귀 | 동일 wiremock dependency 재사용 (libs.versions.toml 통합) |
| MES adapter switch 시점 회귀 (jpa → http) | 운영 영향 | 단계적 — STG 1주 검증 → PROD switch (config flag 만) |

---

## 7. 작업 순서 추천

**Day 1** — Adapter + Scheduler:
1. TK-MES-1-1~6 (HttpMesShiftAdapter)
2. TK-MES-2-1~3 (Scheduler)

**Day 2** — IT + Degraded 통합:
3. TK-MES-3-1~4 (WireMock IT 4)
4. TK-MES-4-1~2 (Degraded)

**Day 3** — 문서 + DoD:
5. TK-MES-5-1~2 (매뉴얼 + Grafana)
6. **본 PC 시각 검증** — WireMock 정상 + 실패 시뮬 1회씩

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Service (vc.mes) | HttpMesShiftAdapter + MesPollingService + MesShiftResponse |
| Backend Config | application.yml mes.adapter flag + resilience4j mes instance |
| Backend IT | WireMockMesIT (4 cases) + DegradedModeMesIntegrationIT (2 cases) |
| Infra Grafana | notify-sprint18.json 확장 (MES polling success / circuit state) |
| Docs | USER_MANUAL_v1.4.md (§3.5 MES adapter 가이드) |

---

## 9. Sprint 23 후 다음 단계

**Sprint 24 (EP-OPS-FEEDBACK) 진입 조건:**
- ✅ DoD 10/10 충족
- ⏳ 베타 운영 1개월 데이터 누적 (Sprint 23 종료 시점 = 베타 4주차)

**Phase 5+ carry-over:**
- MES MQ adapter (RabbitMQ / Kafka — 벤더 결정 후)
- MES file adapter (legacy 호환 — CSV / Excel polling)

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 네 번째 sprint EP-MES-ADAPTER-1 5 Story / 17 Task / ~5 SP 분해. Sprint 17 MesShiftPort baseline 위 HTTP polling adapter Phase 1. Pre-Phase 벤더 API spec + 사내 방화벽 협의. DoD 10 + 리스크 5. WireMock dependency Sprint 20 재사용. Phase 5+ MQ/file adapter carry-over. |
