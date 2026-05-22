-- =============================================================================
-- V017__create_ex_schedule_candidate.sql — TK-07-1-2 (EP-07 ST-07-1)
-- =============================================================================
-- 압출 후보 스케줄 — VC 확정 이벤트 수신 시 1:1 매핑 row 생성.
-- BR-E01 hard 제약: extrusion_deadline = vc_production_date − 1 working day.
-- EP-08 (수식) / EP-09 (그룹핑) 진입 전 candidate 단계.
-- =============================================================================

CREATE TABLE IF NOT EXISTS app.ex_schedule_candidate (
    ex_candidate_id        UUID         PRIMARY KEY,
    schedule_id            UUID         NOT NULL,                -- VC batch scheduleId
    hose_id                VARCHAR(40)  NOT NULL,
    vc_row_id              UUID         NOT NULL,                -- VcSchedule PK 추적
    vc_production_date     DATE         NOT NULL,                -- 성형 투입일
    extrusion_deadline     DATE         NOT NULL,                -- BR-E01: vc_date − 1 working day
    vc_yield               INTEGER      NOT NULL CHECK (vc_yield >= 0),  -- EP-08 Q_ext 입력
    status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','READY','SCHEDULED','CONFIRMED','FAILED')),
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL
);

-- 1:1 vc_row_id — UNIQUE (재발행 idempotency 가드)
CREATE UNIQUE INDEX IF NOT EXISTS uq_ex_candidate_vc_row
    ON app.ex_schedule_candidate (vc_row_id);

CREATE INDEX IF NOT EXISTS idx_ex_candidate_schedule
    ON app.ex_schedule_candidate (schedule_id);

CREATE INDEX IF NOT EXISTS idx_ex_candidate_deadline
    ON app.ex_schedule_candidate (extrusion_deadline);

CREATE INDEX IF NOT EXISTS idx_ex_candidate_status
    ON app.ex_schedule_candidate (status);

COMMENT ON TABLE  app.ex_schedule_candidate                     IS
    'EP-07 ST-07-1 압출 후보 (VC 확정 → D-1 역산 1:1 매핑)';
COMMENT ON COLUMN app.ex_schedule_candidate.extrusion_deadline  IS
    'BR-E01: vc_production_date − 1 working day (EP-06 WorkingCalendar 적용)';
COMMENT ON COLUMN app.ex_schedule_candidate.vc_yield            IS
    '성형 회전당 yield — EP-08 Q_ext = max(0, Q_vc + target − current) 입력';
COMMENT ON COLUMN app.ex_schedule_candidate.status              IS
    'PENDING(EP-07 생성) → READY(EP-08 yield 계산) → SCHEDULED(EP-09 그룹핑) → CONFIRMED/FAILED';
