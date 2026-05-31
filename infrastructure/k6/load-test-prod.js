/**
 * Sprint 25 S25-A ST-PROD-2 — k6 부하 검증 (Production-readiness gate).
 *
 * <p>대상 NFR (PERF-001 정합):
 * <ul>
 *   <li>REQ-NF-PER-001 — EX matrix API p95 ≤ 800ms</li>
 *   <li>REQ-NF-PER-003 — VC slots API p95 ≤ 800ms</li>
 *   <li>REQ-NF-PER-004 — STOMP / mutation flow p95 ≤ 800ms (느슨하게 통합 측정)</li>
 *   <li>REQ-NF-PER-007 — 30 VU 5분 ramping · error rate &lt; 0.1% (PLAN-SPRINT-25 strict)</li>
 * </ul>
 *
 * <p>실행 (STG 전제):
 * <pre>
 *   export BASE_URL=http://stg.intranet:8080
 *   export JWT_TOKEN_LIST="$JWT1,$JWT2,...,$JWT30"   # 30 PLANNER JWT (콤마 구분)
 *   export PLANNER_JWT=$JWT_FALLBACK                  # 폴백 (단일 JWT 일괄)
 *   export HORIZON_FROM=2026-06-01
 *   export HORIZON_TO=2026-06-08
 *   k6 run infrastructure/k6/load-test-prod.js
 * </pre>
 *
 * <p>Sprint 24 ST-MES-3 (commit 326bf6f) WireMock 시나리오 분리 패턴을 참고,
 * 시나리오별 check() block 을 명확히 분리. 실 실행은 S25-B 에서 STG seed
 * (DS-VC-CONSTRAINT-47 + 1주 horizon) 확보 후 수행.
 */

import http from 'k6/http'
import { check, sleep, group } from 'k6'
import { Trend, Rate, Counter } from 'k6/metrics'

// ----------------------------------------------------------------------------
// 환경 변수
// ----------------------------------------------------------------------------
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const JWT_TOKEN_LIST = (__ENV.JWT_TOKEN_LIST || '').split(',').filter((t) => t.length > 0)
const PLANNER_JWT = __ENV.PLANNER_JWT || ''
const HORIZON_FROM = __ENV.HORIZON_FROM || '2026-06-01'
const HORIZON_TO = __ENV.HORIZON_TO || '2026-06-08'

// 사번/PIN 로컬 폴백 (NFR-SEC-007 — Keycloak 미가용 시)
const LOCAL_AUTH_EMP = __ENV.LOCAL_AUTH_EMP || ''   // 8자리 사번 콤마구분
const LOCAL_AUTH_PIN = __ENV.LOCAL_AUTH_PIN || ''   // 4자리 PIN (전 VU 공통)

// ----------------------------------------------------------------------------
// Custom 메트릭
// ----------------------------------------------------------------------------
const loginDuration = new Trend('login_duration_ms', true)
const vcSlotsDuration = new Trend('vc_slots_duration_ms', true)
const matrixDuration = new Trend('ex_matrix_duration_ms', true)
const rankingDuration = new Trend('ex_ranking_duration_ms', true)
const diffDuration = new Trend('schedule_diff_duration_ms', true)
const confirmDuration = new Trend('schedule_confirm_duration_ms', true)
const errorRate = new Rate('journey_errors')
const confirmAuditCount = new Counter('confirm_audit_total')

// ----------------------------------------------------------------------------
// 시나리오 — 30 VU × 5분 ramping (PLAN-SPRINT-25 명세)
// ----------------------------------------------------------------------------
export const options = {
  scenarios: {
    prod_readiness: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },   // warm-up
        { duration: '1m',  target: 20 },   // ramp
        { duration: '2m',  target: 30 },   // peak
        { duration: '1m',  target: 30 },   // hold
        { duration: '30s', target: 0 },    // ramp down
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    // PERF-001 정합 (REQ-NF-PER-001/003/004)
    'http_req_duration':         ['p(95)<800'],
    'http_req_failed':           ['rate<0.001'],     // PLAN strict — 0.1% (baseline 1%)
    'iteration_duration':        ['p(95)<5000'],     // 1 journey ≤ 5s
    // 보조 — 시나리오별 (정보성)
    'vc_slots_duration_ms':      ['p(95)<800'],      // REQ-NF-PER-003
    'ex_matrix_duration_ms':     ['p(95)<800'],      // REQ-NF-PER-001
    'ex_ranking_duration_ms':    ['p(95)<1200'],     // REQ-NF-PER-002
    'schedule_diff_duration_ms': ['p(95)<800'],
    'journey_errors':            ['rate<0.001'],
  },
}

// ----------------------------------------------------------------------------
// VU 별 JWT 선택 — JWT_TOKEN_LIST 우선, 없으면 PLANNER_JWT 폴백
// ----------------------------------------------------------------------------
function resolveToken() {
  if (JWT_TOKEN_LIST.length > 0) {
    return JWT_TOKEN_LIST[(__VU - 1) % JWT_TOKEN_LIST.length]
  }
  return PLANNER_JWT
}

// 로컬 사번/PIN 로그인 (Keycloak 미가용 폴백 — BR-X06 유사 패턴)
function loginLocal() {
  const empList = LOCAL_AUTH_EMP.split(',').filter((e) => e.length === 8)
  if (empList.length === 0 || LOCAL_AUTH_PIN.length !== 4) return null
  const emp = empList[(__VU - 1) % empList.length]
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ employeeNo: emp, pin: LOCAL_AUTH_PIN }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
  )
  loginDuration.add(res.timings.duration)
  const ok = check(res, {
    'login 200': (r) => r.status === 200,
    'login token present': (r) => !!r.json('accessToken'),
  })
  if (!ok) { errorRate.add(1); return null }
  return res.json('accessToken')
}

// ----------------------------------------------------------------------------
// User Journey — 6 step
// ----------------------------------------------------------------------------
export default function () {
  // Step 1 — login (Keycloak JWT pre-issued OR local fallback)
  let token = resolveToken()
  if (!token) token = loginLocal()
  if (!token) { errorRate.add(1); return }
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }

  // Step 2 — VC slots (REQ-NF-PER-003)
  group('vc_slots', () => {
    const res = http.get(`${BASE_URL}/api/v1/schedule/vc/slots`, {
      headers, tags: { name: 'vc_slots' },
    })
    vcSlotsDuration.add(res.timings.duration)
    const ok = check(res, {
      'vc/slots 200': (r) => r.status === 200,
      'vc/slots array': (r) => Array.isArray(r.json()),
    })
    if (!ok) errorRate.add(1)
  })
  sleep(0.3)

  // Step 3 — EX matrix 1500×30 (REQ-NF-PER-001)
  group('ex_matrix', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/schedule/ex/matrix?from=${HORIZON_FROM}&to=${HORIZON_TO}`,
      { headers, tags: { name: 'ex_matrix' } },
    )
    matrixDuration.add(res.timings.duration)
    const ok = check(res, {
      'ex/matrix 200': (r) => r.status === 200,
      'ex/matrix body array': (r) => Array.isArray(r.json()),
      'ex/matrix >=1 row': (r) => (r.json() || []).length >= 1,
    })
    if (!ok) errorRate.add(1)
  })
  sleep(0.5)

  // Step 4 — EX ranking (REQ-NF-PER-002)
  group('ex_ranking', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/schedule/ex/candidates/ranking?from=${HORIZON_FROM}&to=${HORIZON_TO}&limit=10`,
      { headers, tags: { name: 'ex_ranking' } },
    )
    rankingDuration.add(res.timings.duration)
    const ok = check(res, {
      'ex/ranking 200': (r) => r.status === 200,
      'ex/ranking array': (r) => Array.isArray(r.json()),
    })
    if (!ok) errorRate.add(1)
  })
  sleep(0.3)

  // Step 5 — schedule diff (확정 게이트 BR-X01 pre-check)
  group('schedule_diff', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/schedule/diff?from=${HORIZON_FROM}&to=${HORIZON_TO}`,
      { headers, tags: { name: 'schedule_diff' } },
    )
    diffDuration.add(res.timings.duration)
    const ok = check(res, {
      'diff 200': (r) => r.status === 200,
      'diff body present': (r) => r.body && r.body.length > 0,
    })
    if (!ok) errorRate.add(1)
  })
  sleep(0.3)

  // Step 6 — schedule confirm POST (audit 정합 BR-X02)
  // NOTE — confirm 은 BR-X01 D-2~D-1 gate + BR-V07 D-0 락 → 대량 confirm 회피
  //        실 부하에서는 200 (성공) 또는 409 (gate 차단) 모두 허용
  group('schedule_confirm', () => {
    const payload = JSON.stringify({
      from: HORIZON_FROM,
      to: HORIZON_TO,
      dryRun: true,                // 부하 측정 — dryRun true 로 audit 흐름만 검증
    })
    const res = http.post(
      `${BASE_URL}/api/v1/schedule/confirm`,
      payload,
      { headers, tags: { name: 'schedule_confirm' } },
    )
    confirmDuration.add(res.timings.duration)
    const ok = check(res, {
      'confirm 2xx/409': (r) => (r.status >= 200 && r.status < 300) || r.status === 409,
      'confirm audit traceId': (r) => !!r.headers['X-Trace-Id'] || !!r.headers['Traceid'],
    })
    if (ok && res.status >= 200 && res.status < 300) confirmAuditCount.add(1)
    if (!ok) errorRate.add(1)
  })

  sleep(1.0)
}

// ----------------------------------------------------------------------------
// 요약 — handleSummary
// ----------------------------------------------------------------------------
export function handleSummary(data) {
  return {
    stdout: textSummary(data),
    'infrastructure/k6/reports/load-test-prod-summary.json': JSON.stringify(data, null, 2),
  }
}

function textSummary(data) {
  const m = data.metrics
  const p95 = (key) => (m[key]?.values?.['p(95)'] ?? 0).toFixed(0)
  const rate = (key) => ((m[key]?.values?.rate ?? 0) * 100).toFixed(3)
  let out = '\n=== Sprint 25 ST-PROD-2 — Production Readiness k6 결과 ===\n'
  out += `login p95:           ${p95('login_duration_ms')}ms\n`
  out += `vc/slots p95:        ${p95('vc_slots_duration_ms')}ms (target <800)\n`
  out += `ex/matrix p95:       ${p95('ex_matrix_duration_ms')}ms (target <800)\n`
  out += `ex/ranking p95:      ${p95('ex_ranking_duration_ms')}ms (target <1200)\n`
  out += `schedule/diff p95:   ${p95('schedule_diff_duration_ms')}ms (target <800)\n`
  out += `schedule/confirm p95:${p95('schedule_confirm_duration_ms')}ms\n`
  out += `http_req_duration p95:${p95('http_req_duration')}ms\n`
  out += `journey_errors:      ${rate('journey_errors')}% (target <0.1)\n`
  out += `http_req_failed:     ${rate('http_req_failed')}% (target <0.1)\n`
  out += `confirm audit count: ${m.confirm_audit_total?.values?.count ?? 0}\n`
  return out
}
