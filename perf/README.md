# 부하 / 성능 테스트 — k6 (TK-03-3-3 및 후속)

본 폴더는 [k6](https://k6.io) 기반 부하 시나리오 스크립트와 결과 리포트를 담는다.
Sprint 1 — EP-03 ST-03-3 알림 SLA (REQ-NF-KPI-015 K-O04) 검증부터 시작.

---

## 디렉터리

```
perf/
  scripts/
    notification_sla_test.js              # TC-OC-009 Critical 100건 1분 SLA
    notification_normal_sla_test.js       # 부속 Normal 100건 5분 SLA
    fixtures/
      critical_events.json                # BR-O02 시나리오 표본 (디버깅 / 데이터 시드용)
  reports/                                # k6 handleSummary 결과 (gitignore)
  README.md                               # 본 문서
```

---

## 사전 준비

1. **k6 설치** — 로컬: `winget install k6`, CI: Jenkins agent 에 `k6 ≥ 0.50` 사전 설치
2. **JWT 토큰 발급** — STG Keycloak realm 에서 `PLANNER` role 토큰 획득. 환경 변수 `STG_JWT_PLANNER`
3. **STG 환경 가용** — `http://stg.intranet/api/v1` 접근 가능. Backend + Keycloak + Redis + Postgres 모두 healthy
4. **테스트 dispatch endpoint 활성** — Sprint 2 EP-03 부속 작업에서 `POST /api/v1/orders/_test/dispatch-notification` 활성 (DEV/STG 전용, PROD 비활성)

---

## 실행 — Critical 1분 SLA (TC-OC-009)

```bash
k6 run \
  -e BASE_URL=http://stg.intranet/api/v1 \
  -e JWT_TOKEN=$STG_JWT_PLANNER \
  perf/scripts/notification_sla_test.js
```

### 성공 기준 (Sprint 1 DoD)

| 지표 | 임계 | 측정 |
|---|---|---|
| `sla_compliance` | ≥ 0.99 | rate (ack ≤ 60s) |
| `notification_dispatch_to_ack_ms` | p95 < 60_000 | trend |
| `http_req_failed` | ≤ 0.01 | rate |
| `critical_dispatched_total` | = 100 | counter |

---

## 실행 — Normal 5분 SLA (부속)

```bash
k6 run \
  -e BASE_URL=http://stg.intranet/api/v1 \
  -e JWT_TOKEN=$STG_JWT_PLANNER \
  perf/scripts/notification_normal_sla_test.js
```

---

## Jenkins CI 통합

Jenkinsfile (또는 `infrastructure/jenkins/Jenkinsfile.perf`) 에 다음 stage 추가:

```groovy
stage('Notification SLA Test') {
    when {
        anyOf {
            branch 'main'
            expression { params.RUN_PERF == true }
        }
    }
    environment {
        JWT_TOKEN = credentials('stg-jwt-planner')
    }
    steps {
        sh '''
            k6 run \
              -e BASE_URL=http://stg.intranet/api/v1 \
              -e JWT_TOKEN=$JWT_TOKEN \
              perf/scripts/notification_sla_test.js
        '''
        archiveArtifacts artifacts: 'perf/reports/notification_sla_summary.json',
                         allowEmptyArchive: false
    }
    post {
        failure {
            slackSend(
                channel: '#ops-scheduling',
                color: 'danger',
                message: "❗ Notification SLA test FAILED on ${env.BRANCH_NAME} — REQ-NF-KPI-015 위반 (build ${env.BUILD_NUMBER})"
            )
        }
    }
}
```

> ⚠️ Sprint 1 단계 — Jenkins shared-library / Jenkinsfile 실 활성은 EP-32 (Jenkins CI) 완료 후.
> 본 문서는 stage 정의만 명세, 실 통합은 Sprint 2 진입 시 PR.

---

## Grafana 대시보드 (TK-03-3-3 후속)

대시보드 JSON: `infrastructure/observability/grafana/dashboards/notification-sla.json` (Sprint 2 작성 예정).

핵심 패널 (제안):

- **K-O04 Critical SLA 준수율** — `rate(sla_compliance[5m])` (target 0.99 green line)
- **Dispatch → ACK p95** — `histogram_quantile(0.95, notification_dispatch_to_ack_ms)` (target <60_000)
- **카카오 회로차단기 상태** — `resilience4j_circuitbreaker_state{name="kakaotalk"}` (Sprint 2 활성)
- **미 ack Critical 누적** — `idx_notification_undelivered_critical` partial index count

---

## 안정성 확인 (DoD §부하 테스트 30회 연속 PASS)

다음 wrapper 스크립트로 5회 연속 실행 (수동):

```bash
for i in 1 2 3 4 5; do
  echo "=== Run $i ==="
  k6 run \
    -e BASE_URL=http://stg.intranet/api/v1 \
    -e JWT_TOKEN=$STG_JWT_PLANNER \
    perf/scripts/notification_sla_test.js \
    --summary-export "perf/reports/run-$i.json" \
    || exit 1
done
```

flaky 0건 + 전 회차 SLA ≥ 99% 시 Sprint Review 데모 가능.

---

## 트러블슈팅

| 증상 | 조치 |
|---|---|
| `dispatch 202` 미응답 | STG `/orders/_test/dispatch-notification` endpoint 활성 확인 (Sprint 2 EP-03) |
| `sla_compliance < 0.99` | Critical 알림 queue / Resilience4j 회로 상태 / Kakao webhook latency 점검 (Grafana) |
| `http_req_failed > 0.01` | JWT 만료 / STG 인증 서비스 (Keycloak) 가용성 |
| Polling 무한 대기 | ACK_TIMEOUT_MS 도달 후 자동 종료 — 정상 동작. 단, 매 회 timeout 시 K-O04 위반 |

---

## 참조

- TK-03-3-3: `Phase 2/4.Tasks/Tasks/EP-03/ST-03-3/TK-03-3-3.md`
- TestPlan: TC-OC-009 (Critical SLA 100건 ≥99%)
- SRS: REQ-NF-KPI-015 (K-O04), REQ-FUNC-OC-009 (SLA 정의)
- SAD: §3.1 EXT-SYS-05 카카오톡 Workplace Bot
