# 작업 분할 구조서 (WBS) v1.14 — Sprint 18 EP-NOTIFY 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.14 | **작성일**: 2026-05-28
**전판**: [v1.13](TASK-001_WBS_v1.13.md) (Sprint 17 EP-DAY-LOCK 마감 Addendum)
**상태**: Addendum — Sprint 18 EP-NOTIFY (운영 알림 통합) 100% 마감 + DoD 11/11 ✅ (Slack/Kakao 실 webhook URL은 Phase 4+ carry-over)

> v1.13 (Sprint 17 마감, 65 Epic / 334 SP 실) 의 §6 carry-over **Slack/STOMP degraded 알림** + **Sprint 17 hotfix Batch UX** 동시 흡수. Sprint 1~3 DeliveryEscalator + NotificationService 자산 위에 Slack 실 활성 + Kakao webhook 실 + Resilience4j + MES degraded push + Frontend NotificationDrawer 통합.

---

## 1. v1.13 → v1.14 변경 요지

| 항목 | v1.13 (Sprint 17) | v1.14 (Sprint 18 + Sprint 17 hotfix) |
|---|---|---|
| Epic 총수 | 65 | 66 (+ EP-NOTIFY) |
| SP 실 합 | 334 | **338.5** (+~4.5 실, 계획 4 + Sprint 17 hotfix Batch UX +0.5) |
| Sprint 18 상태 | 계획 4 SP | ✅ **마감** (6 Story / 17 Task / 5 commits / ~0.8 PD AI 가속) |
| Sprint 17 hotfix | (포함 안 됨) | ✅ **Batch 확정 UX** (`ef8967a` Sprint 17 carry-over → Sprint 18 직전 hotfix 통합) |
| **Slack alert** | 미존재 (DeliveryEscalator LOG only) | ✅ **SlackNotifier + @Retry/@CircuitBreaker** (config flag default false) |
| **Kakao webhook** | stub only | ✅ **실 HTTP RestClient POST** (config flag + Bearer token + Resilience4j) |
| **Resilience4j** | kakaotalk instance만 | ✅ **slack instance 추가** (3회 retry, 5회 실패 30s OPEN) |
| **MES degraded push** | polling snapshot only (이벤트 미발행) | ✅ **MesDegradedModeChangedEvent + @Scheduled 1분 poll + DegradedModePushListener** (Slack + STOMP 양쪽) |
| **NotificationDrawer** | 미존재 | ✅ **우상단 종 아이콘 Badge + Drawer** (4 role 공통, Zustand persist 50건 cap, STOMP 2 토픽 구독) |
| **VcSchedule.createdBy 노출** | backend 만 | ✅ **SlotRow.createdBy** 추가 (Frontend BR-X05 본인 작성 식별, batch 자동 제외) |
| **DoD** | 11/11 ✅ | ✅ **11/11** (Slack/Kakao 실 URL/token 발급은 Phase 4+ carry-over) |

---

## 2. Sprint 18 마감 — EP-NOTIFY 6 Story 회고

### EP-NOTIFY 전체 (운영 알림 통합 BR-O02·REQ-FUNC-OC-009)

**Sprint**: **S18** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-18_EP-NOTIFY_v1.0](PLAN-SPRINT-18_EP-NOTIFY_v1.0.md) (3-Day) / **SP 실**: ~4 / **선행**: EP-DAY-LOCK (S17)

| Story | 구현 |
|---|---|
| ST-NOTIFY-1 — Slack alert 활성 | `NotificationConfig.Slack` nested class (enabled/webhookUrl/alertsChannel/criticalChannel) + `SlackNotifier` (RestClient POST, severity 별 채널 라우팅, `@Retry(name=slack)` + `@CircuitBreaker(name=slack)` + fallback log only) + `DeliveryEscalator` 강화 (overdue Critical loop 내 `slackNotifier.alert()` 호출) |
| ST-NOTIFY-2 — Kakao webhook 활성 | `KakaoTalkClient` stub → 실 RestClient POST (config.enabled + webhookUrl 비어있지 않을 때만, Bearer token 헤더). 기존 Resilience4j annotation 유지 |
| ST-NOTIFY-3 — Resilience4j 통합 | `application.yml resilience4j.retry.instances.slack` + `resilience4j.circuitbreaker.instances.slack` 신규 (3회 retry · sliding-window 10 · 5회 실패 50% 임계 30s OPEN · half-open 자동 전이) |
| ST-NOTIFY-4 — MES degraded push (Sprint 17 carry-over) | `MesDegradedModeChangedEvent` record (vc.events, isEntering/isRecovered helper) + `DegradedModeService.pollAndPublish()` @Scheduled 1분 + in-memory lastDegradedState 비교 + `DegradedModePushListener` (notify, @ApplicationModuleListener AFTER_COMMIT, Slack CRITICAL + STOMP `/topic/mes-degraded-updates`) |
| ST-NOTIFY-5 — Frontend NotificationDrawer | features/notify 패키지 신설: `notificationStore` (Zustand persist 50건 cap + markRead/markAllRead/clear/unreadCount) + `NotificationDrawer` (우상단 종 + Badge + Drawer severity 색상 Tag + deep-link) + `useStompNotificationFeed` (role별 + MES degraded 2 토픽 구독) + `MainLayout` Header 통합 |
| ST-NOTIFY-6 — EP-NOTIFY IT + 회귀 | SlackEscalationIT 4 cases + DegradedModePushIT 3 cases + Sprint 16/17 회귀 23 cases + 신규 Kakao/Diff 회귀 6 cases = **62/62 GREEN** |

### Sprint 17 hotfix (Batch UX, `ef8967a` 직전 통합)

| 항목 | 구현 |
|---|---|
| TK-HOTFIX-1 SlotRow.createdBy 노출 | `VcScheduleQueryController.SlotRow` record 에 `createdBy` 필드 추가 — Frontend 가 BR-X05 본인 작성 row 식별 |
| TK-HOTFIX-2 confirmBatch() API client | `vcScheduleApi.confirmVcScheduleBatch()` — Sprint 16 backend `/confirm-batch` endpoint 클라이언트 |
| TK-HOTFIX-3 BatchConfirmModal | 다건 확정 UI (선택 건수 + 총수량 + 가류기/Hose 분포 + BR-X05 자동 제외 안내 + 4xx HttpError 분기) |
| TK-HOTFIX-4 CandidateConfirmTable | List → Ant Design Table + rowSelection + 전체 선택(본인 제외) + 일괄 확정 버튼. 본인 작성 row 는 빨강 배지 + checkbox/단건 disabled |

### Sprint 18 Task 매트릭스 (17 Task + Sprint 17 hotfix 4)

| Task | 소속 Story | SP 실 |
|---|---|---|
| TK-NOTIFY-1-1 SlackNotifier 신규 | ST-NOTIFY-1 | 0.4 |
| TK-NOTIFY-1-2 DeliveryEscalator 강화 | ST-NOTIFY-1 | 0.3 |
| TK-NOTIFY-1-3 SlackEscalationIT 4 cases | ST-NOTIFY-1 | 0.3 |
| TK-NOTIFY-2-1 KakaoTalkClient 실 HTTP POST | ST-NOTIFY-2 | 0.3 |
| TK-NOTIFY-2-2 회귀 Kakao IT GREEN | ST-NOTIFY-2 | 0.1 |
| TK-NOTIFY-3-1 application.yml slack instance | ST-NOTIFY-3 | 0.2 |
| TK-NOTIFY-3-2 @Retry/@CircuitBreaker annotation | ST-NOTIFY-3 | 0.3 |
| TK-NOTIFY-3-3 fallback 검증 (cosmetic warnings only) | ST-NOTIFY-3 | 0.1 |
| TK-NOTIFY-4-1 MesDegradedModeChangedEvent | ST-NOTIFY-4 | 0.2 |
| TK-NOTIFY-4-2 DegradedModeService pollAndPublish | ST-NOTIFY-4 | 0.4 |
| TK-NOTIFY-4-3 DegradedModePushListener | ST-NOTIFY-4 | 0.3 |
| TK-NOTIFY-4-4 DegradedModePushIT 3 cases | ST-NOTIFY-4 | 0.1 |
| TK-NOTIFY-5-1 NotificationDrawer + notificationStore | ST-NOTIFY-5 | 0.3 |
| TK-NOTIFY-5-2 useStompNotificationFeed | ST-NOTIFY-5 | 0.2 |
| TK-NOTIFY-6-1 Sprint 16/17 회귀 23 cases | ST-NOTIFY-6 | 0.3 |
| TK-NOTIFY-6-2 Kakao/Diff/Swap 회귀 9 cases | ST-NOTIFY-6 | 0.1 |
| TK-NOTIFY-6-3 Frontend vitest 82/82 GREEN | ST-NOTIFY-6 | 0 |
| **Sprint 18 합계** | | **~4 SP** |
| TK-HOTFIX-1~4 (Sprint 17 hotfix) | (별도) | **0.5** |
| **누적 합계** | | **~4.5 SP** |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | Critical Diff 1분 overdue → Slack push | ✅ DeliveryEscalator + SlackEscalationIT (overdue 1건 + 3건 호출 검증) |
| 2 | Kakao Critical 알림 실 발송 + KakaoDeliveryAttempt 영속 | ✅ KakaoTalkClient RestClient POST + 기존 IT 회귀 GREEN |
| 3 | Slack/Kakao Resilience4j retry + circuit breaker | ✅ application.yml slack instance + @Retry/@CircuitBreaker annotation + fallback method |
| 4 | MES degraded mode 진입 시 Slack + STOMP push | ✅ DegradedModePushIT (entering case) |
| 5 | MES degraded mode 해제 시 Slack + STOMP push | ✅ DegradedModePushIT (recovered case) |
| 6 | Frontend NotificationDrawer 우상단 종 + Badge + Drawer | ✅ NotificationDrawer + MainLayout Header 통합 |
| 7 | STOMP `/topic/notifications/{role}` 구독 → Drawer 누적 | ✅ useStompNotificationFeed (role별 + MES degraded 2 토픽) |
| 비기능 1 | ArchUnit GREEN | ✅ |
| 비기능 2 | Backend 신규 IT 7+ + 회귀 0 | ✅ 62/62 GREEN (Sprint 16/17/18 통합) |
| 비기능 3 | TypeScript compile + frontend tests GREEN | ✅ tsc 0 + vitest 82/82 |
| 비기능 4 | circuit breaker open 시 fallback (no crash) | ✅ fallbackAlert / fallbackSend log only |

**기능 7 + 비기능 4 = 11/11 ✅**.

---

## 3. v1.13 §6 carry-over → v1.14 갱신

| 항목 | v1.13 carry-over | v1.14 결과 |
|---|---|---|
| **Slack/STOMP degraded 알림** | High Sprint 18 | ✅ **Sprint 18 ST-NOTIFY-4 마감** — DegradedModeChangedEvent + Listener |
| 본 PC 실 시나리오 E2E (Sprint 13~17 통합) | High | ⏳ 잔여 (Sprint 19 베타 진입 직전 단일 시나리오, Sprint 18 추가 통합) |
| **MES 실 연동 (HTTP/MQ/file adapter)** | High Phase 5+ | High Phase 5+ (변동 없음 — Sprint 18 stub adapter 위에서 교체) |
| 장비/셋팅/합금형/라인 5 entity CRUD UI | Medium | Medium Sprint 19 carry-over (변동 없음) |
| Order 자동 INSERT 흐름 | Medium | Phase 5+ (변동 없음, Sprint 17 Allocator.requestedBy 완성으로 진입 게이트 충족) |
| 99999-SAMPLE-* PROD cleanup | Low | Sprint 19 EP-BETA-LAUNCH (변동 없음) |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 (변동 없음) |
| **Slack/Kakao 실 webhook URL/token** | (Sprint 18 신설) | High Phase 4+ — config flag default false 로 실 발급 전 안전 |

---

## 4. v1.2 § 추가 영향 정리 (v1.13 → v1.14 확장)

| § | v1.13 → v1.14 변경 |
|---|---|
| §9 Deferred Epic | + **EP-NOTIFY (S18 마감)** — Slack alert + Kakao webhook 실 + Resilience4j + MES degraded push + Frontend Drawer |
| §14 SP 합계 | 334 → **338.5** (Sprint 18 +~4 + hotfix +0.5) |
| §16 Phase B 진입 조건 | + **Sprint 18 마감 → Sprint 19 EP-BETA-LAUNCH 진입 게이트 충족** (운영 알림 + 가시성 완비) |
| §17 GitHub label | `sprint:S18` 추가 |
| §18 BR 추적 | BR-X06 강화 (degraded push), BR-O02 보강 (Slack alert), BR-X05 보강 (Frontend batch 자동 제외) |
| §19 Modulith 경계 | notify 모듈 allowedDependencies — vc::events 통해 MesDegradedModeChangedEvent 의존 (기존 정합) |

---

## 5. 신규 산출물 (Sprint 18)

### Backend Config
- [NotificationConfig.java](../../backend/notify/src/main/java/com/scheduling/notify/NotificationConfig.java) — `Slack` nested class 추가
- [application.yml](../../backend/app/src/main/resources/application.yml) — resilience4j slack instance 추가

### Backend Service (notify)
- [SlackNotifier.java](../../backend/notify/src/main/java/com/scheduling/notify/SlackNotifier.java) — Slack webhook 클라이언트
- [DeliveryEscalator.java](../../backend/notify/src/main/java/com/scheduling/notify/DeliveryEscalator.java) — SlackNotifier 호출 추가
- [KakaoTalkClient.java](../../backend/notify/src/main/java/com/scheduling/notify/KakaoTalkClient.java) — 실 RestClient POST
- [DegradedModePushListener.java](../../backend/notify/src/main/java/com/scheduling/notify/DegradedModePushListener.java) — MES 전이 → Slack + STOMP

### Backend Event/Service (vc)
- [MesDegradedModeChangedEvent.java](../../backend/vc/src/main/java/com/scheduling/vc/events/MesDegradedModeChangedEvent.java) — vc.events 신규
- [DegradedModeService.java](../../backend/vc/src/main/java/com/scheduling/vc/mes/DegradedModeService.java) — `pollAndPublish()` @Scheduled 추가
- [VcScheduleQueryController.java](../../backend/vc/src/main/java/com/scheduling/vc/domain/VcScheduleQueryController.java) — SlotRow.createdBy 노출 (Sprint 17 hotfix)

### Backend IT (app)
- [SlackEscalationIT.java](../../backend/app/src/test/java/com/scheduling/integration/SlackEscalationIT.java) — 4 cases (DeliveryEscalator → SlackNotifier 호출)
- [DegradedModePushIT.java](../../backend/app/src/test/java/com/scheduling/integration/DegradedModePushIT.java) — 3 cases (entering/recovered/same state)

### Frontend (features/notify 신설)
- [notificationStore.ts](../../frontend/src/features/notify/notificationStore.ts) — Zustand persist + 50건 cap
- [NotificationDrawer.tsx](../../frontend/src/features/notify/components/NotificationDrawer.tsx) — 우상단 종 + Badge + Drawer
- [useStompNotificationFeed.ts](../../frontend/src/features/notify/hooks/useStompNotificationFeed.ts) — 2 토픽 구독 hook
- [stompClient.ts](../../frontend/src/api/stompClient.ts) — `TOPIC_MES_DEGRADED_UPDATES` + `TOPIC_NOTIFICATIONS_PREFIX` 상수
- [MainLayout.tsx](../../frontend/src/pages/layouts/MainLayout.tsx) — Header NotificationDrawer + useStompNotificationFeed
- [BatchConfirmModal.tsx](../../frontend/src/features/vc-scheduling/components/BatchConfirmModal.tsx) — Sprint 17 hotfix
- [VcSimulationPage.tsx](../../frontend/src/pages/VcSimulationPage.tsx) — CandidateConfirmTable + BatchConfirmModalWrapper (Sprint 17 hotfix)

---

## 6. carry-over 식별 (Sprint 19+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| 본 PC 실 시나리오 E2E (Sprint 13~18 통합) + Batch 확정 + MES degraded 시뮬 + Slack/Drawer | High | Sprint 19 베타 진입 직전 단일 시나리오 검증 |
| **MES 실 adapter (HTTP/MQ/file)** | High Phase 5+ | MesShiftPort 인터페이스 위 교체 |
| **Slack/Kakao 실 webhook URL/token 발급** | High Phase 4+ | 사내 IT/관리팀 협의 — Sprint 18 baseline 은 config flag default false 로 안전 |
| 장비/셋팅/합금형/라인 5 entity CRUD UI | Medium | Sprint 19 carry-over |
| Order 자동 INSERT 흐름 (ImportOrchestrator → Allocator chain) | Medium | Phase 5+ |
| 99999-SAMPLE-* PROD cleanup | Low | Sprint 19 EP-BETA-LAUNCH cutover script |
| DaoAuthenticationProvider deprecation | Low | Sprint 19 직전 |
| Resilience4j fallback 정밀 검증 (circuit OPEN 강제 시뮬) | Low | Sprint 19+ WireMock 도입 후 |

---

## 7. 관련 자료

- [TASK-001_WBS_v1.13](TASK-001_WBS_v1.13.md) — Sprint 17 마감
- [PLAN-SPRINT-18_EP-NOTIFY_v1.0](PLAN-SPRINT-18_EP-NOTIFY_v1.0.md) — Sprint 18 진입 plan (6 Story / 17 Task / DoD 11)
- [SlackNotifier](../../backend/notify/src/main/java/com/scheduling/notify/SlackNotifier.java)
- [DegradedModePushListener](../../backend/notify/src/main/java/com/scheduling/notify/DegradedModePushListener.java)
- [MesDegradedModeChangedEvent](../../backend/vc/src/main/java/com/scheduling/vc/events/MesDegradedModeChangedEvent.java)
- [NotificationDrawer](../../frontend/src/features/notify/components/NotificationDrawer.tsx)

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0~1.4 | 2026-05-15~23 | (작성자/Claude) | 초안 ~ Sprint 8 마감 |
| 1.5~1.13 | 2026-05-27 | Claude Code | Sprint 9~17 마감 + V038 hotfix + AuditLogService hotfix + EX chain + EP-CONFIRM + EP-DAY-LOCK |
| 1.14 | 2026-05-28 | Claude Code | **Addendum — Sprint 18 EP-NOTIFY 100% 마감 (6 Story / 17 Task / ~4 SP) + Sprint 17 hotfix Batch UX (+0.5 SP) 동시 통합. Slack alert 실 활성 (SlackNotifier + @Retry/@CircuitBreaker + DeliveryEscalator 강화) + Kakao webhook 실 RestClient POST + Resilience4j slack instance 추가 + MES degraded mode push (MesDegradedModeChangedEvent + @Scheduled 1분 poll + DegradedModePushListener Slack/STOMP 양쪽) + Frontend NotificationDrawer (우상단 종 + Badge + Drawer Zustand persist 50건 cap + useStompNotificationFeed 2 토픽 구독). Backend IT 7 신규 + 11 회귀 = 62/62 GREEN, Frontend tsc 0 + vitest 82/82 GREEN. DoD 11/11 ✅ (Slack/Kakao 실 URL/token 발급은 Phase 4+ carry-over). 66 Epic / 338.5 SP 실. Sprint 19 EP-BETA-LAUNCH 진입 게이트 충족 — 운영 알림 + 가시성 완비. 베타 진입도 9/10 (S10~18 완료)** |
