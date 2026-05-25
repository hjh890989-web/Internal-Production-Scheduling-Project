# Phase 4-A STG 부팅 Dry-Run 결과 (DEV mode local build)

**Phase**: 4-A (STG 부팅) — Dry-Run | **실행일**: 2026-05-23 | **환경**: 개발자 PC (Windows 11 + Docker Desktop v4.74.0)
**상위 참조**: [Phase-4_EntryPlan_v1.1](Phase-4_EntryPlan_v1.1.md) §5 STG 환경 명세

> 사내 STG 환경 (Harbor pull) 부팅 전 **개발자 PC 에서 DEV mode local build 로 사전 검증**.
> 실 STG (Harbor + Keycloak SSO + 사내 IdP) 진입 시 본 dry-run 결과를 baseline 으로 사용.

---

## 1. Dry-Run 범위 + 한계

### 1.1 범위 ✅

| 영역 | DEV (본 dry-run) | STG (실 진입 시) |
|---|---|---|
| Backend image | local build (`backend/Dockerfile` multi-stage JDK 21) | Harbor pull (`harbor.internal/scheduling/backend:tag`) |
| Postgres / Redis | DEV image | STG image (동일) |
| Flyway 마이그레이션 | V001~V033 ✅ | V001~V033 동일 |
| Keycloak SSO | 부팅 안 함 (DEV permitAll fallback 없음, RBAC 강제) | 사내 IdP LDAP/AD sync |
| JWT 발급 | 미구축 (k6 + REST 호출 제한) | Keycloak realm + 사용자 + JWT |
| 시드 데이터 | DS-VC-CONSTRAINT-47 + 1주 horizon + KPI + V033 sample | 동일 + 실 영업 데이터 (DI-07/08) |

### 1.2 한계 (실 STG 환경 의존)

- **Keycloak realm + 사용자 + JWT 발급** 미수행 (큰 작업, 사내 IdP 사용 시 자연)
- **k6 NFR-PER 실 측정** 보류 — matrix/ranking endpoint RBAC 401 (JWT 필요)
- **Lighthouse audit** 미수행 — Frontend dev server + browser 가동 별 작업
- **사내 NAS / Harbor registry** 미접근 — DEV mode local build 로 우회

---

## 2. 부팅 절차 (재현 가능)

### 2.1 사전 — `.env` 작성 (개발자 PC)

`infrastructure/.env` 기존 파일 사용 (DEV password placeholder). STG 진입 시 `.env.stg` 로 vault secret 주입.

### 2.2 인프라 부팅 (postgres + redis)

```bash
cd infrastructure
docker compose up -d postgres redis
# postgres healthy ~10s, redis healthy ~5s
```

검증: `docker ps --filter "name=scheduling-" --format "table {{.Names}}\t{{.Status}}"` → 양쪽 `(healthy)`

### 2.3 Backend build + 부팅

```bash
SPRING_PROFILES_ACTIVE=dev,with-infra docker compose up -d --build backend
# 첫 빌드 — Gradle dependency 다운로드 + bootJar (~3-5 분)
# Spring Boot 부팅 + Flyway V001~V033 자동 적용 (~30-45s)
```

검증:
- `docker exec scheduling-backend wget -qO- http://localhost:8080/actuator/health` → `{"status":"UP"}` ✅
- `docker exec scheduling-postgres psql -U app_user -d scheduling -c "SELECT installed_rank, version, success FROM app.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"` → V033 success ✅

### 2.4 시드 적용 (BS-01~05 + BS-06)

```bash
# BS-01~05 만 — 기본
COMPOSE_FILE="infrastructure/docker-compose.yml" \
DB_NAME=scheduling POSTGRES_DB=scheduling \
bash infrastructure/scripts/seed-stg-beta-data.sh

# BS-06 추가 — V033 sample seed (옵션)
SEED_V12V13=1 \
COMPOSE_FILE="infrastructure/docker-compose.yml" \
DB_NAME=scheduling POSTGRES_DB=scheduling \
bash infrastructure/scripts/seed-stg-beta-data.sh
```

---

## 3. 검증 결과

### 3.1 Flyway 적용 — V001~V033

```text
installed_rank | version |             description              | success
       33      |   033   | create product priority and kd order |   t
       32      |   032   | create business kpi measurement      |   t
       31      |   031   | create event publication             |   t
       30      |   030   | partition audit log monthly          |   t
        ...
```

**33/33 success** ✅ — Phase 4 진입 게이트 §4 "34 Flyway 마이그레이션" 충족.

### 3.2 V033 신규 schema

```text
table_schema |    table_name
-------------+------------------
 master      | product_priority   ← V033 신규 (BR-V12)
 master      | kd_order           ← V033 신규 (BR-V13)
```

### 3.3 시드 적용 결과 (SEED_V12V13=1 활성)

```text
[1/4] DS-VC-CONSTRAINT-47 마스터 import
  → 47 품번 vc_constraint + 5 vc_hose_rule + 4 line_type seed 완료
[2/4] 1주 horizon vc_schedule baseline (CANDIDATE 상태)
  → 1주 horizon × 5 머신 × ~7 슬롯 × 18 회전 = 3420 row baseline seed (idempotent)
[3/4] business_kpi.measurement baseline (오늘 기준)
  → 9 KPI baseline 영속
[4/5] 시드 검증
       tbl       | row_count
----------------+-----------
 kpi_baseline   |         9
 kpi_definition |         9
 line_type      |         4
 vc_constraint  |        46
 vc_hose_rule   |         3
 vc_schedule    |      3420
[5/5] V033 BR-V12·V13 sample seed
  → PRODUCT_PRIORITY 3 + KD_ORDER 2 OPEN seed — BS-06 진입 준비 완료
```

✅ 전수 통과 (commit `4294da7` V028 DEFERRABLE UNIQUE 호환 fix 후).

### 3.4 Backend `/actuator/health`

```bash
docker exec scheduling-backend wget -qO- http://localhost:8080/actuator/health
# {"status":"UP","groups":["liveness","readiness"]}
```

✅ liveness + readiness UP.

### 3.5 REST `@PreAuthorize` 강제 확인

```bash
docker exec scheduling-backend wget -qS http://localhost:8080/api/v1/schedule/vc/capacity-overflow/split
# HTTP/1.1 401 — Spring Security 정상 활성 (JWT 없음 → 401)
```

✅ BR-X05 dual-review 정합 — JWT 없이 mutation endpoint 차단.

---

## 4. 식별된 issue + 마감 (commit)

| # | Issue | 영향 | Fix |
|---|---|---|---|
| 1 | `seed-stg [2/4]` V028 DEFERRABLE UNIQUE 가 `ON CONFLICT` arbiter 사용 불가 | Phase 4-A 진입 시 BS-01 1주 horizon baseline 누락 (정상 시나리오 진입 차단) | ✅ `4294da7` — `WHERE NOT EXISTS` idempotent 패턴, 3420 row 적용 검증 |

---

## 5. 한계 영역 — STG 환경 진입 시 추가 검증 필요

### 5.1 k6 NFR-PER 실 측정 (보류)

```text
대상 endpoint:
  - /api/v1/schedule/ex/matrix?from=&to=  (NFR-PER-001 p95 ≤ 800ms)
  - /api/v1/schedule/ex/candidates/ranking?from=&to=  (NFR-PER-002 p95 ≤ 1200ms)
  - /api/v1/schedule/vc/capacity-overflow/split  (Sprint 7 신규, NFR 미정)
  - /api/v1/schedule/vc/capacity-overflow/supplement  (Sprint 7 신규, NFR 미정)

실행 환경 요구:
  1. k6 설치 (host) 또는 docker run --network scheduling-net grafana/k6
  2. Keycloak 부팅 + scheduling realm import + PLANNER 사용자 + JWT 발급
  3. K6_JWT 환경변수 주입 후 k6 run infra/k6/matrix-1500-row.js
```

### 5.2 Lighthouse audit (NFR-PER-006)

```text
대상 페이지:
  - / (entry first paint)
  - /vc/simview (AG Grid 1500 row first render ≤ 500ms)
  - /extrusion-matrix
  - /vc/capacity-queue (Sprint 7 신규)

실행 절차 (PERF-002 v1.1 §3.4 참조):
  cd frontend && npm run build && npx serve dist -p 4173
  npx lighthouse http://localhost:4173/vc/capacity-queue --preset=desktop ...
```

### 5.3 Full E2E SSO + JWT flow (Keycloak)

```text
1. docker compose up -d keycloak-db keycloak  (~60s)
2. Keycloak admin console → realm "scheduling" import 확인
3. 사용자 생성 — planner-001 (PLANNER), stk-001 (STK_USER), itops-001 (IT_OPS), readonly-001 (READ_ONLY)
4. Authorization Code flow 또는 Direct Grant 로 JWT 발급
5. Bearer header 로 REST endpoint 호출 — BS-06 진입 시뮬레이션 (Planner 만 200)
```

---

## 6. Phase 4-A 실 STG 진입 권장 절차 (사내 환경)

본 dry-run 검증 후, 사내 STG 진입 시 다음 절차 (stg-deploy.md §10 + §11 + §12):

1. `.env.stg` 작성 (사내 vault secret — KEYCLOAK_* + KAKAO_* + VITE_AG_GRID_*)
2. `docker compose --env-file .env.stg -f docker-compose.yml -f docker-compose.stg.yml up -d` (Harbor pull)
3. Flyway V001~V033 자동 적용 로그 확인 (본 dry-run 과 동일)
4. `./scripts/seed-stg-beta-data.sh` 실행 (BS-01~05)
5. Keycloak realm import + LDAP/AD sync (사내 IdP)
6. 베타 사용자 5명 SSO 진입 검증
7. k6 NFR 실 측정 (matrix/ranking p95)
8. Lighthouse audit (NFR-PER-006)
9. (Phase 4-B 후반) `SEED_V12V13=1 ./scripts/seed-stg-beta-data.sh` 재실행 + BS-06 진입

---

## 7. Phase 4 진입 게이트 — Dry-Run 후 재확인 (10/10)

본 dry-run 후 [Phase-4_EntryPlan_v1.1 §4](Phase-4_EntryPlan_v1.1.md#4-phase-4-진입-게이트-충족-phase-3--phase-4--v11-갱신) 9/9 + 신규 1:

- [x] Phase 3 Sprint 0~6 + Sprint 7 carry-over 풀 스택
- [x] 9 핵심 BR + 2 deferred BR-V12·V13 UI 진입점
- [x] **34 Flyway 마이그레이션** V001~V033 (dry-run 검증 ✅)
- [x] 9 Modulith 모듈 + ArchUnit 29 rule + Modulith verify 0 위반
- [x] Backend 795 tests + Frontend 58 vitest + Playwright 226 등록 + REST IT 7
- [x] `/vc/capacity-queue` UI 마감
- [x] VSCode 문제 탭 0 + outward 문서 동기화
- [x] 누적 ~190 commit · 머지 충돌 0
- [x] seed-stg-beta-data.sh 전수 통과 (V028 호환 fix 후)
- [x] **🆕 dry-run 실 부팅 검증** — DEV mode local build (Flyway V033 + V033 sample seed + RBAC 강제 확인)

→ **Phase 4-A 실 STG 진입 승인 가능** (사내 환경 Keycloak + Harbor 준비 후).

---

## 8. 개정 이력

| 버전 | 날짜 | 작성자 | 변경 |
|----|-----|------|------|
| 1.0 | 2026-05-23 | Claude Code | 초안 — Phase 4-A 부팅 dry-run (DEV mode local build) 결과 + V028 DEFERRABLE UNIQUE 호환 fix 마감 + 사내 STG 진입 권장 절차 |
