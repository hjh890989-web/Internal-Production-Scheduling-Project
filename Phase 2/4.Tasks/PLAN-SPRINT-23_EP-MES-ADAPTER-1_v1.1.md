# Sprint 23 진입 계획 — EP-MES-ADAPTER-1 (MES HTTP polling adapter Phase 1) v1.1

**작성일**: 2026-05-29 | **버전**: 1.1 | **상태**: Phase 4 네 번째 sprint 진입 권고안 (S22 완료 직후 — 실 코드 대조 갱신)

> **참조**: [v1.0 (2026-05-28 초안)](PLAN-SPRINT-23_EP-MES-ADAPTER-1_v1.0.md) + [PHASE-4_STABILIZATION_v1.1 §3 S23](PHASE-4_STABILIZATION_v1.1.md) + [MesShiftPort (Sprint 17 baseline)](../../backend/vc/src/main/java/com/scheduling/vc/mes/MesShiftPort.java) + SRS REQ-FUNC-CO-004 / REQ-NF-REL-004

---

## 0. v1.0 → v1.1 변경 요약 (실 코드베이스 검증 반영)

v1.0 은 S20/S22 진입 전 작성 — 2026-05-29 실 코드 대조 후 정정:

| 항목 | v1.0 가정 | v1.1 정정 (검증) |
|---|---|---|
| **WireMock** | "Sprint 20 도입 분 재사용" | ❌ 백엔드 전체에 WireMock 의존·사용 **0건** (S20 ST-EXT-3 deferred). → S23 가 **신규 도입** (libs.versions.toml + vc/app test 의존). ST-MES-3 scope + |
| **Http adapter 위치** | "HttpMesShiftAdapter implements MesShiftPort" (bean swap) | ⚠️ MesShiftPort.reportProduction 은 **로컬 영속**(Excel 폴백·degraded 공용) — adapter 로 swap 시 회귀 위험. → **MesShiftPort(영속, Jpa 무변경) + 신규 MesShiftClient(MES fetch, http 전용 @ConditionalOnProperty) 분리** (Option B, 구현 중 채택). bean 충돌 없음 — JpaMesShiftPort 무수정 (회귀 0) |
| **resilience4j instance** | "mes instance 추가 (Slack/Kakao 독립)" | ✅ 현재 instance = `kakaotalk` + `slack` (retry + circuitbreaker). `mes` 신규 추가 — 정합 (단 명칭 `kakao` 아닌 `kakaotalk` 정정) |
| **MesShiftPort 계약** | 시그니처 미명시 | ✅ `reportProduction(machineId, shiftDate, shiftNo:short, plannedQty:int, actualQty:Integer, source:MesShiftSource, reportedBy, note)` + `lastReceivedShift(machineId)`. HttpMesShiftAdapter 는 동일 시그니처 구현 (source=MES_AUTO) |
| **벤더 MES API spec** | Pre-Phase 확보 전제 | ⚠️ 실 spec **미확보** (사내 협의 미완). v1.0 §6 risk 의 "mock spec 진행" 을 **기본 접근**으로 승격 — 가정 contract (`GET /api/mes/shift?machine=&date=&shift_no=` → JSON) 으로 구현, 실 spec 은 Phase 5+ DTO 매핑만 교체 |
| **모듈** | vc.mes | ✅ 신규 클래스 전부 `com.scheduling.vc.mes` (신규 모듈 아님). RestClient 기반 |

**SP·구조 영향**: WireMock 신규 도입분(+0.2)을 ST-MES-5 Grafana(−0.2 deferred 가능)로 상쇄, 합계 **~5 SP 유지**. Task 17 유지 (ST-MES-1-5 에 jpa 조건부 전환 명시).

---

## 1. 목적

**Sprint 17 EP-DAY-LOCK 의 [MesShiftPort](../../backend/vc/src/main/java/com/scheduling/vc/mes/MesShiftPort.java) stub (JpaMesShiftPort) 위에 실 MES 시스템 HTTP polling adapter 추가 — Phase 5+ MQ/file adapter 도입 전 baseline 확보.**

| 항목 | Sprint 17 baseline | Sprint 23 활성 |
|---|---|---|
| MesShiftPort 구현체 | JpaMesShiftPort (stub, DB) | ✅ **HttpMesShiftAdapter** (REST GET) + JpaMesShiftPort 유지 (config flag 분기) |
| MES polling | DegradedModeService snapshot 만 | ✅ **@Scheduled(fixedDelay=60s) polling** → reportProduction() |
| MES 실패 시뮬 | 없음 | ✅ **WireMock IT 4 시나리오** (신규 도입) |
| 운영 가이드 | 없음 | ✅ **매뉴얼 §3.6 IT_OPS MES adapter 설정** (v1.2 §3.5 Excel 폴백 다음 절) |

**Pre-Phase 의존 (현황):**
- ⚠️ 벤더 MES REST API spec — **미확보** → mock contract 으로 진행 (실 spec Phase 5+ DTO 교체)
- ⏳ 사내 IT — MES 네트워크 방화벽 허용 (PROD switch 전까지 불필요 — DEV/STG 는 jpa default)

**활성 후 효과:**
- 실 MES 실적 자동 수신 → degraded mode 자동 해제 (Excel 폴백 빈도 ↓)
- Phase 5+ MQ adapter 진입 게이트 (HTTP baseline 위 adapter 교체)

---

## 2. Sprint 23 SP·기간

| Story | SP | 추정 PD |
|---|:--:|:--:|
| ST-MES-1 HttpMesShiftAdapter (REST GET + 인증 + DTO + jpa 조건부 전환) | 2.0 | 1.0 |
| ST-MES-2 MES polling scheduler (@Scheduled 60s + reportProduction 호출) | 1.0 | 0.5 |
| ST-MES-3 WireMock **신규 도입** + IT 4 시나리오 (정상 / 5xx / timeout / 부분 응답) | 1.0 | 0.5 |
| ST-MES-4 DegradedModeService 통합 검증 (실 polling 시 해제) | 0.5 | 0.3 |
| ST-MES-5 매뉴얼 §3.6 IT_OPS MES 가이드 (+ Grafana 패널 optional) | 0.5 | 0.2 |
| **합계** | **~5 SP** | **~2.5 PD** |

---

## 3. 의존성 DAG

```
Pre-Phase (mock API contract — 실 spec Phase 5+)
    ↓
ST-MES-1 (HttpMesShiftAdapter + jpa 조건부) ──┐
                                             │
ST-MES-2 (Scheduled polling) ────────────────┤  (adapter ↔ scheduler 독립)
                                             ↓
ST-MES-3 (WireMock 도입 + IT 4) ─────────────→ ST-MES-4 (Degraded 통합)
                                                          ↓
                                              ST-MES-5 (매뉴얼)
```

**병렬 윈도우:** ST-MES-1 ↔ ST-MES-2. ST-MES-3 는 WireMock 도입 후 (ST-MES-1 의 adapter 대상).

---

## 4. Story · Task 매트릭스

### ST-MES-1 — HttpMesShiftAdapter (2.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-1-1 | `application.yml` — `scheduling.mes.adapter=jpa\|http` flag + `scheduling.mes.http.base-url=${MES_URL:}` + `bearer-token=${MES_TOKEN:}` + `timeout-seconds=10` | 0.3 |
| TK-MES-1-2 | MesShiftClient(신규 interface) + HttpMesShiftClient(vc.mes) — RestClient GET `${base-url}/api/mes/shift?machine=&date=&shift_no=` + Bearer + `@Retry(name="mes")` + `@CircuitBreaker(name="mes", fallback→Optional.empty())`. fetchShift(machine,date,shiftNo) → MesShiftResponse. MesShiftPort(영속) 와 분리 — 회귀 0 | 0.6 |
| TK-MES-1-3 | MesShiftResponse DTO record (machineId, shiftDate, shiftNo, plannedQty, actualQty, receivedAt) + @JsonIgnoreProperties + isComplete() 부분응답 방어 | 0.3 |
| TK-MES-1-4 | application.yml resilience4j — **`mes` instance 신규** (retry 3회·wait 1s·backoff 2 + ResourceAccessException / circuitbreaker window 10·5회·50%·30s OPEN) — kakaotalk/slack 패턴 정합 | 0.2 |
| TK-MES-1-5 | Bean 분기 — HttpMesShiftClient `@ConditionalOnProperty(name="scheduling.mes.adapter", havingValue="http")`. **JpaMesShiftPort 무변경** (MesShiftPort 영속은 adapter 모드 무관 — Option B 로 bean 충돌 자체 없음) | 0.4 |
| TK-MES-1-6 | 통합 IT — `scheduling.mes.adapter` 미설정(=jpa) 시 JpaMesShiftPort 단독 활성 회귀 (MesShiftAndDegradedIT GREEN) | 0.2 |

### ST-MES-2 — MES polling scheduler (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-2-1 | MesPollingService (vc.mes) — `@Scheduled(fixedDelayString=60s)` + `@Profile("with-infra")` + 활성 vc_machine 머신별 GET → mesPort.reportProduction(). `@ConditionalOnProperty(scheduling.mes.adapter=http)` (jpa 모드 polling 불필요) | 0.5 |
| TK-MES-2-2 | polling 실패 시 LOG WARN + 다음 cycle 재시도 (circuit OPEN 동안 fast-fail skip) + Clock 주입 (BR-X04) | 0.2 |
| TK-MES-2-3 | IT — polling 1회 → mes_shift_event 1+ row (WireMock 정상 응답) | 0.3 |

### ST-MES-3 — WireMock 신규 도입 + IT 4 시나리오 (1.0 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-3-0 | **WireMock 신규 도입** — libs.versions.toml `wiremock-standalone` 버전 + vc(또는 app) `testImplementation`. (S20 ST-EXT-3 미도입 확인) | 0.2 |
| TK-MES-3-1 | WireMockMesIT 시나리오 1 — 정상 200 → reportProduction() + mes_shift_event 영속 | 0.3 |
| TK-MES-3-2 | 시나리오 2 — 5xx 연속 → 3회 retry → circuit OPEN (mes_shift_event INSERT 없음) | 0.2 |
| TK-MES-3-3 | 시나리오 3 — timeout (delay > 10s) → Resilience4j timeout → 다음 cycle | 0.2 |
| TK-MES-3-4 | 시나리오 4 — 부분 응답 (machineId 누락) → DTO 파싱 fail → log + skip (다른 머신 진행) | 0.1 |

### ST-MES-4 — DegradedModeService 통합 검증 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-4-1 | IT — `scheduling.mes.adapter=http` + WireMock 정상 → polling 1회 → DegradedModeService NORMAL 유지 | 0.3 |
| TK-MES-4-2 | IT — WireMock 미수신 (1 shift 6h 초과) → DegradedModeService 진입 이벤트 publish 검증 (기존 DegradedMode 이벤트 계약 재사용) | 0.2 |

### ST-MES-5 — 매뉴얼 §3.6 IT_OPS MES 가이드 (0.5 SP)

| Task | 내용 | SP |
|---|---|:--:|
| TK-MES-5-1 | USER_MANUAL_v1.x §3.6 — MES adapter 설정 (jpa vs http) + URL/Token 등록 + 트러블슈팅 (circuit OPEN 시 §3.2 Excel 폴백 연계). v1.2 다음 버전으로 발행 | 0.3 |
| TK-MES-5-2 | (optional) Grafana 패널 — MES polling success rate / circuit state. 시간 부족 시 S24 ST-FB-3 로 defer | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ `scheduling.mes.adapter=http` 시 HttpMesShiftAdapter 단독 활성 / 미설정 시 JpaMesShiftPort (bean 충돌 0)
2. ✅ MesPollingService 60s 주기 polling + WireMock 정상 시 mes_shift_event INSERT
3. ✅ MES 5xx → Resilience4j 3회 retry → circuit OPEN 30s → HALF_OPEN 자동 복귀
4. ✅ MES timeout (10s+) → polling skip + 다음 cycle
5. ✅ DegradedModeService 통합 — 정상 polling NORMAL / 미수신 시 진입 이벤트
6. ✅ 매뉴얼 §3.6 IT_OPS MES 설정 절차

**비기능 DoD:**
1. ✅ ArchUnit GREEN (vc.mes 경계 + Clock 주입)
2. ✅ verifyAll GREEN — 신규 WireMock IT 4+ + 회귀 0
3. ✅ DEV/STG default `scheduling.mes.adapter=jpa` (PROD 만 http)
4. ✅ Resilience4j `mes` instance 별도 (kakaotalk/slack 독립)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| 벤더 MES API spec 미확보 | 실 연동 불가 | mock contract 으로 baseline 구현 (현 기본 접근). 실 spec 확보 시 MesShiftResponse DTO 매핑만 교체 (Phase 5+) |
| WireMock 신규 도입 의존 충돌 (Spring Boot 3.5 / JUnit 5) | IT 빌드 fail | `wiremock-standalone` (shaded) 버전 사용 — Jetty/Jackson 충돌 회피. testImplementation 한정 |
| jpa→http bean 전환 회귀 (조건부 누락 시 2 bean) | 컨텍스트 起動 fail | TK-MES-1-5 에서 jpa matchIfMissing=true 명시 + ST-MES-1-6 회귀 IT |
| MES polling 60s 부하 (5 머신 × shift) | DB pool 압박 | HikariCP active 모니터링 + 부하 시 interval 120s. jpa 모드는 polling 비활성 |
| 사내 방화벽 미허용 | PROD MES 호출 실패 | DEV/STG jpa default 유지 — PROD switch 전 IT_OPS outbound 허용 협의 |

---

## 7. 작업 순서 추천

**Day 1** — Adapter + Scheduler:
1. TK-MES-1-1~6 (HttpMesShiftAdapter + jpa 조건부 전환 + config)
2. TK-MES-2-1~3 (MesPollingService)

**Day 2** — WireMock 도입 + IT + Degraded:
3. TK-MES-3-0~4 (WireMock 신규 + IT 4)
4. TK-MES-4-1~2 (Degraded 통합)

**Day 3** — 문서 + DoD:
5. TK-MES-5-1 (매뉴얼 §3.6) [+ 5-2 Grafana optional]
6. **본 PC 시각 검증** — WireMock 정상/실패 1회씩 + jpa default 회귀
7. `verifyAll` + commit/push

**총 ~2.5 PD (1인 AI 가속).**

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Service (vc.mes) | HttpMesShiftAdapter + MesPollingService + MesShiftResponse |
| Backend Config | application.yml mes.adapter flag + resilience4j `mes` instance + JpaMesShiftPort 조건부 |
| Backend Build | libs.versions.toml (wiremock-standalone) + vc/app build.gradle.kts testImplementation |
| Backend IT | WireMockMesIT (4 cases) + DegradedModeMesIntegrationIT (2 cases) + jpa default 회귀 |
| Infra Grafana (optional) | MES polling success / circuit state 패널 |
| Docs | USER_MANUAL_v1.x §3.6 MES adapter 가이드 |

---

## 9. Sprint 23 후 다음 단계

**Sprint 24 (EP-OPS-FEEDBACK) 진입 조건:**
- ✅ DoD 10/10 충족
- ⏳ 베타 운영 1개월 데이터 누적 (S23 종료 = 베타 4주차)

**Phase 5+ carry-over:**
- 실 MES API spec 적용 (mock → 실 DTO)
- MES MQ adapter (RabbitMQ / Kafka — 벤더 결정 후)
- MES file adapter (legacy CSV / Excel polling)

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 네 번째 sprint EP-MES-ADAPTER-1 5 Story / 17 Task / ~5 SP. Sprint 17 MesShiftPort baseline 위 HTTP polling adapter Phase 1. DoD 10 + 리스크 5. |
| **1.1** | **2026-05-29** | **Claude Code** | **S22 마감 직후 실 코드 대조 갱신 — stale 가정 정정: (1) WireMock 미도입 확인 (S20 ST-EXT-3 deferred) → S23 신규 도입 (TK-MES-3-0 추가). (2) JpaMesShiftPort 무조건 @Component → @ConditionalOnProperty(jpa, matchIfMissing) 전환 명시 (bean 충돌 방지, TK-MES-1-5). (3) resilience4j instance kakaotalk/slack 확인 → mes 신규. (4) MesShiftPort 8-arg 시그니처 명시 (source=MES_AUTO). (5) 벤더 spec 미확보 → mock contract 기본 접근 승격. (6) 매뉴얼 §3.5→§3.6 (v1.2 Excel 폴백 다음 절). SP ~5 유지, Task 17→18 (TK-MES-3-0).** |
