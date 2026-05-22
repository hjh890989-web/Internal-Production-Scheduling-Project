# Sprint 6 진입 계획 (E2E + NFR + 베타 운영)

**Sprint**: S6 | **목표 기간**: 2026-05-23 ~ (2주, AI 가속 시 ~1~2일) | **상태**: 🔄 진입 게이트
**작성**: 2026-05-22 | **상위 참조**: [Sprint-5_Completion_v1.0.md](Sprint-5_Completion_v1.0.md) §10·11, [WBS v1.2 §8~9](../../Phase%202/4.Tasks/TASK-001_WBS_v1.2.md)

> Sprint 5 (UI 통합 7 Epic 14 commit) 종료 직후 진입. **Phase 2 end-game** — E2E 통합 +
> NFR 본격 검증 (EP-40~47) + 베타 운영 환경 + Resilience4j + 파티셔닝.

---

## 1. Sprint 6 목표 (PDD-MASTER v1.7 + SRS v1.5 NFR 본격)

- **EP-E2E E2E 시뮬레이션 + 베타 준비** (5 SP, Sprint 5 carry → Sprint 6 정식)
- **EP-40 성능 NFR** — k6 부하 1500 row × 30 col p95 < 500ms (UI) + 800ms (API)
- **EP-41 신뢰성 NFR** — Resilience4j Retry + CircuitBreaker (Kakao + Import) + Modulith event 영속
- **EP-42 보안 NFR** — Keycloak OIDC 통합 + JWT 검증 + audit 무결성 회귀
- **EP-43 사용성 NFR** — i18n + 키보드 접근성 + WCAG 2.1 AA
- **EP-44 운영 NFR** — Prometheus + Grafana 대시 + Loki 로그 + Sentry APM
- **EP-45 호환성 NFR** — Excel 양식 호환 + Chrome/Edge cross-browser
- **EP-46 비용 NFR** — Vite bundle 최적화 + jacoco coverage 80%↑
- **EP-47 사업 KPI** — NS-01~S09 (납기·생산성·도달률) 영속 + 대시 (사업 ROI)
- **인프라** — audit 월별 파티셔닝 + Redis Pub/Sub fallback + 베타 STG 배포

---

## 2. Sprint 6 Epic·SP 매트릭스

| Epic | 제목 | SP | 의존 (선행) | 핵심 산출 |
|---|---|:--:|---|---|
| **EP-E2E** ⭐⭐ | E2E 시뮬레이션 + 베타 준비 (Sprint 5 carry) | 5 | EP-15·17·EX13·EX14 ✓ | Playwright E2E + 베타 운영 시나리오 |
| **EP-40** ⭐ | 성능 NFR — k6 부하 + UI p95 | 5 | Sprint 5 UI ✓ | k6 1500 row × 30 col p95 측정 + Vite chunk 정책 |
| **EP-41** ⭐ | 신뢰성 NFR — Resilience4j + 영속 event | 5 | EP-16 ✓ EP-EX14 ✓ | @Retry / @CircuitBreaker / spring-modulith-events-jpa |
| **EP-42** ⭐ | 보안 NFR — Keycloak OIDC 통합 | 4 | EP-30 baseline ✓ | OIDC SAML/JWT + audit 무결성 회귀 |
| **EP-43** | 사용성 NFR — i18n + WCAG 2.1 AA | 3 | Sprint 5 UI ✓ | 키보드 nav + aria + i18n EN |
| **EP-44** ⭐ | 운영 NFR — Prometheus/Grafana/Loki | 4 | Sprint 0 Actuator ✓ | scrape + 대시 + alert + Loki promtail |
| **EP-45** | 호환성 NFR — Excel + cross-browser | 3 | EP-12 ✓ | POI XSSF 셀-수준 회귀 + Playwright Chromium/Firefox |
| **EP-46** | 비용 NFR — Vite bundle + jacoco | 2 | Sprint 5 build ✓ | manualChunks ant-design lazy + coverage 80%↑ |
| **EP-47** ⭐ | 사업 KPI 측정 인프라 | 4 | EP-40·44 | NS-01~S09 영속 + Grafana dashboard |
| **인프라** | audit 파티셔닝 + Redis Pub/Sub fallback + STG 베타 | 3 | EP-11 ✓ EP-EX14 ✓ | 월별 partition + Redis cluster + Docker STG |

**합계**: **~38 SP** (Sprint 6 capacity 50 SP velocity 기준 · ~76% 활용 — NFR 본격 검증 큰 작업).
EP-E2E + EP-40 + EP-41 + EP-44 가 critical path (19 SP).

---

## 3. 의존성 그래프

```
Sprint 5 (UI 통합)
       │
       ├──► EP-E2E (Playwright 시나리오) ⭐⭐
       │      │
       │      └──► EP-40 (k6 부하 + UI p95) ⭐
       │
       ├──► EP-41 (Resilience4j + 영속 event) ⭐
       │      │
       │      └──► 인프라 (Redis Pub/Sub fallback)
       │
       ├──► EP-42 (Keycloak OIDC) ⭐
       │
       ├──► EP-44 (관측성 — Prometheus/Loki/Grafana) ⭐
       │      │
       │      └──► EP-47 (사업 KPI 대시) ⭐
       │
       ├──► EP-43 (i18n + WCAG) — 독립
       ├──► EP-45 (Excel + cross-browser) — 독립
       └──► EP-46 (bundle + coverage) — 독립
```

Critical Path: **EP-E2E → EP-40 → EP-44 → EP-47** (~19 SP, ~13 PD).

---

## 4. 권장 진행 순서 (AI 가속 vibe coding)

| 단계 | Epic·Story | 비고 |
|---|---|---|
| **Phase A** (Day 1) | EP-E2E Playwright 시나리오 (vc 시뮬뷰 → swap → 매트릭스 cascade) + Chromium 1 브라우저 | E2E 인프라 |
| **Phase A** (Day 1) | EP-40 k6 부하 (1500 row 매트릭스 + 100 동시 사용자) + UI Lighthouse p95 측정 | 성능 measurement |
| **Phase B** (Day 1~2) | EP-41 Resilience4j @Retry + @CircuitBreaker (Kakao + InternalImport) + spring-modulith-events-jpa 영속 | 신뢰성 |
| **Phase B** (Day 1~2) | 인프라 — audit 월별 파티셔닝 V030 + Redis Pub/Sub fallback notify 모듈 | 운영 |
| **Phase C** (Day 2) | EP-44 Actuator scrape + Prometheus + Grafana 대시 4종 (스케줄 / 알림 / DB / JVM) | 관측성 |
| **Phase C** (Day 2) | EP-47 사업 KPI (NS-01~S09) 영속 테이블 + Grafana 대시 통합 | KPI |
| **Phase D** (Day 2~3) | EP-42 Keycloak OIDC + EP-43 i18n + EP-45 + EP-46 (NFR 묶음) | NFR 보조 |
| **Phase E** (Day 3) | 베타 운영 환경 STG 셋업 (Docker Compose + Blue/Green) + 베타 운영 시나리오 작성 | 베타 진입 |
| **Phase F** (Day 3) | Sprint 6 회고 + Phase 3 완료 보고 (Sprint 0~6 9 Sprint 종합) | 마감 |

**병렬 옵션** (의존성 그래프 기반):
- **A. EP-E2E + EP-40 병렬** — E2E 시나리오 + k6 부하 (다른 영역, 같은 UI 사용)
- **B. EP-41 + 인프라 병렬** — Resilience4j + audit 파티셔닝 + Redis (다른 모듈)
- **C. EP-44 + EP-47 병렬** — Prometheus scrape + KPI 영속 (관측성 묶음)
- **D. EP-42 + EP-43 + EP-45 + EP-46 묶음** — NFR 보조 4 Epic 한 턴

---

## 5. 신규 인프라 (Sprint 6)

### 마이그레이션 (예상 V030~V033)

| Migration | 테이블 / VIEW | Epic·Task |
|---|---|---|
| V030 | `audit.schedule_audit_log` 월별 파티셔닝 (LIST + 2026-01~2028-12 36 partitions) | 인프라 |
| V031 | `business_kpi.measurement` (NS-01~S09 시계열, 일별 집계) | EP-47 ST-47-1 |
| V032 | `app.import_attempt_log` (Resilience4j Retry 영속 트래킹) | EP-41 ST-41-1 |
| V033 | spring-modulith-events-jpa schema (`event_publication`) | EP-41 ST-41-2 |

### 신규 모듈·인프라

```
backend/
  resilience/   (신규 — common 모듈 확장)
                + Resilience4jConfig (@CircuitBreaker + @Retry 정의)
  notify/
    + RedisStompRelayConfig (다중 인스턴스 STOMP fan-out, Pub/Sub)
  audit/
    resources/db/migration/  V030 월별 파티셔닝
  app/
    + BusinessKpiPersister (NS-01~S09 일별 영속)

frontend/
  e2e/
    + vc-swap-cascade.spec.ts (Playwright 시나리오)
    + ex-matrix-stomp.spec.ts
    + master-restore.spec.ts
  src/i18n/locales/
    + en.json (EN translations — EP-43)
  vite.config.ts (manualChunks ant-design lazy split — EP-46)

infra/
  observability/
    prometheus.yml + grafana/dashboards/*.json (4 대시)
    loki.yml + promtail-config.yml
  docker-compose.stg.yml (베타 운영 환경 — Blue/Green NGINX)
  scripts/k6/
    + 1500-row-matrix.js (k6 부하 시나리오)
```

---

## 6. Sprint 6 DoD (진입 게이트 충족 → 종료 게이트 목표)

| 영역 | 지표 | 목표 |
|---|---|---|
| **EP-E2E Playwright** | vc → swap → cascade chain | Chromium 통과 100% |
| **EP-E2E 베타 시나리오** | 5건 사용자 시나리오 (정상·예외) | 100% |
| **EP-40 UI p95** | 1500 row × 30 col 첫 렌더 | ≤ 500ms |
| **EP-40 API p95** | matrix + ranking + snapshot | ≤ 800ms |
| **EP-40 k6 동시 사용자** | 100 user 1500 row 부하 | error < 1% |
| **EP-41 Kakao retry** | Resilience4j @Retry max 3 + Circuit | 활성 |
| **EP-41 event 영속** | spring-modulith-events-jpa | 100% (재시작 복구) |
| **EP-42 Keycloak** | OIDC JWT + role mapping | 100% |
| **EP-43 i18n EN** | 모든 페이지 EN 번역 | 100% |
| **EP-44 Prometheus** | scrape + 4 Grafana 대시 + Slack alert | 활성 |
| **EP-45 Excel** | 원본 호환 셀-수준 차이 | ≤ 2% |
| **EP-46 Vite bundle** | first entry gzip | ≤ 200kB (ant-design lazy 분리) |
| **EP-46 jacoco** | 백엔드 coverage | ≥ 80% |
| **EP-47 NS-S04** | 알림 도달률 ≥ 95% | KPI 영속 + 대시 |
| **EP-47 NS-S09** | 신규 라인 사용률 ≥ 90% | KPI 영속 + 대시 |
| **인프라 audit 파티셔닝** | 월별 LIST partition 36개 | 100% |
| **인프라 Redis Pub/Sub** | STOMP 다중 인스턴스 fan-out | 통합 |
| **인프라 STG** | Docker Compose Blue/Green | 활성 |
| **Modulith verify** | 0 위반 | 0 |
| **회귀** | 백엔드 + 프론트 전수 + E2E | 0 failure |

---

## 7. 진입 게이트 체크리스트 (Sprint 5 완료 → Sprint 6 진입)

- [x] **Sprint 5 7 Epic 100% 완료** (EP-15·16·17·18·19·20) — Sprint-5_Completion §10
- [x] **Frontend 본격 활성** (React 18 + Vite 5 + AG Grid Enterprise + STOMP)
- [x] **UI 페이지 4종** + Router/Menu 활성
- [x] **REST API 안정** — 모든 Sprint 1~5 endpoint 통합
- [x] **Modulith verify 0 위반** + ArchUnit 29 rule 통과
- [x] **Frontend 50 vitest + lint 0 + production build 통과**
- [x] **백엔드 회귀 ≥ 99%** (212+ tests · 0 failure)
- [x] **누적 116 commit** (Sprint 0~5) · 머지 충돌 0
- [x] **거버넌스 4-layer** (DB trigger + AOP + RBAC + immutability) 통과

→ **Sprint 6 진입 승인 가능**. Phase A (EP-E2E + EP-40 병렬) 즉시 시작 가능.

---

## 8. 잠재 리스크 + 완화 전략

| 리스크 | 영향 | 완화 |
|---|---|---|
| Playwright Chromium 환경 (CI) | E2E 통과 차단 | local Chrome 빌드 + GHA matrix 추가 검토 |
| k6 1500 row 부하 시 Hibernate N+1 | API p95 초과 | QueryDSL projection + EntityGraph 적용 (Sprint 6 진단) |
| Resilience4j BulkHead + Modulith Async 충돌 | event drop | spring-modulith-events-jpa 활성 후 단계별 도입 |
| audit 36 partition 마이그레이션 시간 | 베타 진입 지연 | 빈 partition pre-create + lazy attach |
| Keycloak OIDC SAML 사내 IdP 정합 | 보안 인증 차단 | local fallback (사내 mockup) + Sprint 7 본격 |
| Vite ant-design 1.2MB → lazy split 회귀 | UI 진입 지연 | dynamic import() + skeleton fallback |
| 베타 운영 시 데이터 손실 | 사용자 불신 | pg_basebackup + WAL archiving + PITR 검증 |

---

## 9. Sprint 6 종료 시 — Phase 3 완료 보고 가능

Sprint 6 종료 = **Phase 3 (개발) 완료 + Phase 4 (운영 베타) 진입 게이트** 도달:
- Sprint 0 (인프라 + 인증 + CI/CD + CO) ✅
- Sprint 1 (수주 통합) ✅
- Sprint 2 (성형 가류) ✅
- Sprint 3 (압출 종단) ✅
- Sprint 4 (거버넌스 + 일중 락) ✅
- Sprint 5 (UI 통합) ✅
- Sprint 6 (E2E + NFR + 베타) ⏳ → **Phase 3 완료**

Phase 3 완료 보고서는 Sprint 6 종료 시 별도 문서 (`Phase-3_Completion_v1.0.md`) 로 작성.

---

## 10. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-22 | Claude Code | 초안 — Sprint 6 진입 계획 (EP-E2E + EP-40~47 + 인프라 = ~38 SP, critical path 19 SP, Phase 3 마지막 Sprint) |
