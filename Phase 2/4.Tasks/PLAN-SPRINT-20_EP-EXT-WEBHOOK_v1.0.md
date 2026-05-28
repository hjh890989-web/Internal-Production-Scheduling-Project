# Sprint 20 진입 계획 — EP-EXT-WEBHOOK (Slack/Kakao 실 webhook + WireMock IT) v1.0

**작성일**: 2026-05-28 | **버전**: 1.0 | **상태**: Phase 4 첫 sprint 진입 권고안

> **참조**: [PHASE-4_STABILIZATION_v1.0 §3 S20](PHASE-4_STABILIZATION_v1.0.md) + [WBS v1.15 §6 carry-over](TASK-001_WBS_v1.15.md) + [PLAN-SPRINT-18_EP-NOTIFY_v1.0](PLAN-SPRINT-18_EP-NOTIFY_v1.0.md)

---

## 1. 목적

**Phase 4 운영 안정화 첫 sprint — Sprint 18 EP-NOTIFY 의 config flag default false stub 을 실 webhook 으로 활성 + Sprint 18 carry-over 인 Resilience4j fallback 정밀 검증 (WireMock 도입).**

| 항목 | Sprint 18 baseline | Sprint 20 활성 |
|---|---|---|
| Slack alert | config `slack.enabled=false` (LOG only) | ✅ `application-prod.yml` 실 webhook URL + 채널 라우팅 |
| Kakao 도달 | config `kakao.enabled=false` (LOG only) | ✅ 실 biz token + webhook URL + 도달률 KPI Grafana 패널 |
| Resilience4j circuit OPEN 시뮬 | 미검증 (코드만) | ✅ WireMock 5xx 강제 → 3회 retry → OPEN 전이 → fallback LOG 검증 |
| 매뉴얼 §6 장애 대응 | 9건 | ✅ + Slack/Kakao 발송 실패 절차 추가 (총 11건) |
| rbac-matrix | v1.4 (Sprint 18) | ✅ v1.5 — Slack/Kakao 발송 actor = `system:webhook` audit 강화 |

**Pre-Phase 의존 (Sprint 20 진입 직전 완료 필요):**
- 사내 IT — Slack workspace 사내 채널 `#scheduling-alerts` + `#scheduling-critical` 발급
- 사내 관리팀 — Kakao Workplace Bot 계약 + biz token + webhook URL 발급

**활성 후 효과:**
- 실 Slack 채널에 Critical Diff 1분 overdue 자동 push (운영팀 즉시 인지)
- Kakao 도달률 KPI 측정 시작 (NS-04 KPI — REQ-FUNC-CO-008)
- Resilience4j OPEN/HALF_OPEN 자동 전이 실 검증 → 장애 시 fail-fast + 자동 복구 보장

---

## 2. Sprint 20 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-EXT-1 Slack 실 webhook 적용 + 채널 라우팅 검증 | 1.0 | 0.5 |
| ST-EXT-2 Kakao 실 webhook + biz token + 도달률 Grafana | 1.0 | 0.5 |
| ST-EXT-3 WireMock 도입 + Resilience4j circuit OPEN 강제 시뮬 IT | 1.0 | 0.5 |
| ST-EXT-4 사용자 매뉴얼 §6 갱신 (Slack/Kakao 장애 절차 +2건) | 0.5 | 0.3 |
| ST-EXT-5 rbac-matrix.md v1.5 (Slack/Kakao actor 감사) | 0.5 | 0.2 |
| **합계** | **~4 SP** | **~2 PD** |

> **PHASE-4 §3 계획 4 SP 정합.**

---

## 3. 의존성 DAG

```
Pre-Phase (사내 IT/관리팀 협의)
    ↓
ST-EXT-1 (Slack 실 webhook) ──┐
                              │
ST-EXT-2 (Kakao 실 webhook)   │
                              ↓
ST-EXT-3 (WireMock IT) ───────→ ST-EXT-4 (매뉴얼)
                                          ↓
                                  ST-EXT-5 (rbac-matrix)
```

**병렬 윈도우:**
- **ST-EXT-1 ↔ ST-EXT-2** — Slack/Kakao 독립 channel
- **ST-EXT-4 ↔ ST-EXT-5** — 문서 작업 병렬

---

## 4. Story · Task 매트릭스

### ST-EXT-1 — Slack 실 webhook 활성

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-1-1 | `infrastructure/secrets-template/slack-webhook.env.sample` — 사내 IT 발급 URL placeholder + 실 값은 secrets 폴더 (gitignore) | 0.2 |
| TK-EXT-1-2 | `application-prod.yml` — `scheduling.notification.slack.enabled=true` + `webhook-url=${SLACK_WEBHOOK_URL}` + `alerts-channel=#scheduling-alerts` + `critical-channel=#scheduling-critical`. DEV/STG 는 default false 유지 | 0.3 |
| TK-EXT-1-3 | PROD 환경 변수 안내 — `infrastructure/scripts/install-nssm-services.ps1` 에 `SLACK_WEBHOOK_URL` env 추가 (NSSM AppEnvironmentExtra) | 0.2 |
| TK-EXT-1-4 | 실 Slack 채널 1회 발송 시각 검증 — DeliveryEscalator overdue 시뮬 (테스트 schedule 추가) | 0.3 |

### ST-EXT-2 — Kakao 실 webhook 활성

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-2-1 | `application-prod.yml` — `scheduling.notification.kakao.enabled=true` + `webhook-url=${KAKAO_WEBHOOK_URL}` + `bot-token=${KAKAO_BOT_TOKEN}` | 0.2 |
| TK-EXT-2-2 | PROD env — KAKAO_WEBHOOK_URL + KAKAO_BOT_TOKEN NSSM AppEnvironmentExtra 추가. secrets-template 동봉 | 0.2 |
| TK-EXT-2-3 | Grafana 패널 추가 — `notify-sprint18.json` 의 "Kakao 도달률" 패널 확장: SUCCESS / FAILED / SKIPPED 카운트 + 도달률 % gauge (NS-04 KPI) | 0.3 |
| TK-EXT-2-4 | 실 Kakao 1회 발송 시각 검증 — Critical Diff 시뮬 (audit_log + kakao_delivery_log row 검증) | 0.3 |

### ST-EXT-3 — WireMock 도입 + Resilience4j 정밀 검증

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-3-1 | `gradle/libs.versions.toml` — `wiremock = { module = "org.wiremock:wiremock-standalone", version = "3.9.2" }` 추가 + notify/app testImplementation | 0.2 |
| TK-EXT-3-2 | `WireMockSlackIT` — WireMock server stub 5xx 5회 연속 응답 → SlackNotifier.alert 3회 retry 후 fallback → 4회 연속 5xx 후 circuit OPEN 검증 (`resilience4j_circuitbreaker_state{name=slack}` metric 변화) | 0.3 |
| TK-EXT-3-3 | `WireMockKakaoIT` — 동일 패턴 — Kakao webhook 5xx → 3회 retry 후 KakaoDeliveryAttempt FAILED 영속 + circuit OPEN | 0.3 |
| TK-EXT-3-4 | half-open 전이 검증 — circuit OPEN 30초 후 자동 HALF_OPEN + 3 calls permitted + 성공 시 CLOSED 복귀 | 0.2 |

### ST-EXT-4 — 사용자 매뉴얼 §6 갱신

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-4-1 | USER_MANUAL_v1.1.md — §6 장애 대응에 2건 추가: (1) Slack #scheduling-critical push 미수신 → Grafana resilience4j_circuitbreaker_state{name=slack} 확인, (2) Kakao 도달률 < 90% → KAKAO_BOT_TOKEN 만료 확인 + IT_OPS 갱신 | 0.3 |
| TK-EXT-4-2 | 매뉴얼 v1.1 개정 이력 추가 + 베타 사용자 8명 재배포 | 0.2 |

### ST-EXT-5 — rbac-matrix.md v1.5

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-5-1 | rbac-matrix.md v1.5 — Slack/Kakao 발송 actor = `system:webhook` 명시 + audit_log 강제 (BR-X02) | 0.3 |
| TK-EXT-5-2 | 베타 사용자 매뉴얼 cross-reference 갱신 (rbac-matrix link 추가) | 0.2 |

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ `application-prod.yml` Slack/Kakao 실 webhook URL/token 적용
2. ✅ 실 Slack 채널 1회 발송 시각 검증 (사내 #scheduling-critical)
3. ✅ 실 Kakao 1회 발송 시각 검증 (PLANNER1 비즈 메시지 수신)
4. ✅ Grafana "Kakao 도달률" 패널 정상 표시 (NS-04 KPI)
5. ✅ WireMock IT — Slack circuit OPEN 자동 전이 + 30초 후 HALF_OPEN 복귀
6. ✅ WireMock IT — Kakao 3회 retry 후 KakaoDeliveryAttempt FAILED 영속
7. ✅ 사용자 매뉴얼 v1.1 §6 11건 + 베타 8명 재배포
8. ✅ rbac-matrix v1.5 — Slack/Kakao actor `system:webhook` 감사 강화

**비기능 DoD:**
1. ✅ ArchUnit GREEN
2. ✅ Backend IT 신규 2+ (WireMockSlackIT + WireMockKakaoIT) + 회귀 0 (303+ GREEN)
3. ✅ Resilience4j 자동 half-open 전이 동작 검증
4. ✅ DEV/STG 환경 default false 유지 (실 webhook 영향 0)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| 사내 IT Slack 채널 발급 지연 | S20 진입 불가 | Phase 4 진입 직전 1주 협의 시작. 발급 지연 시 WireMock IT (ST-EXT-3) 먼저 진행 가능 |
| Kakao Workplace Bot 계약 지연 | S20 ST-EXT-2 불가 | 동일 — WireMock IT 만 + 실 발송은 S21 carry-over |
| WireMock 3.9.2 의 Spring Boot 3.5 호환 미확인 | 도입 fail | Spring Cloud Contract WireMock 또는 testcontainers-mockserver fallback |
| 실 PROD secrets (webhook URL/token) Git 노출 위험 | 보안 사고 | `infrastructure/secrets-template/*.env.sample` 만 commit, 실 값은 NSSM env 또는 PowerShell SecureString |
| Resilience4j HALF_OPEN 전이 30s timing 차이 (테스트 flaky) | IT 가끔 fail | Awaitility atMost(35s) + retry assertion |
| Kakao 도달률 측정 baseline 0 (token 미발급 시) | KPI 패널 빈 화면 | Grafana 패널 default "No data — config 활성 필요" annotation |

---

## 7. 작업 순서 추천

**Day 1** — Slack/Kakao 실 활성 (병렬):
1. TK-EXT-1-1~4 (Slack)
2. TK-EXT-2-1~4 (Kakao)

**Day 2** — WireMock IT:
3. TK-EXT-3-1 (WireMock 의존 추가)
4. TK-EXT-3-2~4 (Slack + Kakao + half-open IT)

**Day 3** — 문서 + DoD:
5. TK-EXT-4-1~2 (매뉴얼 v1.1)
6. TK-EXT-5-1~2 (rbac-matrix v1.5)
7. **DoD 본 PC 시각 검증** — Slack/Kakao 실 발송 1회 + circuit OPEN 시뮬 1회

**총 ~2 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Config | `application-prod.yml` (Slack/Kakao enabled=true + env 변수) |
| Infra | `infrastructure/secrets-template/slack-webhook.env.sample` + `kakao-webhook.env.sample` |
| Infra | `install-nssm-services.ps1` 갱신 (SLACK_WEBHOOK_URL + KAKAO_WEBHOOK_URL + KAKAO_BOT_TOKEN env 추가) |
| Backend IT | `WireMockSlackIT.java` (4 cases) + `WireMockKakaoIT.java` (3 cases) |
| Infra Grafana | `notify-sprint18.json` Kakao 도달률 패널 확장 |
| Docs | `USER_MANUAL_v1.1.md` + `docs/security/rbac-matrix_v1.5.md` |

---

## 9. Sprint 20 후 다음 단계

**Sprint 21 (EP-CRUD-MASTER-2) 진입 조건:**
- ✅ DoD 12/12 충족
- ✅ 실 Slack/Kakao 발송 시각 검증 (베타 사용자 1주 운영 데이터에서 KPI 측정 시작)

**Sprint 21 첫 작업** — PLAN-SPRINT-21 작성 (5 entity CRUD UI 완성).

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 첫 sprint EP-EXT-WEBHOOK 5 Story / 17 Task / ~4 SP 분해. Sprint 18 carry-over (Slack/Kakao 실 webhook + WireMock fallback 정밀 검증) 해소. DoD 12 + 리스크 6 + 3-Day 작업 순서. Pre-Phase 사내 IT/관리팀 협의 1주 전제. PROD env 분리 (DEV/STG default false 유지) + secrets-template 보안. |
