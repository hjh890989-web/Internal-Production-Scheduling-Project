# Sprint 18 진입 계획 — EP-NOTIFY (운영 알림 통합 BR-O02·REQ-FUNC-OC-009) v1.0

**작성일**: 2026-05-28 | **버전**: 1.0 | **상태**: Sprint 18 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 18 baseline](TASK-001_WBS_v1.5.md) + [WBS v1.13 §6 carry-over](TASK-001_WBS_v1.13.md) + REF-SRS REQ-FUNC-OC-009 + REQ-NF-REL-004 + [PLAN-SPRINT-17_EP-DAY-LOCK_v1.0](PLAN-SPRINT-17_EP-DAY-LOCK_v1.0.md)

---

## 1. 목적

**Sprint 17 EP-DAY-LOCK 직후 진입** — 확정 게이트 + 당일 락 + MES 폴백 baseline 완비된 상태에서 **운영 알림 통합 강화**:

| 정책 | 적용 단계 |
|---|---|
| Slack alert 활성 (Critical 1분 overdue + degraded mode 진입/해제) | DeliveryEscalator 강화 + 신규 SlackNotifier |
| Kakao webhook 활성 (현재 stub) | KakaoTalkClient 실 webhook + 설정 활성 |
| Resilience4j retry + circuit breaker (Slack/Kakao) | application.yml 설정 통합 |
| MES degraded mode push (Sprint 17 carry-over) | DegradedModeChangedEvent 신규 + Listener + STOMP/Slack |
| Frontend in-app 알림 센터 | NotificationDrawer (4 role 공통, /topic/notifications/{role} 구독) |

**현황 인벤토리 (Sprint 17 직전 상태):**
- ✅ `WebSocketNotificationPublisher` + STOMP `/topic/notifications/{role}` (Sprint 1)
- ✅ `NotificationService` → in-app(모두) + Kakao(Critical only) 라우팅 (Sprint 1)
- ✅ `SeverityClassifier` — delivery/hose 변경 → CRITICAL, qty ±20% → CRITICAL (Sprint 1~3)
- ✅ `DeliveryEscalator` — 1분 overdue Critical retry_count++ (log only, Slack 미연동)
- ✅ `KakaoTalkClient` + `KakaoDeliveryService` (stub, `scheduling.notification.kakao.enabled=false`)
- ⏳ **Slack alert 실 활성** — 정책만 (DeliveryEscalator log only)
- ⏳ **Kakao webhook 실 활성** — stub only
- ⏳ **Resilience4j** — application.yml 설정만, Slack/Kakao 미적용
- ⏳ **MES degraded push** — Sprint 17 deferred (DegradedModeService stateless snapshot only, 이벤트 미발행)
- ⏳ **Frontend NotificationDrawer** — 미존재 (STK 시뮬뷰 만 토스트 받음)

**활성 후 효과:**
- Critical Diff 1분 overdue → Slack #alerts-prod 자동 push
- MES 1 shift 미수신 → degraded 전이 시 Slack + STOMP 동시 push (해제 시도 동일)
- Kakao Critical 알림 실 발송 (도달률 KPI 측정 가능)
- 4 role 누구나 우상단 NotificationDrawer 에서 in-app 알림 일괄 조회
- Sprint 19 EP-BETA-LAUNCH 진입 게이트 — 운영 가시성 완비

---

## 2. Sprint 18 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-NOTIFY-1 Slack alert 활성 (DeliveryEscalator + 신규 SlackNotifier) | 1.0 | 0.5 |
| ST-NOTIFY-2 Kakao webhook 활성 + 실 발송 검증 | 0.5 | 0.3 |
| ST-NOTIFY-3 Resilience4j retry + circuit breaker (Slack/Kakao 통합) | 0.8 | 0.4 |
| ST-NOTIFY-4 MES degraded mode push (Sprint 17 carry-over) | 1.0 | 0.5 |
| ST-NOTIFY-5 Frontend NotificationDrawer (4 role 공통 in-app 센터) | 0.5 | 0.3 |
| ST-NOTIFY-6 EP-NOTIFY IT 5+ cases + 회귀 | 0.4 | 0.2 |
| **합계** | **~4.2 SP** | **~2.2 PD** |

> **WBS v1.5 계획 4 SP 정합** (Sprint 17 carry-over MES push 0.2 over-allocation, capa 여유 내).

---

## 3. 의존성 DAG

```
ST-NOTIFY-1 (Slack alert)
    ↓
ST-NOTIFY-3 (Resilience4j) ──┐
                              │
ST-NOTIFY-2 (Kakao webhook)   │
                              ↓
ST-NOTIFY-4 (MES degraded push)
                              ↓
              ST-NOTIFY-5 (Frontend Drawer)
                              ↓
                      ST-NOTIFY-6 (IT + DoD)
```

**병렬 윈도우:**
- **ST-NOTIFY-1 ↔ ST-NOTIFY-2** — Slack vs Kakao 독립 채널
- **ST-NOTIFY-4 ↔ ST-NOTIFY-5** — Backend event vs Frontend 위젯 분리

---

## 4. Story · Task 매트릭스

### ST-NOTIFY-1 — Slack alert 활성

| Task | 내용 | SP |
|---|---|:--:|
| TK-NOTIFY-1-1 | `SlackNotifier` 클래스 신설 (Slack webhook POST, 채널 `scheduling.notification.slack.{alerts-channel,critical-channel}` 분리). config `scheduling.notification.slack.enabled` default false | 0.4 |
| TK-NOTIFY-1-2 | `DeliveryEscalator` 강화 — overdue Critical 발견 시 `SlackNotifier.alert(severity, hose, due, retryCount)` 호출 + 기존 retry_count++ 유지 | 0.3 |
| TK-NOTIFY-1-3 | IT — SlackNotifier 호출 횟수 검증 (WireMock 또는 spy) + overdue 1건 시 Slack 1회 호출 | 0.3 |

### ST-NOTIFY-2 — Kakao webhook 활성

| Task | 내용 | SP |
|---|---|:--:|
| TK-NOTIFY-2-1 | `KakaoTalkClient` 실 webhook URL 적용 + 200 OK 응답 검증. 실 운영 webhook 은 시크릿 (Phase 4+ application-prod.yml). 본 task 는 stub URL 로 발송 흐름 검증 | 0.3 |
| TK-NOTIFY-2-2 | IT — Kakao enabled=true + Critical Diff → KakaoDeliveryAttempt 영속 + 200 응답 시뮬 (WireMock) | 0.2 |

### ST-NOTIFY-3 — Resilience4j 통합

| Task | 내용 | SP |
|---|---|:--:|
| TK-NOTIFY-3-1 | `application.yml` resilience4j retry + circuit breaker 설정 (Slack/Kakao 각 instance). 3회 retry · 5초 timeout · 50% failure rate 임계 OPEN | 0.3 |
| TK-NOTIFY-3-2 | SlackNotifier/KakaoTalkClient @Retry + @CircuitBreaker annotation 적용. 기존 inline retry (3회) 제거 | 0.3 |
| TK-NOTIFY-3-3 | IT — circuit breaker OPEN 시 fallback (log only) 검증 + fail-fast 동작 (Slack 5회 연속 실패 → 30초 OPEN) | 0.2 |

### ST-NOTIFY-4 — MES degraded mode push (Sprint 17 carry-over)

| Task | 내용 | SP |
|---|---|:--:|
| TK-NOTIFY-4-1 | `MesDegradedModeChangedEvent` record 신규 (machineId, previous, current, changedAt) — vc.events 패키지 | 0.2 |
| TK-NOTIFY-4-2 | `DegradedModeService` — snapshot 호출마다 in-memory 직전 상태 비교 → 변화 시 ApplicationEventPublisher 발행. 또는 scheduled task `@Scheduled(fixedDelay=60s)` 로 polling + 발행 | 0.4 |
| TK-NOTIFY-4-3 | `DegradedModePushListener` (notify 모듈) — 이벤트 수신 → SlackNotifier.alert(degraded 진입/해제) + WebSocketNotificationPublisher.publish (모든 role) | 0.3 |
| TK-NOTIFY-4-4 | IT — DegradedMode 전이 시 SlackNotifier 호출 + STOMP push 발행 검증 | 0.1 |

### ST-NOTIFY-5 — Frontend NotificationDrawer

| Task | 내용 | SP |
|---|---|:--:|
| TK-NOTIFY-5-1 | `NotificationDrawer.tsx` — 우상단 종 아이콘 Badge (미읽음 N) + Drawer 슬라이드 in-app 알림 리스트 (severity 색상 + 시각 + 클릭 시 deep-link) | 0.3 |
| TK-NOTIFY-5-2 | STOMP `/topic/notifications/{role}` 구독 → Zustand store 누적 (최근 50건). 읽음 처리 (localStorage) | 0.2 |

### ST-NOTIFY-6 — EP-NOTIFY IT + 회귀

| Task | 내용 | SP |
|---|---|:--:|
| TK-NOTIFY-6-1 | EP-NOTIFY 통합 IT 5+ — Slack alert + Kakao 발송 + circuit breaker + degraded push + STOMP 구독 | 0.3 |
| TK-NOTIFY-6-2 | Sprint 1~17 회귀 0 (NotificationService 변경 영향 최소화 확인) | 0.1 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ Critical Diff 1분 overdue → Slack #alerts-prod 자동 push (DeliveryEscalator 강화)
2. ✅ Kakao Critical 알림 실 발송 + KakaoDeliveryAttempt 영속
3. ✅ Slack/Kakao Resilience4j retry + circuit breaker (5회 실패 30초 OPEN)
4. ✅ MES degraded mode 진입 시 Slack + STOMP push (모든 role)
5. ✅ MES degraded mode 해제 시 Slack + STOMP push
6. ✅ Frontend NotificationDrawer 우상단 종 아이콘 + Badge + Drawer 리스트 (4 role)
7. ✅ STOMP `/topic/notifications/{role}` 구독 → Drawer 자동 누적

**비기능 DoD:**
1. ✅ ArchUnit GREEN (notify 모듈 boundary)
2. ✅ Backend 신규 IT 5+ + 회귀 0
3. ✅ TypeScript compile + frontend tests GREEN
4. ✅ Slack/Kakao circuit breaker open 시 fallback 동작 (no crash, log only)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| Slack webhook URL 미발급 (사내 운영 채널) | Slack 실 발송 불가, IT 만 WireMock 검증 | `scheduling.notification.slack.webhook-url` 기본값 빈 문자열 + `enabled=false` default. 실 운영 URL 은 Phase 4+ application-prod.yml |
| Kakao biz token 사내 발급 지연 | Kakao 실 발송 불가 | 동일 패턴 — config flag + WireMock IT. 도달률 KPI 측정은 token 발급 후 (Sprint 19+) |
| Resilience4j circuit breaker OPEN 후 자동 회복 누락 | 알림 영구 중단 | `wait-duration-in-open-state=30s` + `permitted-number-of-calls-in-half-open-state=3` 으로 자동 half-open 전이 |
| DegradedModeChangedEvent 발행 polling 과다 | DB load + 알림 노이즈 | `@Scheduled(fixedDelay=60s)` 1분 주기 + in-memory 직전 상태 비교 (변화 시만 publish) |
| NotificationDrawer localStorage 무한 누적 | 브라우저 memory leak | 최근 50건 cap + 읽음 처리 후 7일 만료 |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — Slack + Kakao + Resilience4j (병렬):
1. TK-NOTIFY-1-1~3 (Slack alert)
2. TK-NOTIFY-2-1~2 (Kakao webhook)
3. TK-NOTIFY-3-1~3 (Resilience4j 통합)

**Day 2** — MES degraded push + Frontend:
4. TK-NOTIFY-4-1~4 (DegradedModeChangedEvent + push)
5. TK-NOTIFY-5-1~2 (NotificationDrawer)

**Day 3** — IT + DoD:
6. TK-NOTIFY-6-1~2 (통합 IT + 회귀)
7. **DoD 본 PC 시각 검증** — degraded 모드 강제 진입 (SQL UPDATE 로 mes_shift_event received_at 6h 전으로 set) → Slack/STOMP 동시 push 확인

**총 ~2.2 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Service | SlackNotifier (신규), DegradedModePushListener (notify), DegradedModeService scheduled push 추가 |
| Backend Event | MesDegradedModeChangedEvent (vc.events) |
| Backend Config | application.yml resilience4j + slack/kakao webhook 설정 |
| Backend IT | `NotifyIntegrationIT.java` (5+ cases) |
| Frontend | NotificationDrawer + notificationStore (Zustand) + Layout 통합 |
| Docs | rbac-matrix.md v1.4 부분 갱신 (Slack/Kakao 발송 actor — system) |

---

## 9. Sprint 18 후 다음 단계

**Sprint 19 (EP-BETA-LAUNCH) 진입 조건:**
- ✅ DoD 11/11 충족
- ✅ Slack 실 webhook URL 발급 (사내 IT 운영팀 협의)
- ✅ Kakao biz token 발급 (운영 phase 4 baseline)
- ✅ 본 PC degraded mode 강제 진입 시각 검증 (Slack/STOMP push)

**Sprint 19 첫 작업** — PLAN-SPRINT-19 작성 (베타 cutover script + 99999-SAMPLE-* PROD cleanup + 본 PC E2E 통합 시나리오 단일 검증).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — EP-NOTIFY 6 Story / 17 Task / ~4.2 SP 분해 + 의존성 DAG + DoD 11 + 3-Day 작업 순서. Sprint 1~3 자산 (DeliveryEscalator + SeverityClassifier + WebSocketNotificationPublisher + KakaoDeliveryService) 위에 Slack 실 활성 + Kakao webhook 실 + Resilience4j 통합 + MES degraded push (Sprint 17 carry-over) + Frontend NotificationDrawer. WBS v1.5 §5 Sprint 18 4 SP 정합 (carry-over 0.2 over). |
