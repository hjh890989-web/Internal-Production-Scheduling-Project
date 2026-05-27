# Sprint 19 진입 계획 — EP-BETA-LAUNCH (베타 cutover 최종) v1.0

**작성일**: 2026-05-28 | **버전**: 1.0 | **상태**: Sprint 19 작업 시작 직전

> **참조**: [WBS v1.5 §5 Sprint 19 baseline](TASK-001_WBS_v1.5.md) + [WBS v1.14 §6 carry-over](TASK-001_WBS_v1.14.md) + [PLAN-SPRINT-18_EP-NOTIFY_v1.0](PLAN-SPRINT-18_EP-NOTIFY_v1.0.md)

---

## 1. 목적

**Sprint 18 EP-NOTIFY 직후 진입 — 표준 베타 plan 마지막 sprint.** Sprint 10~18 누적 자산 (인증/RBAC/마스터 UI/수주통합/성형/압출/확정/당일락/MES/알림) 위에 **베타 cutover 6 deliverable** 완료:

| 산출물 | 적용 단계 |
|---|---|
| **99999-SAMPLE PROD cleanup script** (V045 cutover) | Flyway migration baseline 후 sample seed 제거 |
| **본 PC E2E 통합 시나리오 IT** | Sprint 13~18 단일 시나리오 (수주 import → diff → commit → VC chain → 확정 → EX → 알림) |
| **Grafana dashboard JSON** | Prometheus scrape 활성 metric 시각화 (운영 가시성) |
| **사용자 매뉴얼 v1.0** | 4 role (PLANNER / STK_USER / IT_OPS / READ_ONLY) SOP markdown |
| **NSSM Windows 자동시작** | Backend + Frontend 서비스 등록 (PC 재부팅 자동 복구) |
| **베타 Go/No-Go 체크리스트** | cutover 진입 전 11 항목 verifyable list |

**현황 인벤토리 (Sprint 18 직전 상태):**
- ✅ E2E IT 1건 (DiffNotifyEndToEndIT) + 모듈별 IT 62/62 GREEN
- ✅ Prometheus + Micrometer + Spring Actuator 활성 (`/api/actuator/prometheus`)
- ✅ Docker Compose STG/PROD + NGINX TLS/gzip (`infrastructure/`)
- ✅ Blue/Green container baseline (PROD compose)
- ⏳ **99999-SAMPLE PROD cleanup** — V039/V040 seed 운영 cutover 시점 제거 필요
- ⏳ **본 PC 통합 E2E IT** — 단일 시나리오 결합 미진행 (모듈별 분산)
- ⏳ **Grafana dashboard** — 미존재 (Prometheus metric 수집 만, 시각화 없음)
- ⏳ **사용자 매뉴얼** — `docs/operations/initial-users-table.md` 1건만 (Sprint 10 sourced), 4 role SOP 미존재
- ⏳ **NSSM 자동시작** — 미존재 (수동 PowerShell 두 창 운영)
- ⏳ **Go/No-Go 체크리스트** — 미존재

**활성 후 효과 (베타 진입 완료):**
- 단일 PC 재부팅 → backend/frontend 자동 복구 → 베타 사용자 무중단
- E2E 단일 시나리오 IT → 회귀 1건 실행으로 전체 chain 보장
- Grafana 대시보드 → 운영자가 metric 실시간 모니터 (Critical SLA, Kakao 도달률 등)
- 사용자 매뉴얼 → 4 role 별 SOP (로그인/주요 작업/장애 대응)
- 99999-SAMPLE 제거 → PROD DB 깨끗한 상태로 cutover

---

## 2. Sprint 19 SP·기간

| Story | SP | 추정 PD (1인 AI 가속) |
|---|:--:|:--:|
| ST-BETA-1 V045 99999-SAMPLE cleanup migration | 0.5 | 0.3 |
| ST-BETA-2 본 PC E2E 통합 시나리오 IT 신규 (Sprint 13~18 합본 1 시나리오) | 1.2 | 0.6 |
| ST-BETA-3 Grafana dashboard JSON (Critical SLA + Kakao 도달률 + degraded + HTTP p95) | 0.8 | 0.4 |
| ST-BETA-4 사용자 매뉴얼 v1.0 (4 role SOP markdown) | 1.0 | 0.5 |
| ST-BETA-5 NSSM Windows 자동시작 (backend + frontend 서비스 등록 스크립트 + README) | 0.5 | 0.3 |
| ST-BETA-6 베타 Go/No-Go 체크리스트 + cutover runbook | 0.5 | 0.3 |
| ST-BETA-7 (선택) 장비/셋팅 CRUD UI 2 entity (vc_machine + setting_group) 활성 | 0.5 | 0.3 |
| **합계** | **~5 SP** | **~2.7 PD** |

> **WBS v1.5 계획 3 SP → 실 5 SP** (carry-over 흡수 — 매뉴얼/NSSM/CRUD UI 통합으로 +2 SP).

---

## 3. 의존성 DAG

```
ST-BETA-1 (V045 cleanup) ─┐
                          │
ST-BETA-3 (Grafana JSON)  │
                          ↓
ST-BETA-2 (E2E IT) ──────→ ST-BETA-6 (Go/No-Go runbook)
                          ↑
ST-BETA-4 (매뉴얼 v1.0) ──┤
                          │
ST-BETA-5 (NSSM 자동시작) │
                          │
ST-BETA-7 (CRUD UI 2 entity, optional) ┘
```

**병렬 윈도우:**
- **ST-BETA-1 / 3 / 4 / 5 / 7** 모두 독립 — Day 1~2 동시 진행
- **ST-BETA-2** (E2E IT) 는 Sprint 13~18 IT 회귀 위에서 만든다 — 전제 sprint 완료 후 작업

---

## 4. Story · Task 매트릭스

### ST-BETA-1 — V045 99999-SAMPLE cleanup migration

| Task | 내용 | SP |
|---|---|:--:|
| TK-BETA-1-1 | V045__cleanup_sample_seed.sql — `DELETE FROM app.vc_schedule WHERE hose_id LIKE '99999-SAMPLE-%'` + `DELETE FROM app.ex_schedule_candidate WHERE hose_id LIKE '99999-SAMPLE-%'`. Flyway profile 분기 (`scheduling.cutover.enabled=true` 일 때만 발화 — STG 검증 후 PROD 활성) | 0.3 |
| TK-BETA-1-2 | IT — V045 발화 후 99999-SAMPLE row 0건 verify + 운영 데이터 보존 | 0.2 |

### ST-BETA-2 — 본 PC E2E 통합 시나리오 IT

| Task | 내용 | SP |
|---|---|:--:|
| TK-BETA-2-1 | `BetaE2EIntegrationIT` — 단일 시나리오: (1) PLANNER 수주 import → (2) trackingId diff → commit → (3) OrderCommittedEvent → VC chain → (4) VcConfirmedEvent → EX cascade → (5) DegradedModeChangedEvent 강제 → Slack/STOMP push → (6) Drawer 누적 검증. ~5분 실행 | 0.8 |
| TK-BETA-2-2 | IT 안 actor 다양화 — PLANNER1 작성 + PLANNER2 확정 (BR-X05 dual-review 실 흐름) | 0.2 |
| TK-BETA-2-3 | 회귀 — Sprint 16/17/18 ITs 모두 GREEN 재확인 | 0.2 |

### ST-BETA-3 — Grafana dashboard JSON

| Task | 내용 | SP |
|---|---|:--:|
| TK-BETA-3-1 | `infrastructure/observability/grafana-dashboard.json` — 4 패널: (1) Critical 알림 1분 overdue count, (2) Kakao 도달률 (SUCCESS / FAILED + SKIPPED), (3) MES degraded duration per machine, (4) HTTP request p95 latency by endpoint | 0.5 |
| TK-BETA-3-2 | docker-compose 에 Grafana 컨테이너 추가 (Prometheus datasource 자동 provision) | 0.2 |
| TK-BETA-3-3 | README — Grafana 접속 (port 3000) + 초기 admin/admin + dashboard import 수동 절차 | 0.1 |

### ST-BETA-4 — 사용자 매뉴얼 v1.0

| Task | 내용 | SP |
|---|---|:--:|
| TK-BETA-4-1 | `docs/manual/USER_MANUAL_v1.0.md` — 공통: 로그인/로그아웃/PIN 변경/메뉴 nav | 0.3 |
| TK-BETA-4-2 | PLANNER 섹션 — 수주 import, diff 확정, VC 시뮬뷰, batch 확정, 일중 override, 본인 작성 dual-review | 0.3 |
| TK-BETA-4-3 | STK_USER 섹션 — 시뮬뷰 read-only, swap 제안 등록 | 0.1 |
| TK-BETA-4-4 | IT_OPS 섹션 — 마스터 데이터 입력 (user/priority/kd), 사번 잠금 해제, Excel 폴백 입력 | 0.2 |
| TK-BETA-4-5 | READ_ONLY 섹션 — 감사 로그, 시뮬뷰, 압출 매트릭스 조회 | 0.1 |

### ST-BETA-5 — NSSM Windows 자동시작

| Task | 내용 | SP |
|---|---|:--:|
| TK-BETA-5-1 | `infrastructure/scripts/install-nssm-services.ps1` — NSSM 설치 + scheduling-backend (gradlew bootRun) + scheduling-frontend (npm run dev) 서비스 등록 + auto-start | 0.3 |
| TK-BETA-5-2 | `infrastructure/scripts/uninstall-nssm-services.ps1` — 제거 스크립트 + README 사용법 | 0.2 |

### ST-BETA-6 — 베타 Go/No-Go 체크리스트

| Task | 내용 | SP |
|---|---|:--:|
| TK-BETA-6-1 | `docs/cutover/BETA_GO_NOGO_CHECKLIST_v1.0.md` — 11 항목: (1) IT 62/62 GREEN, (2) E2E IT 1/1, (3) Grafana 4 패널, (4) 매뉴얼 v1.0 배포, (5) NSSM 자동시작, (6) V045 cleanup STG 검증, (7) 8명 베타 사용자 시드 검증, (8) PIN 0001~0008 동작, (9) Slack/Kakao config flag default false, (10) NGINX TLS 인증서 갱신 (사내 CA), (11) 백업 절차 (pg_basebackup) 실행 1회 | 0.4 |
| TK-BETA-6-2 | `docs/cutover/BETA_RUNBOOK_v1.0.md` — 단계별 실행 가이드 (T-1주, T-1일, T-1시간, T0 cutover, T+1시간 health check) | 0.1 |

### ST-BETA-7 (선택) — 장비/셋팅 CRUD UI 2 entity

| Task | 내용 | SP |
|---|---|:--:|
| TK-BETA-7-1 | VcMachineAdminPage — vc_machine 5대 (LP-01~04 + IC-01) CRUD UI. MasterHubPage 카드 disabled 해제 | 0.3 |
| TK-BETA-7-2 | SettingGroupAdminPage — setting_group 1~8 CRUD UI | 0.2 |

> **Sprint 19 가 빠듯하면 ST-BETA-7 은 Phase 4 carry-over** (실 운영 진입 후 마스터 갱신 시점에 작성).

---

## 5. Definition of Done (DoD)

**기능적 DoD:**
1. ✅ V045 cleanup migration STG 검증 → 99999-SAMPLE row 0건
2. ✅ BetaE2EIntegrationIT 단일 시나리오 GREEN (수주 import → 알림 누적까지)
3. ✅ Grafana dashboard 4 패널 정상 표시 (Prometheus datasource OK)
4. ✅ 사용자 매뉴얼 v1.0 4 role 섹션 + 스크린샷
5. ✅ NSSM 서비스 등록 후 PC 재부팅 → backend/frontend 자동 기동 (Started SchedulingApplication + Vite ready)
6. ✅ 베타 Go/No-Go 체크리스트 11/11 ✓
7. ✅ Sprint 13~18 회귀 IT 62/62 GREEN

**비기능 DoD:**
1. ✅ ArchUnit GREEN (전 모듈 boundary)
2. ✅ Backend IT 신규 1+ (BetaE2EIntegrationIT) + 회귀 62/62
3. ✅ TypeScript compile + frontend tests GREEN
4. ✅ V045 Flyway 적용 안정성 검증 (DEV/STG/PROD profile 분기)

---

## 6. 리스크 + 회피

| 리스크 | 영향 | 회피 |
|---|---|---|
| V045 cleanup PROD 적용 시 사용자 in-flight 데이터 손실 | 운영 데이터 삭제 | `WHERE hose_id LIKE '99999-SAMPLE-%'` 만 (sample namespace 정확 매칭). `scheduling.cutover.enabled=false` default → 명시적 활성 시만 발화 |
| E2E IT 5분 초과 → CI 시간 부담 | CI 비용 | TestContainers 재사용 + DirtiesContext.AFTER_CLASS (Sprint 10~18 패턴 정합) |
| Grafana dashboard import 수동 의존 | 환경 차이 | `infrastructure/observability/provisioning/` 폴더로 dashboard auto-provision (Grafana 시작 시 자동 import) |
| 매뉴얼 스크린샷 outdated 위험 | 사용자 혼란 | 스크린샷 별도 폴더 `docs/manual/screenshots/` + 매뉴얼 안에 작성일/버전 명시 |
| NSSM 서비스 실행 권한 부족 (사내 PC) | 자동시작 실패 | install 스크립트 안 `Start-Process -Verb RunAs` 관리자 권한 요청 |
| 베타 사용자 PIN 8개 초기 발급 → 보안 정책 위반 가능 | NFR-SEC-007 위반 | 매뉴얼 §보안 항목에 "첫 로그인 후 PIN 변경 권장" + 30일 후 강제 변경 정책은 Phase 4+ |

---

## 7. 작업 순서 추천 (TK chain)

**Day 1** — 인프라/Migration:
1. TK-BETA-1-1~2 (V045 + IT)
2. TK-BETA-3-1~3 (Grafana dashboard + provision)
3. TK-BETA-5-1~2 (NSSM 스크립트)

**Day 2** — 통합 IT + 문서:
4. TK-BETA-2-1~3 (E2E IT + 회귀)
5. TK-BETA-4-1~5 (사용자 매뉴얼)

**Day 3** — Cutover runbook + 검증:
6. TK-BETA-6-1~2 (Go/No-Go 체크리스트 + Runbook)
7. (선택) TK-BETA-7-1~2 (CRUD UI 2 entity)
8. **DoD 본 PC 시각 검증** — 11/11 체크리스트 실행

**총 ~2.7 PD (1인 AI 가속)** — 3 영업일 여유.

---

## 8. 산출물 (Deliverables)

| 분류 | 파일 |
|---|---|
| Backend Migration | V045__cleanup_sample_seed.sql (조건부 발화) |
| Backend IT | `BetaE2EIntegrationIT.java` (1 통합 시나리오) |
| Infra | `infrastructure/observability/grafana-dashboard.json` + Grafana 컨테이너 + provisioning |
| Infra Scripts | `infrastructure/scripts/install-nssm-services.ps1` + uninstall + README |
| Docs (Cutover) | `docs/cutover/BETA_GO_NOGO_CHECKLIST_v1.0.md` + `BETA_RUNBOOK_v1.0.md` |
| Docs (User) | `docs/manual/USER_MANUAL_v1.0.md` + `docs/manual/screenshots/` |
| Frontend (선택) | VcMachineAdminPage + SettingGroupAdminPage + MasterHubPage 카드 활성 |

---

## 9. Sprint 19 후 다음 단계 — 베타 운영 시작

**베타 진입 완료 후 즉시 진행:**
- 베타 사용자 8명 (PLANNER 3 + STK_USER 3 + IT_OPS 1 + READ_ONLY 1) 첫 로그인 + PIN 변경
- Grafana dashboard 모니터링 시작 (Critical SLA / Kakao 도달률 / MES degraded duration)
- 1주 시점: 실 운영 데이터 1주분 누적 → Allocator 정확도 검증 (Phase 4 KPI 측정 시작점)

**Phase 4 (베타 → 운영 안정화) carry-over:**
- Slack 실 webhook URL 발급 (사내 IT/관리팀 협의)
- Kakao biz token 발급 (Workplace Bot 계약)
- DaoAuthenticationProvider deprecation 리팩토링
- Resilience4j fallback 정밀 검증 (WireMock 도입)
- 사용자 매뉴얼 스크린샷 최신화

**Phase 5+ (운영 → 본격) carry-over:**
- MES 실 adapter (HTTP/MQ/file)
- Order 자동 INSERT chain (ImportOrchestrator → Allocator)
- Allocator priority/slot 알고리즘 운영 운용 결정

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — EP-BETA-LAUNCH 7 Story / 17 Task / ~5 SP 분해 (WBS v1.5 계획 3 SP → carry-over 흡수 +2 SP). 99999-SAMPLE cleanup + E2E 통합 IT + Grafana 4 패널 + 사용자 매뉴얼 v1.0 + NSSM 자동시작 + Go/No-Go 체크리스트 + (선택) CRUD UI 2 entity. DoD 11 + 리스크 6건. 3-Day. Sprint 10~18 누적 자산 위 최종 cutover, 베타 운영 시작 직전. Slack/Kakao 실 webhook (Phase 4+) 과 MES 실 adapter (Phase 5+) 는 carry-over. |
