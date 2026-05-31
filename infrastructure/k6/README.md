# infrastructure/k6 — Production Readiness 부하 테스트

본 디렉터리는 Sprint 25 S25-A ST-PROD-2 (Production-readiness) 단계에서 신설된
k6 부하 스크립트를 담는다. Sprint 6 `infra/k6/matrix-1500-row.js` (EP-40 100 VU
baseline) 의 후속으로, **30 VU × 5분 ramping** + **PLANNER user journey 6 step** 을
실 운영 시나리오에 맞춰 통합 검증한다.

---

## 디렉터리

```
infrastructure/k6/
  load-test-prod.js                 # 본 스크립트 — 30 VU ramping + 6 step journey
  reports/                          # handleSummary JSON (gitignore)
  README.md                         # 본 문서
```

---

## 사전 준비

1. **k6 설치** — 로컬 `winget install k6`, Jenkins agent `k6 >= 0.50`
2. **STG 환경 healthy** — Backend + Keycloak + Redis + Postgres + DS-VC-CONSTRAINT-47 seed + 1주 horizon 시드
3. **JWT 30개 발급** — STG Keycloak `PLANNER` role 토큰 30개 사전 발급
   - 사용자 부족 시 단일 `PLANNER_JWT` 폴백 가능 (단, rate-limit / per-user 캐시 측정 부정확)
   - 또는 `LOCAL_AUTH_EMP` + `LOCAL_AUTH_PIN` 으로 사번 8자리 + PIN 4자리 (NFR-SEC-007) 로그인 폴백
4. **dryRun confirm endpoint** — `POST /api/v1/schedule/confirm { dryRun: true }` 활성
   (audit 흐름만 검증, BR-X01 D-2~D-1 gate 통과 시 200, gate 차단 시 409 허용)

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `BASE_URL` | `http://localhost:8080` | STG backend root |
| `JWT_TOKEN_LIST` | (empty) | 30 PLANNER JWT 콤마구분. VU 별 round-robin |
| `PLANNER_JWT` | (empty) | 단일 JWT 폴백 (`JWT_TOKEN_LIST` 미설정 시) |
| `LOCAL_AUTH_EMP` | (empty) | 사번 8자리 콤마구분 (Keycloak 미가용 폴백) |
| `LOCAL_AUTH_PIN` | (empty) | PIN 4자리 (전 VU 공통) |
| `HORIZON_FROM` | `2026-06-01` | 1주 horizon 시작 |
| `HORIZON_TO` | `2026-06-08` | 1주 horizon 종료 |

---

## 실행

```bash
export BASE_URL=http://stg.intranet:8080
export JWT_TOKEN_LIST="$(cat stg-30-jwt.txt | tr '\n' ',')"
export HORIZON_FROM=2026-06-01
export HORIZON_TO=2026-06-08
k6 run infrastructure/k6/load-test-prod.js
```

threshold 미충족 시 exit code != 0 → Jenkins gate fail.

---

## Threshold (PLAN-SPRINT-25 정합)

| Threshold | 임계 | NFR |
|---|---|---|
| `http_req_duration p(95)` | < 800ms | REQ-NF-PER-001/003/004 |
| `http_req_failed rate` | < 0.001 (0.1%) | PLAN strict (baseline 1%) |
| `iteration_duration p(95)` | < 5s | 1 journey 5초 |
| `vc_slots_duration_ms p(95)` | < 800ms | REQ-NF-PER-003 |
| `ex_matrix_duration_ms p(95)` | < 800ms | REQ-NF-PER-001 |
| `ex_ranking_duration_ms p(95)` | < 1200ms | REQ-NF-PER-002 |

---

## Jenkins stage 통합 (권고)

```groovy
stage('Production Readiness k6 (Sprint 25 ST-PROD-2)') {
    when {
        anyOf {
            branch 'main'
            expression { params.RUN_PROD_PERF == true }
        }
    }
    environment {
        BASE_URL = 'http://stg.intranet:8080'
        JWT_TOKEN_LIST = credentials('stg-30-planner-jwts')
        HORIZON_FROM = '2026-06-01'
        HORIZON_TO = '2026-06-08'
    }
    steps {
        sh 'k6 run infrastructure/k6/load-test-prod.js'
        archiveArtifacts artifacts: 'infrastructure/k6/reports/load-test-prod-summary.json'
    }
}
```

---

## 실 실행 정책

- **S25-A** (본 task) — 스크립트 작성만. 실 실행 deferred
- **S25-B** — STG 환경 + JWT 30개 + DS seed 확보 후 실 실행 + 결과 PERF-003 리포트 발행
- **PROD 직 사용 금지** — `dryRun:true` 라도 PROD 트래픽 부하 부담 차단

---

## 참조

- Sprint 6 baseline — `infra/k6/matrix-1500-row.js` (100 VU)
- NFR 명세 — `docs/perf/PERF-001_NFR_Performance_Spec_v1.0.md`
- 알림 SLA — `perf/README.md` (TK-03-3-3 Notification 1분 SLA)
- Sprint 24 WireMock 패턴 — commit `326bf6f`
