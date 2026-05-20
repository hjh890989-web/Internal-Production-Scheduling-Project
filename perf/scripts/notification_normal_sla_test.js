// =============================================================================
// perf/scripts/notification_normal_sla_test.js — TK-03-3-3 부속
// =============================================================================
// Normal 알림 ≤ 5분 SLA 100건 시뮬 — REQ-FUNC-OC-009 / K-O04 보조 검증.
// 인앱 WebSocket 만 발송 (카카오톡 skip) — Critical 보다 부하 낮음.
//
// 실행:
//   k6 run \
//     -e BASE_URL=http://stg.intranet/api/v1 \
//     -e JWT_TOKEN=$STG_JWT_PLANNER \
//     perf/scripts/notification_normal_sla_test.js
// =============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const dispatchToAck = new Trend('normal_dispatch_to_ack_ms', true);
const normalSlaCompliance = new Rate('normal_sla_compliance');
const dispatched = new Counter('normal_dispatched_total');

export const options = {
    scenarios: {
        normal_burst: {
            executor: 'shared-iterations',
            vus: 10,
            iterations: 100,
            maxDuration: '15m',
        },
    },
    thresholds: {
        normal_sla_compliance: ['rate>0.99'],            // ≥99% 5분 내 ack
        normal_dispatch_to_ack_ms: ['p(95)<300000'],     // p95 < 5분
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://stg.intranet/api/v1';
const TOKEN = __ENV.JWT_TOKEN || '';
const AUTH  = { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' };
const SLA_MS = 300_000;     // Normal 5분
const POLL_S = 5;

export default function () {
    const dispatchedAt = Date.now();
    const payload = JSON.stringify({
        hose_id: `29673-NORM-${__ITER}`,
        delivery_date: '2026-02-15',
        severity: 'NORMAL',
        change_summary: `customer changed iter=${__ITER}`,
    });

    const res = http.post(
        `${BASE_URL}/orders/_test/dispatch-notification`,
        payload,
        { headers: AUTH },
    );

    if (!check(res, { 'dispatch 202': r => r.status === 202 })) {
        normalSlaCompliance.add(0);
        return;
    }
    dispatched.add(1);
    const notificationId = res.json('notificationId');
    const deadline = dispatchedAt + SLA_MS;

    while (Date.now() < deadline) {
        const status = http.get(`${BASE_URL}/notifications/${notificationId}`, { headers: AUTH });
        if (status.status === 200 && status.json('acknowledgedAt')) {
            const elapsed = Date.now() - dispatchedAt;
            dispatchToAck.add(elapsed);
            normalSlaCompliance.add(elapsed < SLA_MS ? 1 : 0);
            return;
        }
        sleep(POLL_S);
    }
    normalSlaCompliance.add(0);
}

export function handleSummary(data) {
    return {
        'perf/reports/notification_normal_sla_summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
