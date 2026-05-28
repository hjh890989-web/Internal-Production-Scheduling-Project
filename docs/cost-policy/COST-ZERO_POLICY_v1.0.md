# Cost-Zero 정책 v1.0 — 무료 운영 영구 문서

**문서 ID**: COST-POLICY-001 | **버전**: 1.0 | **작성일**: 2026-05-29
**전판**: 없음 (신규)
**작성**: Sprint 20 ST-COST-2 산출물 (Phase 4 Cost-Zero 정책 통합)

> **본 문서는 사용자 10명 이내 사내 한정 운영 기준으로 본 시스템의 연 운영 비용을 0원으로 유지하기 위한 정책 영구 문서.** 1년 뒤 사용자 확장 / 비즈니스 요건 변경 시 부활 비용 명시. Phase 5+ 재판단 기준 포함.

---

## 1. 정책 배경

### 결정 시점
- **2026-05-29 (Sprint 20 Day 1)** — Phase 4 운영 안정화 첫 sprint 진입 직전
- **사용자 의사결정**: "비용 발생이 되는 모든 부분을 무료화 할 수 있는 대안을 마련해 줘. 이 프로젝트를 사용하는 인원은 10명을 넘지 않음"

### 적용 기준
- ✅ 사용자 10명 이내 사내 한정 운영
- ✅ 외부 노출 없음 (사내 IdP + 사내 PKI + 사내 DNS)
- ✅ 베타 단계 ~ 1년 PROD 운영
- ⚠ 30명+ 확장 시 일부 항목 (Slack Pro) 재판단 필요
- ⚠ 250+ 직원 회사 시 Docker Desktop 라이센스 별도 확인

---

## 2. 유료 항목 전수 + 무료 대안 (5종)

### 2.1 AG Grid Enterprise → **Community 다운그레이드 (적용)**

| 항목 | 내용 |
|---|---|
| 유료 비용 | **$999~$1,599/dev/년** (Single Dev License) 또는 $2,999/년 (Deploy License) |
| 코드 의존성 | `frontend/package.json` `ag-grid-enterprise@35.3.0` + `agGridSetup.ts` LicenseManager + 4 grid 컴포넌트 |
| Sprint 적용 | **Sprint 20 ST-COST-1** (2026-05-29 commit) |
| 적용 내용 | (1) `ag-grid-enterprise` → `ag-grid-community` (MIT), (2) `LicenseManager.setLicenseKey()` 제거, (3) `AllEnterpriseModule` → `ClientSideRowModelModule`, (4) `statusBar` prop 제거 (2 컴포넌트), (5) `initAgGridEnterprise()` → `initAgGrid()` rename |
| 기능 손실 | `statusBar` (총 row 수 + 집계) — Ant Design Statistic 으로 화면 외부 표시 가능 |
| 영향 미사용 (Community 동일) | 정렬, 필터 (agTextColumnFilter / agDateColumnFilter / agNumberColumnFilter), 페이지네이션, virtual scrolling, rowSelection (multiple 포함), animation, custom cellRenderer |
| 부활 비용 (1년 뒤) | **옵션 A** Single Dev License $999/년 (LicenseManager + Enterprise import 복원, statusBar prop 복원) / **옵션 B** TanStack Table v8 자체 구현 ~3 PD |
| 부활 트리거 | (1) PLANNER 가 Range Selection / Master-Detail 강하게 요구, (2) Excel-like fill handle 필요, (3) ServerSideRowModel 필요 (1500 row → 5000 row 확장 시) |

### 2.2 Kakao 알림톡 → **Phase 5+ deferred (적용)**

| 항목 | 내용 |
|---|---|
| 유료 비용 | **월 ~1만원 / 연 12만원** (알림톡 건당 6.5~9원 × 평균 750건/월). 또는 Kakao Workplace Bot 인당 월 ~5,000~8,000원 (사용자 10명 → 월 ~5~8만원) |
| 코드 의존성 | `backend/notify/com/scheduling/notify/KakaoTalkClient.java` + `kakao_delivery_log` 테이블 (Flyway V0xx) — **stub 유지** |
| Sprint 적용 | **Sprint 20 ST-COST-2** (2026-05-29 commit) |
| 적용 내용 | (1) `application.yml` `scheduling.notification.kakao.enabled` 영구 false + 주석 명시, (2) SRS v1.7 REQ-NF-USA-003 Phase 5+ deferred (Should 폐기 아님), (3) NS-04 KPI Kakao 도달률 deferred + 대체 KPI Slack delivery FAILED 비율 < 5% 신규 |
| Slack 대체 도달 | Slack 모바일 app push (workspace 가입 + 모바일 설치 필요) — 사용자 10명 이내라 PLANNER 4명 가입 충분 |
| 부활 비용 (1년 뒤) | **~0.5 PD** — `KAKAO_ENABLED=true` + `KAKAO_WEBHOOK_URL` + `KAKAO_BOT_TOKEN` NSSM env 추가 (코드 변경 0). Pre-Phase 사내 관리팀 협의 (Kakao Workplace Bot 계약 + biz token 발급) |
| 부활 트리거 | (1) 사용자 30명+ 확장 시, (2) Slack delivery_attempt FAILED 비율 ≥ 5%, (3) PLANNER 1차 응답 시간 > 5분, (4) Phase 5+ 베타 1개월 인터뷰 결과 도달 부족 호소 |

### 2.3 Slack Pro → **Free plan (적용)**

| 항목 | 내용 |
|---|---|
| 유료 비용 | **$7.25/user/월** (Pro plan) → 사용자 10명 = **연 ~$870** |
| Sprint 적용 | Sprint 20 ST-EXT-1 진입 시 적용 (사내 Slack workspace 발급 후) |
| 적용 내용 | 사내 Slack workspace Free plan 사용 — 메시지 90일 보관, 통합 10개, 1:1 video. 본 시스템 통합 1개 + 채널 2개 (#scheduling-alerts + #scheduling-critical) 만 필요 → **Free plan 한참 미달** |
| 부활 비용 (1년 뒤) | 사용자 10+ 명 또는 통합 11+ 개 시 $7.25/user/월. 또는 메시지 보관 90일 초과 필요 시 |
| 부활 트리거 | (1) 사용자 30명+ 확장, (2) 메시지 검색 필요 (90일 초과), (3) 통합 11개+ 필요 |

### 2.4 Sentry SaaS → **미도입 공식화 (적용)**

| 항목 | 내용 |
|---|---|
| 유료 비용 | Free tier 5k events/월 → Team **$26/월** 이후 |
| 코드 의존성 | **0건** 확인 — `build.gradle.kts` + `package.json` 에 sentry 의존성 미등록. 문서/계획만 명시 (CLAUDE.md + Phase 4 plan v1.0) |
| Sprint 적용 | **Sprint 20 ST-COST-2** (2026-05-29 commit) |
| 적용 내용 | CLAUDE.md §Infra 의 "APM — OpenTelemetry + Sentry" 라인을 "OpenTelemetry (Sentry 미도입 — Cost-Zero 정책, Loki + Promtail label 기반 검색)" 으로 변경 |
| 대체 관측 | (1) Prometheus + Spring Actuator + Micrometer (metrics), (2) Loki + Promtail label `traceId` / `userId` / `brId` (logs + 검색), (3) Grafana (시각화), (4) OpenTelemetry (trace) — Phase 4 Sprint 24 통합 검증 |
| 부활 비용 (1년 뒤) | **옵션 A** Sentry SaaS Free tier (월 5k events 이하 무료) — 코드 의존성 추가 ~0.3 PD / **옵션 B** Self-hosted Sentry (무료, 운영 부담 ↑) — 인프라 추가 ~1 PD |
| 부활 트리거 | (1) Loki 검색으로 트러블슈팅 부담 증가 (3건/주 이상), (2) OpenTelemetry trace 시각화 부족, (3) 운영팀 명시적 요구 |

### 2.5 Docker Desktop → **Docker Engine + WSL2 (조건부)**

| 항목 | 내용 |
|---|---|
| 유료 비용 | **$5/user/월** (250+ 직원 회사 의무) — 250 미만 회사 = 무료 |
| 회사 직원 수 확인 | 송우산업 직원 수 250 미만 여부 확인 필요. 미확인 시 안전한 옵션은 Docker Engine + WSL2 (무료 영구) |
| 대안 | Docker Engine + WSL2 (Windows 11) — 모두 OSS 무료. CLI 만 사용 (Docker Desktop GUI 미사용) |
| 적용 (현재) | 사용자 본 PC — Docker Desktop 사용 중 (250 미만 가정). 추후 250+ 확인 시 Engine 전환 (~0.5 PD) |
| 부활 비용 | 회사 직원 수 증가 시 단계적 라이센스 또는 Engine 전환 |

---

## 3. 항목별 부활 매트릭스 (1년 뒤 시점)

| 항목 | 부활 비용 | 결정 우선순위 | 부활 트리거 |
|---|---|---|---|
| Kakao 알림톡 | ~0.5 PD | **High** (PLANNER 도달 부족 시) | 30명+ 확장 / Slack FAILED ≥ 5% |
| AG Grid Range Selection | $999/년 or 3 PD | Medium | PLANNER 다중행 선택 요구 / Excel-like fill |
| Sentry SaaS | 0.3~1 PD | Low | Loki 검색 부담 / OpenTelemetry trace 부족 |
| Slack Pro | $870/년 | Low | 30명+ 확장 / 메시지 90일 초과 |
| Docker Desktop | $600/년 (조건부) | Low | 회사 250+ 확인 시 |

**합계 (최대 부활 시)** — 연 ~$2,469 + 12만원 (= v1.0 무료화 직전 비용 동일).

---

## 4. KPI 측정 + 부활 판단 시점

### Phase 4 (베타 1개월 운영) 측정 KPI
| KPI | 목표 | 부활 트리거 |
|---|---|---|
| Slack delivery_attempt FAILED 비율 | < 5% | ≥ 5% → Kakao 부활 검토 |
| PLANNER 1차 응답 시간 (Critical alert) | < 5분 | > 5분 → Kakao 부활 검토 |
| 운영팀 Loki 검색 빈도 | < 3건/주 | ≥ 3건/주 → Sentry 부활 검토 |
| AG Grid 사용자 불만족 신고 | 0건 | ≥ 1건 (다중행 선택 / Excel fill) → Range Selection 부활 검토 |
| 사용자 수 | 10명 이내 | 11명+ → 단계적 부활 검토 |
| 운영 비용 | 0원 | (변동 시 즉시 알림) |

### 정식 재판단 시점
- **Phase 4 Sprint 24 (EP-OPS-FEEDBACK)** — 베타 1개월 인터뷰 결과 + KPI 측정
- **Phase 4 Sprint 25 (EP-PROD-LAUNCH)** — PROD 30명+ 확장 시점
- **Phase 5+** — 본격 운영 6개월 후 정식 재판단

---

## 5. Sprint 20 ST-COST-1·2 산출물

### ST-COST-1 (AG Grid Community)
| 파일 | 변경 |
|---|---|
| `frontend/src/grid/agGridSetup.ts` | Community 모듈 + LicenseManager 제거 |
| `frontend/package.json` | ag-grid-enterprise 제거 + ag-grid-community 유지 |
| `frontend/src/features/vc-scheduling/components/VcRotationGrid.tsx` | import 갱신 + statusBar 제거 |
| `frontend/src/features/ex-scheduling/components/ExMatrixGrid.tsx` | import 갱신 + statusBar 제거 |

### ST-COST-2 (Kakao 보류 + Sentry 미도입 + 문서)
| 파일 | 변경 |
|---|---|
| `backend/app/src/main/resources/application.yml` | kakao.enabled false 영구 + 주석 |
| `CLAUDE.md` | §2 Infra APM 라인 Sentry 정리 |
| `Phase 2/2.SRS/SRS-001_Production_Scheduling_System_v1.7.md` | 신규 Addendum — REQ-NF-USA-003 Phase 5+ + NS-04 deferred + Cost-Zero §4 |
| `docs/cost-policy/COST-ZERO_POLICY_v1.0.md` | 본 문서 신규 |

---

## 6. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-29 | Claude Code | 초안 — Sprint 20 ST-COST-2 산출물. 5 유료 항목 (AG Grid Enterprise / Kakao / Slack Pro / Sentry / Docker Desktop) 무료 대안 + 부활 비용 + 부활 트리거 + Phase 4 KPI. 연 ~$2,469 + 12만원 → $0 무료화. 사용자 10명 이내 사내 한정 운영 전제. Phase 4 Sprint 24·25 정식 재판단. |
