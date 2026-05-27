-- =============================================================================
-- V044__create_mes_shift_event.sql — Sprint 17 EP-DAY-LOCK TK-DAY-LOCK-3-3
-- =============================================================================
-- BR-X06 MES 폴백 baseline — MES 실적 (또는 Excel 폴백) shift 단위 적재.
--
-- 정합 (BR/REQ):
--   REQ-FUNC-CO-004 — 1 shift 이상 MES 미수신 → 직전 계획값 임시 + 재개 시 재조정
--   REQ-NF-REL-004 — MES 장애 1 shift 이후 다음 shift 내 자동 재조정
--   SRS-RSK-006 — MES 실적 지연 시 잔여 수량 추정 오염 위험 (중간)
--
-- Sprint 17 baseline 은 stub interface — 실 MES 연동은 Phase 5+ carry-over.
-- 본 테이블이 MES 수신 / Excel 폴백 모두 기록하는 SSoT (source 컬럼으로 구분).
-- =============================================================================

CREATE TABLE app.mes_shift_event (
    shift_event_id  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    machine_id      VARCHAR(10)  NOT NULL,                -- LP-01~04, IC-01
    shift_date      DATE         NOT NULL,
    shift_no        SMALLINT     NOT NULL CHECK (shift_no BETWEEN 1 AND 4),
    -- shift_no: 1=주간 전반, 2=주간 후반, 3=야간 전반, 4=야간 후반 (4 shift × 6h day; PDD BR-V04 정합)
    planned_qty     INTEGER      NOT NULL CHECK (planned_qty >= 0),
    actual_qty      INTEGER      CHECK (actual_qty IS NULL OR actual_qty >= 0),
    received_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    source          VARCHAR(20)  NOT NULL
                                 CHECK (source IN ('MES', 'EXCEL_FALLBACK')),
    reported_by     VARCHAR(40),                          -- Excel 폴백 시 actor (사번)
    note            VARCHAR(500),
    CONSTRAINT uq_mes_shift_event_machine_date_no
        UNIQUE (machine_id, shift_date, shift_no)
);

COMMENT ON TABLE  app.mes_shift_event       IS
    'Sprint 17 BR-X06 MES 실적 / Excel 폴백 shift 적재 SSoT';
COMMENT ON COLUMN app.mes_shift_event.source IS
    'MES = 자동 수신 / EXCEL_FALLBACK = PLANNER 또는 IT_OPS 수동 입력';
COMMENT ON COLUMN app.mes_shift_event.shift_no IS
    '1=주간 전반, 2=주간 후반, 3=야간 전반, 4=야간 후반';

CREATE INDEX idx_mes_shift_event_machine_received
    ON app.mes_shift_event (machine_id, received_at DESC);

CREATE INDEX idx_mes_shift_event_source
    ON app.mes_shift_event (source) WHERE source = 'EXCEL_FALLBACK';

-- =============================================================================
-- BR-X02 audit trigger — Excel 폴백 INSERT 시 actor (reported_by) 캡쳐
-- =============================================================================
CREATE OR REPLACE FUNCTION app.audit_mes_shift_event()
RETURNS TRIGGER AS $$
DECLARE
    v_actor TEXT;
BEGIN
    -- @Auditable AOP 가 set_config('audit.actor', planner_id) 사전 호출
    v_actor := COALESCE(NEW.reported_by, current_setting('audit.actor', true), 'system');
    INSERT INTO audit.schedule_audit_log (table_name, row_pk, action, actor, reason)
    VALUES (
        'mes_shift_event',
        NEW.shift_event_id::TEXT,
        TG_OP,
        v_actor,
        format('source=%s machine=%s date=%s shift=%s qty=%s',
            NEW.source, NEW.machine_id, NEW.shift_date, NEW.shift_no, NEW.actual_qty)
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_mes_shift_event ON app.mes_shift_event;
CREATE TRIGGER trg_audit_mes_shift_event
    AFTER INSERT OR UPDATE ON app.mes_shift_event
    FOR EACH ROW EXECUTE FUNCTION app.audit_mes_shift_event();
