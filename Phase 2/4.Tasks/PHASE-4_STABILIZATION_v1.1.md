# Phase 4 운영 안정화 계획 (Stabilization) v1.1 — Cost-Zero 정책 반영

**작성일**: 2026-05-28 | **버전**: 1.1 | **상태**: Phase 3 표준 베타 plan (Sprint 10~19) 마감 직후 — Phase 4 진입 권고안 (**무료화 통합**)

> **참조**: [v1.0 (Kakao 활성 포함)](PHASE-4_STABILIZATION_v1.0.md) — 본 v1.1 이 cost-zero 정책 반영 / [PLAN-SPRINT-20 v1.1](PLAN-SPRINT-20_EP-EXT-WEBHOOK_v1.1.md) + [WBS v1.15 §6 carry-over](TASK-001_WBS_v1.15.md) + [BETA_RUNBOOK_v1.0](../../docs/cutover/BETA_RUNBOOK_v1.0.md) + 표준 베타 plan 10/10 ✅ (Sprint 10~19 모두 마감)

---

## 0. v1.0 → v1.1 변경 요약 (Cost-Zero 통합)

| 항목 | v1.0 | v1.1 |
|---|---|---|
| **S20 Epic** | EP-EXT-WEBHOOK (4 SP, Slack + Kakao 실 webhook) | **EP-EXT-WEBHOOK + EP-COST-ZERO (3.6 SP, Slack + AG Grid Community + Kakao 보류 + Sentry 미도입)** |
| **Phase 4 합계 SP** | 26 SP | **~25.6 SP** (S20 -0.4) |
| **Pre-Phase 협의** | Slack 채널 + Kakao biz token | **Slack 채널 만** (Kakao 협의 Phase 5+ deferred) |
| **§6 KPI Kakao 도달률** | 95% 이상 활성 | **Phase 5+ deferred** (NS-04 KPI 보류) |
| **§10 Phase 5+ carry-over** | 5건 | **+3건 (Kakao 부활 / AG Grid Range Selection 재도입 / Sentry 도입 옵션) = 8건** |
| **연 운영 비용 (Phase 4 전체)** | ~$2,469 + 12만원 | **$0 / 0원** |
| **사용자 결정 근거** | Phase 4 진입 전 default 활성 | **사용자 10명 이내 운영 — 무료 대안 전환** |

---

## 1. 목적

**Phase 3 표준 베타 plan (Sprint 10~19) 100% 마감 직후 진입 — 베타 운영 1~3개월 기간의 안정화 sprint 묶음.** Phase 4 는 6 sprint (Sprint 20~25) 로 carry-over 5건 해소 + 베타 피드백 반영 + 본격 운영 (PROD) 진입 게이트. **v1.1 추가**: 사용자 10명 이내 사내 운영 기준 — 모든 유료 항목 무료 대안으로 전환.

### Phase 4 진입 조건
- ✅ Sprint 10~19 표준 베타 plan 마감
- ✅ Go/No-Go 11/11 검증 (코드 측면 8/8 + 인프라 측면 3/3 cutover T-1주 단계)
- ✅ 베타 사용자 8명 첫 1주 실 운영 (Grafana 모니터링 / Critical retry 0 / MES degraded 0)
- ⏳ 사내 IT 협의 — **Slack 채널 발급** (Kakao biz token 협의 deferred)

### Phase 4 종료 조건 (Phase 5 진입)
- ✅ Phase 4 carry-over 0건 (5 entity CRUD UI / DaoAuth / Slack 실 / PIN 정책 모두 마감)
- ✅ 본격 운영 (사용자 30명+ 확장 + 부하 검증 + cutover PROD)
- ⏳ MES 실 adapter Phase 1 baseline (HTTP polling — Phase 5+ 본격 chain)

---

## 2. Phase 4 SP·기간 (v1.1)

| Sprint | Epic | SP 계획 | 추정 PD | 비중 |
|---|---|:--:|:--:|---|
| **S20** | EP-EXT-WEBHOOK + EP-COST-ZERO — Slack 실 webhook + AG Grid Community + Kakao 보류 + Sentry 미도입 + WireMock IT | **3.6** | **2.0** | 100% (S20) |
| **S21** | EP-CRUD-MASTER-2 — 5 entity CRUD UI 완성 | 5 | 2.5 | 100% (S21) |
| **S22** | EP-SEC-HARDEN — DaoAuth deprecation + PIN 강제 변경 + audit 강화 | 4 | 2.0 | 100% (S22) |
| **S23** | EP-MES-ADAPTER-1 — MES HTTP polling adapter Phase 1 | 5 | 2.5 | 100% (S23, Phase 5 chain baseline) |
| **S24** | EP-OPS-FEEDBACK — 베타 1개월 피드백 반영 + UX 미세 조정 | 4 | 2.0 | 100% (S24) |
| **S25** | EP-PROD-LAUNCH — 본격 운영 진입 (30명 확장 + 부하 검증) | 4 | 2.0 | 100% (S25) |
| **Phase 4 합계** | | **~25.6 SP** | **~13 PD** | ~6 영업주 |

---

## 3. Sprint 별 Epic 상세

### S20 EP-EXT-WEBHOOK + EP-COST-ZERO — Slack 실 webhook + 비용 무료화 (3.6 SP)

**선행**: 사내 IT 협의 — **Slack 채널 발급 만** (Kakao biz token 협의 deferred)

| Story | 내용 | SP |
|---|---|:--:|
| ST-EXT-1 | Slack 실 webhook URL 적용 (`application-prod.yml`) + 채널 라우팅 검증 (`#scheduling-alerts` vs `#scheduling-critical`) | 1.0 |
| ~~ST-EXT-2~~ | ~~Kakao 실 biz token + webhook URL 적용~~ → **Phase 5+ carry-over** | **deferred** |
| ST-EXT-3 | WireMock 도입 — Resilience4j circuit OPEN 강제 시뮬 IT (Sprint 18 carry-over, **Slack 만**) | 0.5 |
| ST-EXT-4 | 사용자 매뉴얼 §6 장애 대응 갱신 — **Slack 발송 실패 시 대응 절차 +1건 (총 10건)** | 0.3 |
| ST-EXT-5 | rbac-matrix.md v1.5 — **Slack** 발송 actor 감사 강화 (system → audit) | 0.3 |
| **ST-COST-1** | **AG Grid Enterprise → Community 다운그레이드 (MIT)** — 4 grid 컴포넌트 + Range Selection 폴백 + watermark 제거 | **1.0** |
| **ST-COST-2** | **Kakao 보류 + Sentry 미도입 문서 정리** — application-prod.yml + CLAUDE.md + SRS v1.6 + COST-ZERO_POLICY_v1.0 신규 | **0.5** |

> **세부 task — [PLAN-SPRINT-20 v1.1](PLAN-SPRINT-20_EP-EXT-WEBHOOK_v1.1.md) 참조 (17 task, ~2 PD, 3-Day)**

### S21 EP-CRUD-MASTER-2 — 5 entity CRUD UI 완성 (5 SP, v1.0 동일)

| Story | 내용 | SP |
|---|---|:--:|
| ST-CRUD-1 | VcMachineAdminPage — LP-01~04 + IC-01 CRUD + machine_type / total_slots / day/night_rotations 수정 + 비활성 toggle | 1.5 |
| ST-CRUD-2 | SettingGroupAdminPage — setting_group 1~8 CRUD + 비활성 toggle (BR-V12·V13 정합) | 1.0 |
| ST-CRUD-3 | AlloyMoldAdminPage — alloy_mold (composite 1·2·3·6) CRUD + product 매핑 보기 | 1.0 |
| ST-CRUD-4 | LineAdminPage — line entity CRUD (현장 작업 라인) | 1.0 |
| ST-CRUD-5 | HolidayAdminPage — master.holiday CRUD (TK-06-1-1 master.holiday seed 갱신, 연도별) | 0.5 |
| ST-CRUD-6 | MasterHubPage 카드 5개 모두 활성화 + 회귀 IT | 0.5 |

### S22 EP-SEC-HARDEN — Security 강화 (4 SP, v1.0 동일)

| Story | 내용 | SP |
|---|---|:--:|
| ST-SEC-1 | DaoAuthenticationProvider deprecation 제거 — Spring Security 6.1+ `AuthenticationManager` builder pattern (SecurityFilterChain 내 httpSecurity.authenticationManager()) | 1.5 |
| ST-SEC-2 | PIN 강제 변경 정책 30일 — last_pin_change_at 컬럼 + 30일 경과 시 강제 변경 화면 redirect (NFR-SEC-007 보완) | 1.0 |
| ST-SEC-3 | audit_log NFR-SEC-004 3년 보존 cron + Partition 분리 (audit.schedule_audit_log_y2026m05 → m06 자동 생성) | 1.0 |
| ST-SEC-4 | UserAdminPage PIN 재설정 흐름 — IT_OPS 가 일괄 reset → 사용자 첫 로그인 시 강제 변경 | 0.5 |

### S23 EP-MES-ADAPTER-1 — MES HTTP polling adapter Phase 1 (5 SP, v1.0 동일)

| Story | 내용 | SP |
|---|---|:--:|
| ST-MES-1 | HttpMesShiftAdapter — `MesShiftPort` 구현체 (REST GET `/api/mes/shift?machine=&date=&shift_no=`). config flag `scheduling.mes.adapter=http` 활성 | 2.0 |
| ST-MES-2 | MES polling scheduler — `@Scheduled(fixedDelay=60s)` MES GET → `mesPort.reportProduction()` 호출 | 1.0 |
| ST-MES-3 | WireMock IT — MES 정상 / 5xx 실패 / timeout / 부분 응답 4 시나리오 | 1.0 |
| ST-MES-4 | DegradedModeService 통합 검증 — MES 실 polling 정상 시 degraded 해제 / 6h 미수신 시 진입 | 0.5 |
| ST-MES-5 | 사용자 매뉴얼 §3.5 IT_OPS MES adapter 설정 가이드 (config + 트러블슈팅) | 0.5 |

### S24 EP-OPS-FEEDBACK — 베타 1개월 피드백 (4 SP, v1.0 동일)

| Story | 내용 | SP |
|---|---|:--:|
| ST-FB-1 | 매뉴얼 스크린샷 추가 — `docs/manual/screenshots/` 4 role × 5+ 핵심 화면 (실 운영 후) | 1.0 |
| ST-FB-2 | UX 미세 조정 — Drawer 위치 / Batch 확정 selectAll UI / DegradedBanner 시각 + **AG Grid Community Range Selection 폴백 사용자 피드백** 반영 + **Sprint 21 vitest 7 skip 정비 (AntD Portal/Calendar/Popconfirm jsdom 호환 패턴 일괄 정립)** | 1.5 |
| ST-FB-3 | Grafana alert rule 정교화 — 1개월 운영 데이터 기반 threshold 재조정 (Critical retry / HTTP p95) — **Kakao 도달률 deferred** | 1.0 |
| ST-FB-4 | 베타 보고서 v1.0 — 1개월 KPI 측정 결과 + Sprint 25 PROD 진입 권고 + **Kakao 부활 필요성 판단** | 0.5 |

### S25 EP-PROD-LAUNCH — 본격 운영 진입 (4 SP, v1.0 동일)

| Story | 내용 | SP |
|---|---|:--:|
| ST-PROD-1 | 사용자 확장 — 30명+ Keycloak 신규 추가 + role 분배 (PLANNER 10 + STK 15 + IT_OPS 3 + READ_ONLY 5) | 1.0 |
| ST-PROD-2 | k6 부하 검증 — 1500 row × 30 col + 30 동시 사용자 + p95 < 800ms 실 환경 측정 | 1.5 |
| ST-PROD-3 | Blue/Green 첫 무중단 deploy — STG → PROD switch script 검증 (`infrastructure/scripts/blue-green-switch.sh`) | 1.0 |
| ST-PROD-4 | PROD 운영 시작 공지 + 베타 마감 — 1개월 베타 → 본격 운영 전환 | 0.5 |

---

## 4. 의존성 DAG (v1.1)

```
사내 IT 협의 (Slack 채널 만) ─→ S20 EP-EXT-WEBHOOK + EP-COST-ZERO
                                              ↓
S21 EP-CRUD-MASTER-2 ←─────────────────────────┤
                                              ↓
S22 EP-SEC-HARDEN ─────────────────────────────┤
                                              ↓
S23 EP-MES-ADAPTER-1 (← 실 MES 협의 진행 후)    │
                                              ↓
[베타 운영 1개월 데이터 누적] ──→ S24 EP-OPS-FEEDBACK (Kakao 부활 필요성 판단)
                                              ↓
                              S25 EP-PROD-LAUNCH (30명+ 확장)
                                              ↓
                              Phase 5+ (본격 운영 + 본격 MES chain + Kakao 부활 옵션)
```

**병렬 윈도우:**
- **S20 ↔ S21** — Slack/AG Grid vs CRUD UI 독립 (sprint 1주 병행 가능)
- **S22 ↔ S23** — Security vs MES adapter 독립
- **S24 → S25** — 순차 (피드백 → 본격 진입)

---

## 5. Phase 4 carry-over 매핑 (v1.1)

| Sprint 19 §6 carry-over | Phase 4 처리 |
|---|---|
| Slack 실 webhook URL (High) | ✅ S20 ST-EXT-1 (Kakao 분리 — Phase 5+ deferred) |
| **Kakao 실 webhook + biz token (High)** | ⏸ **Phase 5+ carry-over** — 사용자 10명 이내 / Slack 모바일 push 로 충분 / 월 ~1만원 비용 회피 |
| 5 entity CRUD UI (Medium) | ✅ S21 EP-CRUD-MASTER-2 |
| DaoAuthenticationProvider deprecation (Low) | ✅ S22 EP-SEC-HARDEN |
| Resilience4j fallback 정밀 검증 (Low) | ✅ S20 ST-EXT-3 (WireMock 도입 후, **Slack 만**) |
| 매뉴얼 스크린샷 (Low) | ✅ S24 ST-FB-1 |
| PIN 강제 변경 정책 30일 (Low) | ✅ S22 ST-SEC-2 |
| **MES 실 adapter (HTTP/MQ/file) (High Phase 5+)** | ✅ S23 EP-MES-ADAPTER-1 (HTTP Phase 1 baseline, MQ/file 은 Phase 5+) |
| **Order 자동 INSERT 흐름 (Medium Phase 5+)** | ⏳ Phase 5+ (변동 없음 — ImportOrchestrator → Allocator chain) |
| **🆕 AG Grid Enterprise license 결정 (High Phase 4)** | ✅ **S20 ST-COST-1** — Community 다운그레이드 (MIT, $0, watermark 제거) |
| **🆕 Sentry SaaS 도입 결정 (Medium Phase 4)** | ✅ **S20 ST-COST-2** — 미도입 공식화 (Loki + Prometheus 로 충분) |

---

## 6. Phase 4 KPI (베타 → PROD 전환 게이트) v1.1

| 항목 | 목표 |
|---|---|
| 베타 사용자 1주 만족도 | 4.0/5.0 이상 (인터뷰 기반) |
| Critical Diff retry rate (Grafana) | < 0.05 / 5min |
| ~~Kakao 도달률~~ | **Phase 5+ deferred (NS-04 KPI 보류)** |
| **Slack delivery_attempt FAILED 비율 (신규 대체 KPI)** | **< 5%** |
| MES degraded duration | < 30min / day (Phase 5+ 본격 chain 후) |
| HTTP p95 latency (모든 endpoint) | < 800ms |
| Backend IT 회귀 | 303+/303+ GREEN |
| PROD 부하 검증 (k6) | 30 사용자 × 5분 GREEN |
| 사용자 매뉴얼 v1.x | 스크린샷 포함 갱신 |
| **🆕 운영 비용 (월) (신규 KPI)** | **0원** (Cost-Zero 정책) |
| **🆕 AG Grid watermark/license warning** | **0건** (Community 다운그레이드 후) |

---

## 7. Phase 4 산출물 (Deliverables 예상) v1.1

| 분류 | 파일 |
|---|---|
| Backend Migration | V047~V050+ (audit partition, PIN last_change, MES adapter config) |
| Backend Service | HttpMesShiftAdapter, AuthenticationManagerConfig (Spring Security 6.1+), PinExpiryService |
| Backend IT | WireMockSlackIT, WireMockMesIT, PinExpiryIT (~~WireMockKakaoIT 삭제~~) |
| Frontend | VcMachineAdminPage, SettingGroupAdminPage, AlloyMoldAdminPage, LineAdminPage, HolidayAdminPage, PinForceChangeModal |
| **Frontend (🆕 Cost-Zero)** | **agGridSetup.ts (Community 전환), package.json (ag-grid-enterprise 제거), 4 grid 컴포넌트 회귀** |
| Infra | application-prod.yml (Slack 실 URL, Kakao false 영구), blue-green-switch.sh 검증 |
| Docs | USER_MANUAL_v1.1 + screenshots/, BETA_REPORT_v1.0, PROD_LAUNCH_CHECKLIST_v1.0 |
| **Docs (🆕 Cost-Zero)** | **COST-ZERO_POLICY_v1.0** (신규), **SRS v1.6** (Kakao Phase 5+ + NS-04 deferred), **CLAUDE.md §2 갱신** (Sentry 정리) |
| WBS | TASK-001_WBS_v1.16 ~ v1.21 (sprint 별 Addendum) |

---

## 8. Phase 4 리스크 + 회피 (v1.1)

| 리스크 | 영향 | 회피 |
|---|---|---|
| 사내 IT Slack 발급 지연 | S20 진입 부분 지연 | Phase 4 진입 전 1주 협의 시작 + 발급 안 되면 WireMock IT 만 (실 발송은 Sprint 21+ carry-over) |
| 베타 사용자 피드백 부족 (8명 → 분기 어려움) | S24 데이터 부족 | 1개월 KPI + 정성 인터뷰 (각 role 2명+) 조합 |
| 5 entity CRUD UI scope 누적 | S21 5 SP 초과 | scope 우선순위 (vc_machine > setting_group > alloy_mold > line > holiday) — 시간 마감 시 holiday Phase 5+ defer |
| MES 실 adapter 미확정 | S23 진행 불가 | HTTP Phase 1 baseline 만 (MQ/file 은 벤더 협의 후 Phase 5+) |
| Spring Security 6.1+ 회귀 | S22 IT 다수 실패 가능 | 회귀 IT 사전 실행 + 한 sprint 안에 backout 가능하게 commit 분할 |
| PROD 30명 확장 후 부하 | S25 p95 SLA 위반 | k6 사전 검증 + 부분 deploy (10명 → 20명 → 30명 단계적) |
| **🆕 AG Grid Community Range Selection 손실 → PLANNER UX 저하** | **S20 후 사용자 호소** | (1) 다중행 selection 폴백, (2) S24 ST-FB-2 에서 피드백 반영 (TanStack Table custom 또는 license 구매 carry-over), (3) COST-ZERO_POLICY 의 1년 뒤 부활 옵션 보장 |
| **🆕 Kakao 보류 후 PLANNER 도달 실패 호소** | **S24 베타 1개월 인터뷰 시 재도입 요청** | Phase 5+ carry-over 명시 + ~0.5 PD 부활 비용 보장 (config flag + biz token 만). S24 ST-FB-4 에서 정식 판단 |
| **🆕 Sentry 미도입 후 error tracking 부담 증가** | **운영팀 트러블슈팅 비용 ↑** | Loki + Promtail label (traceId / userId / brId) 기반 검색 1개월 운영 후 재판단. 부담 시 self-hosted Sentry Phase 5+ 옵션 |

---

## 9. Phase 4 작업 순서 추천 (v1.1)

**Pre-Phase (Phase 4 진입 직전 1주):**
- 사내 IT **Slack 채널 만** 발급 협의 (Kakao biz token 협의 deferred)
- 베타 운영 1주 실 데이터 누적 (Grafana KPI 측정 시작)

**Pre-Phase 추가 (Sprint 19 carry-over 통합):**
- **Jenkins CI pipeline 활성** (Sprint 0 EP-32 baseline 위) — push trigger 시 자동
  `./gradlew verifyAll` (Sprint 19 hotfix `0a540e7` 의 신규 task) 실행 + SonarQube + Trivy.
  목적: Sprint 16~19 같은 검증 범위 누락 (`:app:test --tests "...integration.*IT"` 부분 패턴)
  방지. `infrastructure/jenkins/` 활성 여부 확인 후 push webhook 등록.
- **S20 첫 작업 권고** — Jenkins pipeline GREEN 확인 후 본격 Slack webhook + AG Grid Community 진행.

**Phase 4 Sprint 순서:**
- **S20 (Week 1)** — EP-EXT-WEBHOOK + EP-COST-ZERO + S21 병행 시작
- **S21 (Week 2)** — EP-CRUD-MASTER-2 완성
- **S22 (Week 3)** — EP-SEC-HARDEN
- **S23 (Week 4)** — EP-MES-ADAPTER-1
- **S24 (Week 5)** — EP-OPS-FEEDBACK (1개월 데이터 + 인터뷰 + Kakao 부활 필요성 판단)
- **S25 (Week 6)** — EP-PROD-LAUNCH

**총 ~6 영업주 (1인 AI 가속).**

---

## 10. Phase 5+ 예상 carry-over (Phase 4 종료 후) v1.1

| 항목 | 우선순위 | 비고 |
|---|---|---|
| MES 실 adapter MQ/file 분기 | High Phase 5 | Phase 4 S23 HTTP polling 위에 MQ/file adapter 추가 |
| Order 자동 INSERT chain (ImportOrchestrator → Allocator) | Medium Phase 5 | Sprint 17 Allocator.requestedBy 정합 위 chain 활성 |
| 30+ 사용자 확장 부하 검증 결과에 따른 인덱스/쿼리 튜닝 | Medium Phase 5 | k6 결과 기반 |
| ML/AI 기반 priority 알고리즘 (PRODUCT_PRIORITY 자동 갱신) | Low Phase 5+ | 실 운영 6개월 데이터 누적 후 |
| Multi-tenant 확장 (다른 공장) | Low Phase 6+ | 사내 multi-plant 결정 시 |
| **🆕 Kakao 알림톡 부활** | **Medium Phase 5+ (S24 결과에 따라)** | **사용자 10명 → 30명+ 확장 시 또는 Slack 모바일 push 불충분 시. ~0.5 PD (config flag true + biz token + KAKAO_WEBHOOK_URL NSSM env). 코드는 stub 유지** |
| **🆕 AG Grid Range Selection 재도입** | **Low Phase 5+** | **사용자 피드백 (S24 ST-FB-2) 에서 다중행 선택 부족 호소 시. 옵션: (1) Single Dev License $999/년, (2) TanStack Table 자체 구현 ~3 PD** |
| **🆕 Sentry SaaS 도입 옵션** | **Low Phase 5+** | **Loki 검색 부담 증가 시점. Self-hosted Sentry (무료, 운영 부담 ↑) 또는 Sentry SaaS Team $26/월** |

---

## 11. 무료화 정책 (v1.1 추가 — COST-ZERO_POLICY_v1.0 본문 참조)

**판단 기준**: 사용자 10명 이내 / 사내 한정 운영 / 외부 노출 없음 / 베타 단계 ~ 1년 PROD 운영.

| 항목 | 유료 시 | 무료 대안 (적용) | 부활 비용 (1년 뒤) |
|---|---|---|---|
| AG Grid Enterprise | $999~$1,599/dev/년 | **Community (MIT)** | $999/년 또는 TanStack 3 PD |
| Kakao 알림톡 | 월 ~1만원 / 연 12만원 | **보류 (Phase 5+)** | ~0.5 PD (config flag + biz token) |
| Slack Pro | $7.25/user/월 / 연 ~$870 @ 10명 | **Free plan** | 사용자 10+ 명 / 통합 11+ 개 시 $7.25/user/월 |
| Sentry SaaS | Free tier ~ Team $26/월 | **미도입 (Loki only)** | Free tier 또는 self-hosted Sentry (운영 부담) |
| Docker Desktop | $5/user/월 @ 250+ 회사 | **Docker Engine + WSL2** | (조건부 — 회사 직원 수에 따라) |
| **연간 총 비용** | **~$2,469 + 12만원** | **$0 / 0원** | 부분 부활 시 ~$1,000~$1,500/년 가능 |

---

## 12. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-28 | Claude Code | 초안 — Phase 4 운영 안정화 6 sprint (S20~S25) / 26 SP / ~13 PD / ~6 영업주 분해. Sprint 19 §6 carry-over 8건 매핑 (High 4 + Medium 1 + Low 3) + Phase 5+ 5건 식별. EP-EXT-WEBHOOK + EP-CRUD-MASTER-2 + EP-SEC-HARDEN + EP-MES-ADAPTER-1 + EP-OPS-FEEDBACK + EP-PROD-LAUNCH. KPI 8 + 리스크 6 + Pre-Phase 사내 IT 협의 1주. 표준 베타 plan (Sprint 10~19) 100% 마감 직후 진입 권고. |
| **1.1** | **2026-05-28** | **Claude Code** | **Cost-Zero 정책 통합 — 사용자 10명 이내 운영 전제. (1) S20 Epic 변경 EP-EXT-WEBHOOK → EP-EXT-WEBHOOK + EP-COST-ZERO (SP 4 → 3.6, Story 5 → 7, Phase 4 합계 26 → 25.6 SP). (2) Kakao 실 webhook → Phase 5+ carry-over 분리. (3) AG Grid Enterprise → Community 다운그레이드 (S20 ST-COST-1). (4) Sentry 미도입 공식화 (S20 ST-COST-2). (5) §5 carry-over +2건 (AG Grid + Sentry). (6) §6 KPI Kakao 도달률 → Slack delivery FAILED 비율 대체 + 운영 비용 0원 KPI 신규. (7) §8 리스크 +3건 (AG Grid Community UX + Kakao 보류 호소 + Sentry 미도입 트러블슈팅). (8) §10 Phase 5+ carry-over +3건. (9) §11 무료화 정책 신규. 연 운영 비용 ~$2,469 + 12만원 → $0.** |
