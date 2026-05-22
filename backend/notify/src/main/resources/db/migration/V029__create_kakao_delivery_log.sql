-- =============================================================================
-- V029__create_kakao_delivery_log.sql — TK-16-1-1 (EP-16 ST-16-1)
-- =============================================================================
-- 카카오톡 BizMessage 도달 추적 — 시도/성공/실패 + retry 횟수 영속.
-- REQ-FUNC-CO-008: 도달 상태 100% 기록.
-- =============================================================================

CREATE TABLE IF NOT EXISTS app.kakao_delivery_log (
    attempt_id        UUID         PRIMARY KEY,
    notification_id   UUID         NOT NULL,
    target_role       VARCHAR(40)  NOT NULL,
    message_preview   TEXT,
    attempt_no        SMALLINT     NOT NULL CHECK (attempt_no BETWEEN 1 AND 3),
    status            VARCHAR(10)  NOT NULL CHECK (status IN ('SUCCESS','FAILED','SKIPPED')),
    error_message     TEXT,
    attempted_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_kakao_log_notification
    ON app.kakao_delivery_log (notification_id, attempted_at DESC);
CREATE INDEX IF NOT EXISTS idx_kakao_log_status
    ON app.kakao_delivery_log (status, attempted_at DESC);

COMMENT ON TABLE app.kakao_delivery_log IS
    'EP-16 ST-16-1 카카오톡 BizMessage 도달 추적 (REQ-FUNC-CO-008)';
COMMENT ON COLUMN app.kakao_delivery_log.attempt_no IS
    'retry 시퀀스 1~3 (Resilience4j Retry max=3 정합)';
