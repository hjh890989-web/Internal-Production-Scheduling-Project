-- =============================================================================
-- V005__create_notification.sql — TK-03-3-2 (SAD §6.2.12 NOTIFICATION)
-- =============================================================================
-- 알림 도달 라이프사이클 + SLA 추적 (REQ-FUNC-OC-009·REQ-NF-KPI-015 K-O04).
--
-- 상태 흐름:
--   DISPATCHED (row 생성) → SENT (외부 호출 완료) → DELIVERED (응답 확인)
--                                                  → ACKNOWLEDGED (사용자 확인)
--                                                  → FAILED (실패 / timeout)
--
-- 인덱스:
--   - idx_notification_change          : change_id 검색 (OrderChange ↔ Notification join)
--   - idx_notification_status          : SLA 모니터링 + 미 ack 조회
--   - idx_notification_dispatched      : 시간순 조회 (대시보드)
--   - idx_notification_undelivered_critical : SLA 위반 부분 인덱스 (DeliveryEscalator hot path)
--
-- 보존: 3년 (REQ-NF-SEC-004) — Phase 2 monthly partition + WAL archiving.
-- =============================================================================

CREATE TABLE app.notification (
    notification_id   UUID PRIMARY KEY,
    order_change_id   UUID NOT NULL REFERENCES app.order_change(change_id),
    channel           VARCHAR(20) NOT NULL CHECK (channel IN ('IN_APP', 'KAKAOTALK', 'SLACK')),
    severity          VARCHAR(10) NOT NULL CHECK (severity IN ('CRITICAL', 'NORMAL')),
    target_role       VARCHAR(30) NOT NULL,
    hose_id           VARCHAR(40) NOT NULL,
    delivery_date     DATE NOT NULL,
    change_summary    TEXT,
    dispatched_at     TIMESTAMPTZ NOT NULL,
    sent_at           TIMESTAMPTZ,
    delivered_at      TIMESTAMPTZ,
    acknowledged_at   TIMESTAMPTZ,
    failed_at         TIMESTAMPTZ,
    error_message     TEXT,
    retry_count       INTEGER NOT NULL DEFAULT 0,
    status            VARCHAR(20) NOT NULL DEFAULT 'DISPATCHED'
                      CHECK (status IN ('DISPATCHED', 'SENT', 'DELIVERED', 'ACKNOWLEDGED', 'FAILED'))
);

CREATE INDEX idx_notification_change      ON app.notification(order_change_id);
CREATE INDEX idx_notification_status      ON app.notification(severity, status, dispatched_at);
CREATE INDEX idx_notification_dispatched  ON app.notification(dispatched_at DESC);

-- SLA 위반 hot path 부분 인덱스 — DeliveryEscalator 1분 주기 polling 최적화
CREATE INDEX idx_notification_undelivered_critical ON app.notification(dispatched_at)
WHERE severity = 'CRITICAL' AND acknowledged_at IS NULL AND failed_at IS NULL;

COMMENT ON TABLE app.notification IS 'EP-03 ST-03-3 — 알림 도달 라이프사이클 + SLA 추적 (REQ-FUNC-OC-009 / K-O04)';
COMMENT ON COLUMN app.notification.status IS 'DISPATCHED→SENT→DELIVERED→ACKNOWLEDGED|FAILED 상태머신';
COMMENT ON COLUMN app.notification.retry_count IS '실패 시 재시도 횟수 (Resilience4j Retry max 3 — Sprint 2 활성)';
