# Sprint 20 진입 계획 — EP-EXT-WEBHOOK + EP-COST-ZERO (Slack 실 webhook + AG Grid Community + Kakao 보류) v1.1

**작성일**: 2026-05-28 | **버전**: 1.1 | **상태**: Phase 4 첫 sprint 진입 권고안 — **무료화 통합**

> **참조**: [v1.0 (Kakao 활성 포함)](PLAN-SPRINT-20_EP-EXT-WEBHOOK_v1.0.md) — 본 v1.1 이 cost-zero 정책 반영하여 상위 buffer / [PHASE-4_STABILIZATION_v1.1](PHASE-4_STABILIZATION_v1.1.md) §3 S20 + [WBS v1.15 §6 carry-over](TASK-001_WBS_v1.15.md) + [PLAN-SPRINT-18_EP-NOTIFY_v1.0](PLAN-SPRINT-18_EP-NOTIFY_v1.0.md)

---

## 0. v1.0 → v1.1 변경 요약 (Cost-Zero 통합)

| 항목 | v1.0 (Kakao 활성) | v1.1 (무료화) |
|---|---|---|
| **Kakao 알림톡** | 실 webhook + biz token + 도달률 KPI 활성 (월 ~1만원) | **❌ 보류 → Phase 5+ carry-over** (config flag false 영구). 코드는 stub 유지 |
| **AG Grid Enterprise** | (Sprint 20 범위 외 — license $999/년 trial 유지) | **➕ Community 다운그레이드 (ST-COST-1)** — $0 |
| **Sentry SaaS** | (문서·계획만 — 실 의존성 0건) | **➕ 미도입 공식화 (ST-COST-2)** — $0, Loki + Prometheus 만으로 관측 |
| Sprint SP | ~4 SP | **~3.6 SP** (Kakao 1.0 삭제 + COST 1.5 추가, net +0.5) |
| Sprint PD | ~2 PD | **~2 PD** (유지) |
| Story 수 | 5 | **6** (ST-EXT-2 삭제 + ST-COST-1·2 추가) |
| Task 수 | 17 | **17** (Kakao 4 task 삭제 + COST 4 task 추가) |
| Pre-Phase 의존 | 사내 IT Slack + Kakao biz token | **사내 IT Slack 만** (Kakao 협의 불필요) |
| 연 운영 비용 | ~$2,469 + 12만원 (AG Grid trial 위반 위험 + Kakao) | **$0 / 0원** |

→ **사용자 10명 이내 운영 기준 — 모든 유료 항목 무료 대안으로 전환.**

---

## 1. 목적

**Phase 4 운영 안정화 첫 sprint — Sprint 18 EP-NOTIFY 의 Slack stub 을 실 webhook 으로 활성 + Sprint 18 carry-over Resilience4j fallback 정밀 검증 (WireMock 도입) + 비용 무료화 (AG Grid Community + Kakao 보류 + Sentry 미도입 공식화).**

| 항목 | Sprint 18 baseline | Sprint 20 v1.1 |
|---|---|---|
| Slack alert | config `slack.enabled=false` (LOG only) | ✅ `application-prod.yml` 실 webhook URL + 채널 라우팅 (`#scheduling-alerts` / `#scheduling-critical`) |
| Kakao 도달 | config `kakao.enabled=false` (LOG only) | ⏸ **Phase 5+ carry-over** — config flag false 영구 + 주석 "1년 뒤 부활 가능" |
| Resilience4j circuit OPEN 시뮬 | 미검증 (코드만) | ✅ WireMock 5xx 강제 → 3회 retry → OPEN 전이 → fallback LOG 검증 (Slack 만) |
| 매뉴얼 §6 장애 대응 | 9건 | ✅ +1 (Slack 발송 실패 절차) = **10건** (Kakao 1건 미추가) |
| rbac-matrix | v1.4 (Sprint 18) | ✅ v1.5 — Slack 발송 actor = `system:webhook` audit 강화 (Kakao actor 미추가) |
| **AG Grid Enterprise** | trial license (~$999/년 위반 위험) | ✅ **Community 다운그레이드 (MIT)** — watermark 제거 + 합법 + $0 |
| **Sentry SaaS** | 문서/계획만 (실 의존성 0건) | ✅ **미도입 공식화** — Loki + Prometheus + Grafana 로 충분 |

**Pre-Phase 의존 (Sprint 20 진입 직전 완료 필요):**
- 사내 IT — Slack workspace 사내 채널 `#scheduling-alerts` + `#scheduling-critical` 발급
- ~~사내 관리팀 — Kakao Workplace Bot 계약 + biz token + webhook URL 발급~~ → **Phase 5+ carry-over**

**활성 후 효과:**
- 실 Slack 채널에 Critical Diff 1분 overdue 자동 push (운영팀 즉시 인지)
- Resilience4j OPEN/HALF_OPEN 자동 전이 실 검증 → 장애 시 fail-fast + 자동 복구 보장
- **AG Grid watermark 제거** — 사용자 화면 깨끗 + 법적 위험 0
- **연 운영 비용 $0** (AG Grid Community + Slack Free + Sentry 미도입 + Kakao 보류 + Docker Engine)

---

## 2. Sprint 20 SP·기간 (v1.1)

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-EXT-1 Slack 실 webhook 적용 + 채널 라우팅 검증 | 1.0 | 0.5 |
| ~~ST-EXT-2 Kakao 실 webhook~~ | ~~1.0~~ | **Phase 5+** |
| ST-EXT-3 WireMock 도입 + Resilience4j circuit OPEN 강제 시뮬 IT (Slack 만) | 0.5 | 0.3 |
| ST-EXT-4 사용자 매뉴얼 §6 갱신 (Slack 발송 실패 절차 +1건) | 0.3 | 0.2 |
| ST-EXT-5 rbac-matrix.md v1.5 (Slack actor 감사) | 0.3 | 0.2 |
| **ST-COST-1 AG Grid Enterprise → Community 다운그레이드** | **1.0** | **0.5** |
| **ST-COST-2 Kakao 보류 + Sentry 미도입 문서 정리 (SRS · CLAUDE.md · PHASE-4)** | **0.5** | **0.3** |
| **합계** | **~3.6 SP** | **~2 PD** |

> **PHASE-4 §3 계획 v1.1 정합** (4 SP → ~3.6 SP, Story 5 → 6, Task 17 → 17 유지).

---

## 3. 의존성 DAG (v1.1)

```
Pre-Phase (사내 IT Slack 채널 발급)
    ↓
ST-EXT-1 (Slack 실 webhook)
    ↓
ST-EXT-3 (WireMock IT - Slack 만) ──→ ST-EXT-4 (매뉴얼 +1건)
                                          ↓
                                  ST-EXT-5 (rbac-matrix Slack actor)

  [병렬 — 독립 작업]
ST-COST-1 (AG Grid Community) ←→ ST-COST-2 (문서 정리)
```

**병렬 윈도우:**
- **ST-EXT-1 ↔ ST-COST-1 ↔ ST-COST-2** — 3 Story 완전 독립 (Slack backend / Frontend grid / 문서)
- **ST-EXT-4 ↔ ST-EXT-5** — 문서 작업 병렬

---

## 4. Story · Task 매트릭스 (v1.1)

### ST-EXT-1 — Slack 실 webhook 활성 (v1.0 동일)

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-1-1 | `infrastructure/secrets-template/slack-webhook.env.sample` — 사내 IT 발급 URL placeholder + 실 값은 secrets 폴더 (gitignore) | 0.2 |
| TK-EXT-1-2 | `application-prod.yml` — `scheduling.notification.slack.enabled=true` + `webhook-url=${SLACK_WEBHOOK_URL}` + `alerts-channel=#scheduling-alerts` + `critical-channel=#scheduling-critical`. DEV/STG 는 default false 유지 | 0.3 |
| TK-EXT-1-3 | PROD 환경 변수 안내 — `infrastructure/scripts/install-nssm-services.ps1` 에 `SLACK_WEBHOOK_URL` env 추가 (NSSM AppEnvironmentExtra) | 0.2 |
| TK-EXT-1-4 | 실 Slack 채널 1회 발송 시각 검증 — DeliveryEscalator overdue 시뮬 (테스트 schedule 추가) | 0.3 |

### ~~ST-EXT-2 — Kakao 실 webhook 활성~~ → **Phase 5+ carry-over**

**삭제 사유**: 사용자 10명 / Slack 모바일 push 로 충분 / Kakao 알림톡 월 ~1만원 비용 회피. `KakaoTalkClient` + `kakao_delivery_log` 코드는 stub 유지 (1년 뒤 부활 시 config flag true + biz token 발급만으로 즉시 활성).

### ST-EXT-3 — WireMock 도입 + Resilience4j 정밀 검증 (Slack 만)

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-3-1 | `gradle/libs.versions.toml` — `wiremock = { module = "org.wiremock:wiremock-standalone", version = "3.9.2" }` 추가 + notify/app testImplementation | 0.1 |
| TK-EXT-3-2 | `WireMockSlackIT` — WireMock server stub 5xx 5회 연속 응답 → SlackNotifier.alert 3회 retry 후 fallback → 4회 연속 5xx 후 circuit OPEN 검증 (`resilience4j_circuitbreaker_state{name=slack}` metric 변화) | 0.2 |
| TK-EXT-3-3 | half-open 전이 검증 — circuit OPEN 30초 후 자동 HALF_OPEN + 3 calls permitted + 성공 시 CLOSED 복귀 | 0.2 |
| ~~TK-EXT-3-4 Kakao WireMock IT~~ | **삭제 — Phase 5+** | ~~0.3~~ |

### ST-EXT-4 — 사용자 매뉴얼 §6 갱신 (Slack 1건만)

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-4-1 | USER_MANUAL_v1.1.md — §6 장애 대응에 1건 추가: Slack #scheduling-critical push 미수신 → Grafana `resilience4j_circuitbreaker_state{name=slack}` 확인 + 사내 IT webhook 토큰 갱신 절차 | 0.2 |
| TK-EXT-4-2 | 매뉴얼 v1.1 개정 이력 추가 + 베타 사용자 8명 재배포 | 0.1 |

### ST-EXT-5 — rbac-matrix.md v1.5 (Slack actor 만)

| Task | 내용 | SP |
|---|---|:--:|
| TK-EXT-5-1 | rbac-matrix.md v1.5 — Slack 발송 actor = `system:webhook` 명시 + audit_log 강제 (BR-X02) | 0.2 |
| TK-EXT-5-2 | 베타 사용자 매뉴얼 cross-reference 갱신 (rbac-matrix link 추가) | 0.1 |

### ST-COST-1 — AG Grid Enterprise → Community 다운그레이드 (신규)

**근거**: `frontend/package.json` 의 `ag-grid-enterprise@^35.3.0` 은 $999~$1,599/dev/년 license 필요. 사용자 10명 / 1500 row × 30 col 사용 범위 기준 **Range Selection / Master-Detail / Status Bar 만 손실**, 나머지 정렬·필터·페이지네이션·virtual scrolling 은 Community 동일 지원.

| Task | 내용 | SP |
|---|---|:--:|
| TK-COST-1-1 | `frontend/src/grid/agGridSetup.ts` — `ag-grid-enterprise` import 제거 + `AllEnterpriseModule` → `ClientSideRowModelModule` + `LicenseManager.setLicenseKey()` 호출 제거 + `VITE_AG_GRID_LICENSE_KEY` 환경변수 사용 제거 | 0.2 |
| TK-COST-1-2 | `frontend/package.json` — `ag-grid-enterprise` 제거 + `ag-grid-community@^35.3.0` 유지. `package-lock.json` regenerate (`npm install`) | 0.1 |
| TK-COST-1-3 | 4 grid 컴포넌트 회귀 — Enterprise 기능 사용 grep 후 제거 또는 폴백: VcRotationGrid · ExMatrixGrid · ProductSpecPage · CandidateConfirmTable · 기타. Range Selection (`enableRangeSelection`) → 다중행 선택 (`rowSelection: 'multiple'`) 폴백. Status Bar 제거. Master-Detail 미사용 확인 | 0.4 |
| TK-COST-1-4 | 회귀 검증 — `vitest` 82/82 GREEN 유지 + `npx playwright test` e2e GREEN + 본 PC 시각 검증 (VcSchedulePage · ExSchedulePage · MasterHub 3 페이지 1500 row 렌더링 정상) | 0.3 |

### ST-COST-2 — Kakao 보류 + Sentry 미도입 문서 정리 (신규)

| Task | 내용 | SP |
|---|---|:--:|
| TK-COST-2-1 | `backend/app/src/main/resources/application-prod.yml` — `scheduling.notification.kakao.enabled=false` 영구 + 주석 "Phase 5+ carry-over — 1년 뒤 부활 가능. 활성 시 config flag true + KAKAO_WEBHOOK_URL + KAKAO_BOT_TOKEN NSSM env 추가" | 0.1 |
| TK-COST-2-2 | `CLAUDE.md` §2 기술 스택 — Sentry 라인 제거 또는 "Phase 5+ optional" annotation. 관측 도구는 Prometheus + Grafana + Loki + Promtail 만 정식 명시 | 0.1 |
| TK-COST-2-3 | SRS v1.6 작성 (`Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.6.md`) — REQ-NF-USA-003 Kakao 알림 Phase 5+ carry-over annotation + NS-04 KPI 도달률 Phase 5+ deferred. 개정 이력 추가 (v1.5 → v1.6) | 0.2 |
| TK-COST-2-4 | `docs/cost-policy/COST-ZERO_POLICY_v1.0.md` (신규) — 본 시스템 무료화 정책 문서. 5 유료 항목 + 무료 대안 + 1년 뒤 부활 비용 표. 사용자 결정 근거 (10명 이내) 명시 | 0.1 |

---

## 5. Definition of Done (DoD) v1.1

**기능적 DoD:**
1. ✅ `application-prod.yml` Slack 실 webhook URL 적용 (Kakao = enabled false 영구)
2. ✅ 실 Slack 채널 1회 발송 시각 검증 (사내 #scheduling-critical)
3. ✅ Grafana Slack 채널 panel 정상 표시 (Resilience4j circuit state)
4. ✅ WireMock IT — Slack circuit OPEN 자동 전이 + 30초 후 HALF_OPEN 복귀
5. ✅ 사용자 매뉴얼 v1.1 §6 10건 + 베타 8명 재배포
6. ✅ rbac-matrix v1.5 — Slack actor `system:webhook` 감사 강화

**비용 무료화 DoD (신규):**
7. ✅ AG Grid Community 다운그레이드 — watermark 제거 + license 위반 위험 0 + vitest 82/82 GREEN
8. ✅ Kakao 보류 공식화 — application-prod.yml + SRS v1.6 + COST-ZERO_POLICY_v1.0 문서 3건
9. ✅ Sentry 미도입 공식화 — CLAUDE.md §2 갱신
10. ✅ 본 PC 시각 검증 — VcSchedulePage / ExSchedulePage 1500 row × 30 col 정상 렌더링 (Community 로)

**비기능 DoD:**
11. ✅ ArchUnit GREEN
12. ✅ Backend IT 신규 1+ (WireMockSlackIT) + 회귀 0 (303+ GREEN)
13. ✅ Frontend vitest 82/82 GREEN + Playwright e2e GREEN
14. ✅ Resilience4j 자동 half-open 전이 동작 검증
15. ✅ DEV/STG 환경 default false 유지 (실 Slack 영향 0)

---

## 6. 리스크 + 회피 (v1.1)

| 리스크 | 영향 | 회피 |
|---|---|---|
| 사내 IT Slack 채널 발급 지연 | S20 진입 불가 | Phase 4 진입 직전 1주 협의 시작. 발급 지연 시 WireMock IT (ST-EXT-3) + AG Grid Community (ST-COST-1) 먼저 진행 가능 |
| WireMock 3.9.2 의 Spring Boot 3.5 호환 미확인 | 도입 fail | Spring Cloud Contract WireMock 또는 testcontainers-mockserver fallback |
| 실 PROD secrets (webhook URL) Git 노출 위험 | 보안 사고 | `infrastructure/secrets-template/*.env.sample` 만 commit, 실 값은 NSSM env 또는 PowerShell SecureString |
| Resilience4j HALF_OPEN 전이 30s timing 차이 (테스트 flaky) | IT 가끔 fail | Awaitility atMost(35s) + retry assertion |
| **AG Grid Community 다운그레이드 후 Range Selection 기능 손실** | **사용자 UX 저하** | **(1) 다중행 선택 (rowSelection: multiple) 폴백**, (2) 사용자 매뉴얼 §3 변경점 안내, (3) 1년 뒤 Range Selection 요구 시 Single Dev License $999/년 옵션 carry-over |
| **AG Grid Enterprise 잔여 import 누락 (TK-COST-1-3)** | **컴파일/런타임 실패** | grep `ag-grid-enterprise` + `enableRangeSelection` + `statusBar` + `masterDetail` 전수 조사 후 일괄 제거 |
| **Kakao 보류 후 PLANNER 도달 실패 호소** | **베타 1개월 후 재도입 요청** | Phase 5+ carry-over 명시 + COST-ZERO_POLICY_v1.0 의 "1년 뒤 부활 비용 ~0.5 PD" 보장 |

---

## 7. 작업 순서 추천 (v1.1)

**Day 1** — Slack backend + AG Grid frontend (병렬):
1. TK-EXT-1-1~4 (Slack 실 webhook 적용)
2. TK-COST-1-1~2 (AG Grid Community 의존성 전환 — 병렬 가능)

**Day 2** — WireMock IT + Frontend 회귀:
3. TK-EXT-3-1~3 (Slack WireMock IT)
4. TK-COST-1-3~4 (4 grid 컴포넌트 회귀 + 본 PC 시각 검증)

**Day 3** — 문서 + DoD:
5. TK-EXT-4-1~2 (매뉴얼 §6 +1건)
6. TK-EXT-5-1~2 (rbac-matrix v1.5)
7. TK-COST-2-1~4 (Kakao + Sentry + SRS v1.6 + COST-ZERO_POLICY v1.0)
8. **DoD 본 PC 시각 검증** — Slack 실 발송 1회 + circuit OPEN 시뮬 1회 + AG Grid 1500 row 렌더링

**총 ~2 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables) v1.1

| 분류 | 파일 |
|---|---|
| Backend Config | `application-prod.yml` (Slack enabled=true, Kakao enabled=false 영구 + 주석) |
| Infra | `infrastructure/secrets-template/slack-webhook.env.sample` |
| Infra | `install-nssm-services.ps1` 갱신 (SLACK_WEBHOOK_URL env 추가) |
| Backend IT | `WireMockSlackIT.java` (3 cases) |
| Frontend | `frontend/src/grid/agGridSetup.ts` (Community 전환), `package.json` (ag-grid-enterprise 제거), 4 grid 컴포넌트 |
| Docs | `USER_MANUAL_v1.1.md` + `docs/security/rbac-matrix_v1.5.md` |
| Docs (신규) | `docs/cost-policy/COST-ZERO_POLICY_v1.0.md` |
| SRS | `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.6.md` (Kakao Phase 5+ + NS-04 deferred) |
| CLAUDE.md | §2 Sentry 라인 정리 |

---

## 9. Sprint 20 후 다음 단계

**Sprint 21 (EP-CRUD-MASTER-2) 진입 조건:**
- ✅ Sprint 20 v1.1 DoD 15/15 충족
- ✅ 실 Slack 발송 시각 검증 (베타 사용자 1주 운영 데이터 누적 시작)
- ✅ AG Grid Community 1주 운영 사용자 피드백 (Range Selection 기능 손실 영향 측정)

**Sprint 21 첫 작업** — PLAN-SPRINT-21 작성 (5 entity CRUD UI 완성).

**Phase 5+ carry-over (Sprint 20 → 5+ 신규 누적):**
- Kakao 알림톡 부활 (~0.5 PD, config flag + biz token 만)
- AG Grid Range Selection 재도입 (필요 시, Single Dev License $999/년 또는 TanStack Table 자체 구현 ~3 PD)
- Sentry SaaS 도입 (필요 시, Loki 검색 부담 증가 시점에 재판단)

---

## 10. 무료화 정책 요약 (참조 — COST-ZERO_POLICY_v1.0 본문)

| 항목 | Before (유료) | After (무료) | 절감 |
|---|---|---|---|
| AG Grid Enterprise | $999~$1,599/dev/년 | Community (MIT) | $999~$1,599/년 |
| Kakao 알림톡 | 월 ~1만원 (연 12만원) | 보류 — Phase 5+ | 연 12만원 |
| Slack Pro | $7.25/user/월 (연 ~$870 @ 10명) | Free plan | $870/년 |
| Sentry SaaS | Free tier ~ Team $26/월 | 미도입 (Loki only) | $0~$312/년 |
| Docker Desktop | $5/user/월 @ 250+ 회사 | Docker Engine + WSL2 | $600/년 (조건부) |
| **연간 총 절감** | ~$2,469 + 12만원 | **$0 / 0원** | **~$2,469/년** |

**판단 기준**: 사용자 10명 이내 사내 운영 / 외부 노출 없음 / 베타 단계.

---

## 11. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 첫 sprint EP-EXT-WEBHOOK 5 Story / 17 Task / ~4 SP 분해. Sprint 18 carry-over (Slack/Kakao 실 webhook + WireMock fallback 정밀 검증) 해소. DoD 12 + 리스크 6 + 3-Day 작업 순서. Pre-Phase 사내 IT/관리팀 협의 1주 전제. PROD env 분리 (DEV/STG default false 유지) + secrets-template 보안. |
| **1.1** | **2026-05-28** | **Claude Code** | **Cost-Zero 통합 — 사용자 10명 이내 운영 전제. (1) Kakao Story (ST-EXT-2) Phase 5+ carry-over 보류 (월 ~1만원 절감 + biz token 협의 회피). (2) AG Grid Enterprise ($999~$1,599/dev/년) → Community 다운그레이드 (ST-COST-1, MIT, Range Selection 폴백). (3) Sentry SaaS 미도입 공식화 (ST-COST-2, Loki + Prometheus 로 충분). (4) Slack Free plan + Docker Engine 무료 확인. (5) WireMock IT 는 Slack 만 (Kakao IT 삭제). 산출물: SRS v1.6 + COST-ZERO_POLICY_v1.0 신규. SP 4 → 3.6, Story 5 → 6, Task 17 → 17, PD 2 (유지). 연 운영 비용 ~$2,469 + 12만원 → $0.** |
