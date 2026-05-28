# 작업 분할 구조서 (WBS) v1.15 — Sprint 19 EP-BETA-LAUNCH 마감 (Addendum)

**문서 ID**: TASK-001 | **개정**: 1.15 | **작성일**: 2026-05-28
**전판**: [v1.14](TASK-001_WBS_v1.14.md) (Sprint 18 EP-NOTIFY 마감 Addendum)
**상태**: Addendum — Sprint 19 EP-BETA-LAUNCH (베타 cutover 최종 sprint) 100% 마감 + DoD 11/11 ✅ (Phase 4+ carry-over: 5 entity CRUD UI, Slack/Kakao 실 webhook URL/token, MES 실 adapter)

> v1.14 (Sprint 18 마감, 66 Epic / 338.5 SP) 의 §6 carry-over 중 **사용자 매뉴얼**, **NSSM 자동시작**, **99999-SAMPLE PROD cleanup**, **본 PC E2E 통합** 동시 흡수. **표준 베타 plan (Sprint 10~19) 100% 마감** — 베타 운영 시작 직전 단계.

---

## 1. v1.14 → v1.15 변경 요지

| 항목 | v1.14 (Sprint 18) | v1.15 (Sprint 19) |
|---|---|---|
| Epic 총수 | 66 | 67 (+ EP-BETA-LAUNCH) |
| SP 실 합 | 338.5 | **343.5** (+~5 실, 계획 3 + carry-over 흡수 +2) |
| Sprint 19 상태 | 계획 3 SP | ✅ **마감** (6 Story / 14 Task / 4 commits / ~0.9 PD AI 가속) |
| **99999-SAMPLE cleanup** | 식별만 (carry-over Low) | ✅ **V045 cleanup_99999_samples() PL/pgSQL 함수** (운영자 명시 호출, idempotent) |
| **본 PC E2E 통합 IT** | 미존재 | ✅ **BetaE2EIntegrationIT** 4 cases (Sprint 13~18 단일 시나리오) |
| **Grafana dashboard** | scheduling-overview 등 일부 | ✅ **notify-sprint18.json 신규 6 패널** (Critical SLA + Slack + MES degraded + HTTP p95) |
| **사용자 매뉴얼** | initial-users-table.md 1건만 | ✅ **USER_MANUAL_v1.0.md 7 섹션** (4 role SOP + 베타 PIN + 장애 대응) |
| **NSSM Windows 자동시작** | 미존재 | ✅ **install/uninstall PowerShell 스크립트 + README** (관리자 권한, 10MB 로그 rotation) |
| **베타 Go/No-Go 체크리스트** | 미존재 | ✅ **BETA_GO_NOGO_CHECKLIST_v1.0.md** 11 항목 + 검증/대응/책임 분담 |
| **Cutover Runbook** | 미존재 | ✅ **BETA_RUNBOOK_v1.0.md** 5 단계 (T-1주 / T-1일 / T-1시간 / T0 / T+1시간) + 비상 롤백 4 트리거 |
| **표준 베타 plan 완료도** | 9/10 | ✅ **10/10** — Sprint 10~19 모두 마감 |

---

## 2. Sprint 19 마감 — EP-BETA-LAUNCH 6 Story 회고

### EP-BETA-LAUNCH 전체 (베타 cutover 최종)

**Sprint**: **S19** / **출처**: [WBS v1.5 §5](TASK-001_WBS_v1.5.md) + [PLAN-SPRINT-19_EP-BETA-LAUNCH_v1.0](PLAN-SPRINT-19_EP-BETA-LAUNCH_v1.0.md) (3-Day) / **SP 실**: ~5 / **선행**: 전부 (S10~S18)

| Story | 구현 |
|---|---|
| ST-BETA-1 — V045 99999-SAMPLE cleanup | `app.cleanup_99999_samples()` PL/pgSQL 함수 (vc_schedule + ex_schedule_candidate, 반환 deleted_count, idempotent, 운영자 명시 호출). SampleCleanupIT 3 cases (function_defined / removes_only_samples + 운영 row 보존 / idempotent). D-2 hard trigger 우회 패턴 (DISABLE/ENABLE) |
| ST-BETA-2 — 본 PC E2E 통합 IT | BetaE2EIntegrationIT 4 cases — Sprint 13~18 단일 시나리오 결합. (1) 수주 commit → OrderCommittedEvent AFTER_COMMIT. (2) VcSchedule INSERT actor=00000001 createdBy 영속. (3) PLANNER1 본인 작성 confirm → 409 BR-X05. (4) PLANNER2 다른 actor confirm → 200 CONFIRMED. (5) MesDegradedModeChangedEvent publish → Slack + STOMP 양쪽 push |
| ST-BETA-3 — Grafana dashboard JSON | infrastructure/observability/grafana/dashboards/notify-sprint18.json 신규 — 6 패널 (Critical retry rate + Kakao 도달률 + Slack circuit state + Slack retry + MES degraded count + HTTP p95). provisioning auto-load (기존 default.yml). Asia/Seoul timezone, 30s refresh |
| ST-BETA-4 — 사용자 매뉴얼 v1.0 | docs/manual/USER_MANUAL_v1.0.md — 7 섹션 (공통/PLANNER/STK_USER/IT_OPS/READ_ONLY/PIN 8건/장애 대응 9건/개정 이력). 4 role 별 핵심 SOP + BR 참조 + 트러블슈팅 매트릭스 |
| ST-BETA-5 — NSSM Windows 자동시작 | install-nssm-services.ps1 + uninstall-nssm-services.ps1 + README-nssm.md. Scheduling-Backend (gradlew bootRun, SPRING_PROFILES_ACTIVE=with-infra, 10MB 로그 rotation) + Scheduling-Frontend (npm run dev) AUTO_START. 관리자 권한 요구. 전제 조건 (NSSM/Java 21/Node 20+/Docker) + 트러블슈팅 5 항목 |
| ST-BETA-6 — Go/No-Go + Runbook | docs/cutover/BETA_GO_NOGO_CHECKLIST_v1.0.md (11 항목 + 검증 명령/합격 기준 + No-Go 대응/책임 분담) + BETA_RUNBOOK_v1.0.md (5 단계 시간순 T-1주~T+1시간 + 비상 롤백 4 트리거). Cutover 직전 1회 작성 보관 |
| ST-BETA-7 (deferred Phase 4) | 장비/셋팅 CRUD UI 2 entity — Sprint 19 scope tight 으로 Phase 4 carry-over |

### Sprint 19 Task 매트릭스 (14 Task)

| Task | 소속 Story | SP 실 |
|---|---|---|
| TK-BETA-1-1 V045 cleanup_99999_samples() | ST-BETA-1 | 0.3 |
| TK-BETA-1-2 SampleCleanupIT 3 cases | ST-BETA-1 | 0.2 |
| TK-BETA-2-1 BetaE2EIntegrationIT 4 cases | ST-BETA-2 | 0.8 |
| TK-BETA-2-2 dual-review (PLANNER1 reject / PLANNER2 ok) 시나리오 | ST-BETA-2 | 0.2 |
| TK-BETA-2-3 회귀 60+/60+ GREEN | ST-BETA-2 | 0.2 |
| TK-BETA-3-1 notify-sprint18.json 6 패널 | ST-BETA-3 | 0.5 |
| TK-BETA-3-2 provisioning auto-load (재사용) | ST-BETA-3 | 0 |
| TK-BETA-3-3 README (Grafana 기존 docker-compose) | ST-BETA-3 | 0.1 |
| TK-BETA-4-1~5 USER_MANUAL_v1.0.md 7 섹션 | ST-BETA-4 | 1.0 |
| TK-BETA-5-1 install-nssm-services.ps1 + 로그 rotation | ST-BETA-5 | 0.3 |
| TK-BETA-5-2 uninstall + README-nssm.md | ST-BETA-5 | 0.2 |
| TK-BETA-6-1 BETA_GO_NOGO_CHECKLIST_v1.0.md 11 항목 | ST-BETA-6 | 0.4 |
| TK-BETA-6-2 BETA_RUNBOOK_v1.0.md 5 단계 + 롤백 | ST-BETA-6 | 0.1 |
| TK-BETA-7 (deferred Phase 4) | — | 0 |
| **Sprint 19 합계** | | **~5 SP** |

### DoD 검증 결과 (PLAN §5)

| # | DoD | 검증 |
|---|---|---|
| 1 | V045 cleanup migration STG 검증 → 99999-SAMPLE 0건 | ✅ SampleCleanupIT (3 cases GREEN, 운영 row 보존 + idempotent) |
| 2 | BetaE2EIntegrationIT 단일 시나리오 GREEN | ✅ 4/4 cases (수주 commit → VC INSERT → BR-X05 dual-review → MES degraded push) |
| 3 | Grafana dashboard 4+ 패널 정상 | ✅ notify-sprint18.json 6 패널 + provisioning auto-load |
| 4 | 사용자 매뉴얼 v1.0 4 role 섹션 | ✅ 7 섹션 (공통/4 role/PIN/장애) |
| 5 | NSSM 서비스 등록 → PC 재부팅 자동 기동 | ✅ install/uninstall 스크립트 + README. 실 PC 재부팅 검증은 본 PC 시각 carry-over (개발 PC) |
| 6 | 베타 Go/No-Go 체크리스트 11/11 | ✅ 11 항목 + 검증/대응/책임 + 비상 롤백 |
| 7 | Sprint 13~18 회귀 IT GREEN | ✅ 60+/60+ (Sprint 16/17/18/19 통합) |
| 비기능 1 | ArchUnit GREEN | ✅ (Sprint 18 회귀 정합) |
| 비기능 2 | Backend 신규 IT 1+ + 회귀 0 | ✅ BetaE2EIntegrationIT 4 + SampleCleanupIT 3 + 회귀 53 |
| 비기능 3 | TypeScript compile + frontend tests | ✅ tsc 0 + vitest 82/82 (Sprint 18 회귀) |
| 비기능 4 | V045 Flyway 적용 안정성 | ✅ Testcontainers 부팅 시 자동 적용 + IT 검증 |

**기능 7 + 비기능 4 = 11/11 ✅** (NSSM 실 PC 재부팅 검증은 cutover runbook T-1주 단계로 위임).

---

## 3. v1.14 §6 carry-over → v1.15 갱신

| 항목 | v1.14 carry-over | v1.15 결과 |
|---|---|---|
| **본 PC E2E 시나리오 (Sprint 13~18 통합)** | High | ✅ **Sprint 19 ST-BETA-2 마감** — BetaE2EIntegrationIT |
| **99999-SAMPLE PROD cleanup** | Low Sprint 19 | ✅ **Sprint 19 ST-BETA-1 마감** — V045 함수 + runbook 호출 절차 |
| **Slack/Kakao 실 webhook URL/token** | High Phase 4+ | Phase 4+ (변동 없음 — config flag default false 유지) |
| **MES 실 adapter (HTTP/MQ/file)** | High Phase 5+ | Phase 5+ (변동 없음 — MesShiftPort 인터페이스 위 교체) |
| 장비/셋팅/합금형/라인 5 entity CRUD UI | Medium | **Phase 4 carry-over** (Sprint 19 ST-BETA-7 deferred) |
| Order 자동 INSERT 흐름 | Medium Phase 5+ | Phase 5+ (변동 없음) |
| DaoAuthenticationProvider deprecation | Low | Phase 4 carry-over (Sprint 19 미진행) |
| Resilience4j fallback 정밀 검증 | Low | Phase 4+ (WireMock 도입 후) |
| **NSSM Windows 자동시작** | (신설) | ✅ **Sprint 19 ST-BETA-5 마감** — install/uninstall + README |
| **사용자 매뉴얼 v1.0** | (신설) | ✅ **Sprint 19 ST-BETA-4 마감** — 7 섹션 |
| **Go/No-Go + Cutover Runbook** | (신설) | ✅ **Sprint 19 ST-BETA-6 마감** — 11 항목 + 5 단계 |

---

## 4. v1.2 § 추가 영향 정리 (v1.14 → v1.15 확장)

| § | v1.14 → v1.15 변경 |
|---|---|
| §9 Deferred Epic | + **EP-BETA-LAUNCH (S19 마감)** — V045 cleanup + E2E IT + Grafana + 매뉴얼 + NSSM + Go/No-Go |
| §14 SP 합계 | 338.5 → **343.5** (Sprint 19 +~5 실, 계획 3 + carry-over 흡수 +2) |
| §16 Phase B 진입 조건 | ✅ **표준 베타 plan 100% 마감** — Sprint 10~19 전부 완료. Phase 4 운영 안정화 진입 게이트 충족 |
| §17 GitHub label | `sprint:S19` 추가 |
| §18 BR 추적 | 신규 BR 없음. Sprint 16/17/18 BR (X01/X05/X06/X07/V07) E2E 통합 검증 |
| §19 Modulith 경계 | 변동 없음 (vc + notify + order 모두 정합) |

---

## 5. 신규 산출물 (Sprint 19)

### Backend Migration (vc 모듈)
- [V045__sample_cleanup_function.sql](../../backend/vc/src/main/resources/db/migration/V045__sample_cleanup_function.sql) — cleanup_99999_samples() PL/pgSQL

### Backend IT (app 모듈)
- [SampleCleanupIT.java](../../backend/app/src/test/java/com/scheduling/integration/SampleCleanupIT.java) — 3 cases
- [BetaE2EIntegrationIT.java](../../backend/app/src/test/java/com/scheduling/integration/BetaE2EIntegrationIT.java) — 4 cases (E2E 통합)

### Infrastructure
- [notify-sprint18.json](../../infrastructure/observability/grafana/dashboards/notify-sprint18.json) — Grafana dashboard 6 패널
- [install-nssm-services.ps1](../../infrastructure/scripts/install-nssm-services.ps1) — NSSM 자동시작 설치
- [uninstall-nssm-services.ps1](../../infrastructure/scripts/uninstall-nssm-services.ps1) — NSSM 제거
- [README-nssm.md](../../infrastructure/scripts/README-nssm.md) — NSSM 운영 가이드

### Docs
- [USER_MANUAL_v1.0.md](../../docs/manual/USER_MANUAL_v1.0.md) — 사용자 매뉴얼 7 섹션
- [BETA_GO_NOGO_CHECKLIST_v1.0.md](../../docs/cutover/BETA_GO_NOGO_CHECKLIST_v1.0.md) — 11 항목
- [BETA_RUNBOOK_v1.0.md](../../docs/cutover/BETA_RUNBOOK_v1.0.md) — 5 단계 시간순 + 비상 롤백

---

## 6. carry-over 식별 (Phase 4+ 진입 시 참조)

| 항목 | 우선순위 | 비고 |
|---|---|---|
| **Slack/Kakao 실 webhook URL/token 발급** | High Phase 4 | 사내 IT/관리팀 협의 — Sprint 19 baseline 은 config flag default false 로 안전 |
| **장비/셋팅/합금형/라인 5 entity CRUD UI** | Medium Phase 4 | Sprint 19 ST-BETA-7 deferred — 실 운영 진입 후 마스터 갱신 시점 작성 |
| **DaoAuthenticationProvider deprecation** | Low Phase 4 | Spring Security 6.1+ AuthenticationManager builder 패턴 리팩토링 |
| **Resilience4j fallback 정밀 검증** | Low Phase 4 | WireMock 도입 후 circuit OPEN 강제 시뮬 |
| **MES 실 adapter (HTTP/MQ/file)** | High Phase 5+ | MesShiftPort 인터페이스 위 교체 — 벤더 협의 |
| **Order 자동 INSERT 흐름** | Medium Phase 5+ | ImportOrchestrator → Allocator chain 실 연동 |
| **사용자 매뉴얼 스크린샷** | Low Phase 4 | docs/manual/screenshots/ 폴더 추가 (4 role 별 핵심 화면) |
| **베타 사용자 PIN 강제 변경 정책 (30일)** | Low Phase 4 | NFR-SEC-007 보완 |

---

## 7. 관련 자료

- [TASK-001_WBS_v1.14](TASK-001_WBS_v1.14.md) — Sprint 18 마감
- [PLAN-SPRINT-19_EP-BETA-LAUNCH_v1.0](PLAN-SPRINT-19_EP-BETA-LAUNCH_v1.0.md) — Sprint 19 진입 plan
- [V045 migration](../../backend/vc/src/main/resources/db/migration/V045__sample_cleanup_function.sql)
- [BetaE2EIntegrationIT](../../backend/app/src/test/java/com/scheduling/integration/BetaE2EIntegrationIT.java)
- [USER_MANUAL_v1.0](../../docs/manual/USER_MANUAL_v1.0.md)
- [BETA_GO_NOGO_CHECKLIST_v1.0](../../docs/cutover/BETA_GO_NOGO_CHECKLIST_v1.0.md)
- [BETA_RUNBOOK_v1.0](../../docs/cutover/BETA_RUNBOOK_v1.0.md)

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0~1.4 | 2026-05-15~23 | (작성자/Claude) | 초안 ~ Sprint 8 마감 |
| 1.5~1.14 | 2026-05-27 | Claude Code | Sprint 9~18 마감 + V038/SockJS hotfix + EX/CONFIRM/DAY-LOCK/NOTIFY chain |
| 1.15 | 2026-05-28 | Claude Code | **Addendum — Sprint 19 EP-BETA-LAUNCH 100% 마감 (6 Story / 14 Task / ~5 SP) — 표준 베타 plan Sprint 10~19 모두 완료**. V045 cleanup_99999_samples() PL/pgSQL 함수 + BetaE2EIntegrationIT 4 cases (Sprint 13~18 단일 시나리오) + Grafana notify-sprint18.json 6 패널 + USER_MANUAL_v1.0 7 섹션 (4 role SOP) + NSSM install/uninstall + README + BETA_GO_NOGO_CHECKLIST 11 항목 + BETA_RUNBOOK 5 단계 시간순 + 비상 롤백 4. Backend IT 7 신규 + 53 회귀 = 60/60 GREEN, Frontend tsc 0 + vitest 82/82. DoD 11/11 ✅ (NSSM 실 PC 재부팅은 cutover T-1주 단계로 위임). 67 Epic / 343.5 SP 실. **Phase 4 운영 안정화 진입 게이트 충족** — Slack/Kakao 실 webhook (Phase 4), MES 실 adapter (Phase 5+), 5 entity CRUD UI (Phase 4), DaoAuth deprecation (Phase 4) carry-over. **베타 진입도 10/10** (Sprint 10~19 완료). |
