// =============================================================================
// perf/scripts/notification_sla_test.js — TK-03-3-3
// =============================================================================
// TC-OC-009: Critical 변경 100건 → SLA <1분 도달률 ≥99% (REQ-NF-KPI-015 K-O04)
// 부속: Normal 알림 ≤ 5분 SLA, HTTP 오류율 ≤ 1%.
//
// 실행 (STG):
//   k6 run \
//     -e BASE_URL=http://stg.intranet/api/v1 \
//     -e JWT_TOKEN=$STG_JWT_PLANNER \
//     perf/scripts/notification_sla_test.js
//
// 결과 — perf/reports/notification_sla_summary.json (Jenkins archiveArtifacts).
// =============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ---------- 메트릭 정의 ----------
const dispatchToAck = new Trend('notification_dispatch_to_ack_ms', true);
const slaCompliance = new Rate('sla_compliance');
const criticalDispatched = new Counter('critical_dispatched_total');
const ackTimeouts = new Counter('ack_timeouts_total');

// ---------- 부하 시나리오 ----------
export const options = {
    scenarios: {
        critical_burst: {
            executor: 'shared-iterations',
            vus: 10,                              // 30명 사용자 중 알림 대상 평균 10
            iterations: 100,                       // TC-OC-009 100건
            maxDuration: '5m',
        },
    },
    thresholds: {
        // REQ-NF-KPI-015 K-O04 — Critical 1분 SLA 99% 준수
        sla_compliance: ['rate>0.99'],
        // p95 dispatch-to-ack < 60s
        notification_dispatch_to_ack_ms: ['p(95)<60000'],
        // HTTP 오류율 ≤ 1%
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://stg.intranet/api/v1';
const TOKEN    = __ENV.JWT_TOKEN || '';
const AUTH     = { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' };
const ACK_TIMEOUT_MS = 60_000;     // Critical SLA
const POLL_INTERVAL_S = 2;

// ---------- iteration 본체 ----------
export default function () {
    const dispatchedAt = Date.now();

    // 1) Critical 변경 트리거 — 테스트 전용 dispatch endpoint (Sprint 1 baseline 에서는
    //    실제 mock — Sprint 2 STG 환경에서 endpoint 활성)
    const payload = JSON.stringify({
        hose_id: `29673-PERF-${__ITER}`,
        delivery_date: '2026-02-15',
        severity: 'CRITICAL',
        change_summary: `qty 100 → 130 (+30%) iter=${__ITER}`,
    });

    const dispatchRes = http.post(
        `${BASE_URL}/orders/_test/dispatch-notification`,
        payload,
        { headers: AUTH },
    );

    const dispatchOk = check(dispatchRes, {
        'dispatch HTTP 202': r => r.status === 202,
        'notificationId 존재': r => r.json('notificationId') !== undefined,
    });

    if (!dispatchOk) {
        slaCompliance.add(0);
        return;
    }
    criticalDispatched.add(1);
    const notificationId = dispatchRes.json('notificationId');

    // 2) ack polling — acknowledged_at 채워질 때까지 (or 60s timeout)
    const deadline = dispatchedAt + ACK_TIMEOUT_MS;
    let acked = false;
    while (Date.now() < deadline) {
        const statusRes = http.get(
            `${BASE_URL}/notifications/${notificationId}`,
            { headers: AUTH },
        );

        if (statusRes.status === 200 && statusRes.json('acknowledgedAt')) {
            const elapsed = Date.now() - dispatchedAt;
            dispatchToAck.add(elapsed);
            slaCompliance.add(elapsed < ACK_TIMEOUT_MS ? 1 : 0);
            acked = true;
            break;
        }
        sleep(POLL_INTERVAL_S);
    }

    if (!acked) {
        slaCompliance.add(0);
        ackTimeouts.add(1);
    }
}

// ---------- 결과 요약 ----------
export function handleSummary(data) {
    return {
        'perf/reports/notification_sla_summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
