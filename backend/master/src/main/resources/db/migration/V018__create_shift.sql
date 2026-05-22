-- =============================================================================
-- V018__create_shift.sql — TK-08-1-1 (EP-08 ST-08-1)
-- =============================================================================
-- 압출 4-shift 마스터 — 코드 상수 외재화. shift 추가/제거·효율 조정 시 코드
-- 재배포 없이 운영 (CON-02 빈도 갱신 대비).
--
-- BR-E03 — 4 shift (주간전반·후반·야간전반·후반)
-- BR-E04 — 75% 효율 (240 min × 0.75 = 180 min effective)
-- =============================================================================

CREATE TABLE IF NOT EXISTS master.shift (
    shift_code      VARCHAR(20) PRIMARY KEY,
    name            VARCHAR(40)   NOT NULL,
    start_time      TIME          NOT NULL,
    end_time        TIME          NOT NULL,
    nominal_min     INTEGER       NOT NULL CHECK (nominal_min > 0),
    efficiency      NUMERIC(4,3)  NOT NULL CHECK (efficiency > 0 AND efficiency <= 1),
    effective_min   INTEGER       GENERATED ALWAYS AS (FLOOR(nominal_min * efficiency)) STORED,
    sort_order      SMALLINT      NOT NULL CHECK (sort_order BETWEEN 1 AND 99),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by      VARCHAR(40)   NOT NULL DEFAULT 'seed'
);

CREATE INDEX IF NOT EXISTS idx_shift_sort ON master.shift (sort_order);

COMMENT ON TABLE  master.shift              IS
    'EP-08 ST-08-1 압출 4-shift 마스터 (BR-E03·E04)';
COMMENT ON COLUMN master.shift.efficiency   IS
    'BR-E04 효율 (default 0.75) — line별 차별화 시 IT_OPS 갱신';
COMMENT ON COLUMN master.shift.effective_min IS
    'GENERATED ALWAYS AS FLOOR(nominal_min × efficiency) STORED — yield 수식 입력';

-- LISTEN/NOTIFY 트리거 — Shift 캐시 무효화 (TK-21-2-3 패턴 재사용)
CREATE OR REPLACE FUNCTION master.notify_shift_change()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('shift_changed', COALESCE(NEW.shift_code, OLD.shift_code));
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_shift_notify ON master.shift;
CREATE TRIGGER trg_shift_notify
    AFTER INSERT OR UPDATE OR DELETE ON master.shift
    FOR EACH ROW EXECUTE FUNCTION master.notify_shift_change();

-- 4-shift seed (BR-E03)
INSERT INTO master.shift (shift_code, name, start_time, end_time, nominal_min, efficiency, sort_order, updated_by) VALUES
  ('DAY_EARLY',   '주간전반', TIME '08:00', TIME '12:00', 240, 0.75, 1, 'seed-v018'),
  ('DAY_LATE',    '주간후반', TIME '13:00', TIME '17:00', 240, 0.75, 2, 'seed-v018'),
  ('NIGHT_EARLY', '야간전반', TIME '20:00', TIME '00:00', 240, 0.75, 3, 'seed-v018'),
  ('NIGHT_LATE',  '야간후반', TIME '01:00', TIME '05:00', 240, 0.75, 4, 'seed-v018')
ON CONFLICT (shift_code) DO UPDATE SET
    name        = EXCLUDED.name,
    start_time  = EXCLUDED.start_time,
    end_time    = EXCLUDED.end_time,
    nominal_min = EXCLUDED.nominal_min,
    efficiency  = EXCLUDED.efficiency,
    sort_order  = EXCLUDED.sort_order,
    updated_at  = now(),
    updated_by  = EXCLUDED.updated_by;
