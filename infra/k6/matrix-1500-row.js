/**
 * EP-40 부하 시나리오 — 1500 row × 30 col 매트릭스 fetch (REQ-NF-PER-001·002·005).
 *
 * <p>목표:
 * <ul>
 *   <li>API p95 (matrix fetch) ≤ 800ms</li>
 *   <li>API p95 (ranking) ≤ 1,200ms</li>
 *   <li>동시 100 사용자 5분 부하 error rate < 1%</li>
 *   <li>throughput ≥ 50 req/s</li>
 * </ul>
 *
 * <p>실행: {@code k6 run infra/k6/matrix-1500-row.js}
 * {@code K6_BASE_URL} 환경변수로 백엔드 URL 지정 (기본 http://localhost:8080).
 *
 * <p>STG 환경 전용 — DS-VC-CONSTRAINT-47 + 1주 horizon 시드 데이터 의존.
 */

import http from 'k6/http'
import { check, sleep } from 'k6'
import { Trend, Rate } from 'k6/metrics'

const baseUrl = __ENV.K6_BASE_URL || 'http://localhost:8080'
const token = __ENV.K6_JWT || ''

const matrixDuration = new Trend('matrix_duration_ms', true)
const rankingDuration = new Trend('ranking_duration_ms', true)
const errorRate = new Rate('errors')

export const options = {
  scenarios: {
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },     // ramp to 20
        { duration: '1m',  target: 50 },     // ramp to 50
        { duration: '2m',  target: 100 },    // peak 100 user
        { duration: '1m',  target: 100 },    // hold
        { duration: '30s', target: 0 },      // ramp down
      ],
    },
  },
  thresholds: {
    'matrix_duration_ms':  ['p(95)<800'],    // REQ-NF-PER-001
    'ranking_duration_ms': ['p(95)<1200'],   // REQ-NF-PER-002
    'errors':              ['rate<0.01'],    // < 1%
    'http_req_duration':   ['p(95)<2000'],   // 전체 API p95 < 2s
  },
}

const headers = token ? { Authorization: `Bearer ${token}` } : {}

const FROM = '2026-05-25'
const TO = '2026-06-01'

export default function () {
  // EP-17 매트릭스 fetch
  const matrixRes = http.get(
    `${baseUrl}/api/v1/schedule/ex/matrix?from=${FROM}&to=${TO}`,
    { headers, tags: { name: 'matrix' } },
  )
  matrixDuration.add(matrixRes.timings.duration)
  const matrixOk = check(matrixRes, {
    'matrix status 200': (r) => r.status === 200,
    'matrix body array': (r) => Array.isArray(r.json()),
  })
  if (!matrixOk) errorRate.add(1)

  sleep(0.5)

  // EP-18 ranking fetch
  const rankingRes = http.get(
    `${baseUrl}/api/v1/schedule/ex/candidates/ranking?from=${FROM}&to=${TO}&limit=10`,
    { headers, tags: { name: 'ranking' } },
  )
  rankingDuration.add(rankingRes.timings.duration)
  const rankingOk = check(rankingRes, {
    'ranking status 200': (r) => r.status === 200,
    'ranking >= 0 candidates': (r) => Array.isArray(r.json()),
  })
  if (!rankingOk) errorRate.add(1)

  sleep(1.0)
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'matrix-1500-row-summary.json': JSON.stringify(data, null, 2),
  }
}

// 간단한 콘솔 요약 (k6 기본 textSummary 가져옴)
function textSummary(data, opts) {
  const ind = opts?.indent ?? ''
  let out = ''
  out += `\n${ind}=== EP-40 k6 1500-row 부하 결과 ===\n`
  const m = data.metrics
  out += `${ind}matrix p95:  ${(m.matrix_duration_ms?.values?.['p(95)'] ?? 0).toFixed(0)}ms (목표 ≤ 800ms)\n`
  out += `${ind}ranking p95: ${(m.ranking_duration_ms?.values?.['p(95)'] ?? 0).toFixed(0)}ms (목표 ≤ 1200ms)\n`
  out += `${ind}error rate:  ${((m.errors?.values?.rate ?? 0) * 100).toFixed(2)}% (목표 < 1%)\n`
  out += `${ind}throughput:  ${(m.http_reqs?.values?.rate ?? 0).toFixed(1)} req/s (목표 ≥ 50)\n`
  return out
}
