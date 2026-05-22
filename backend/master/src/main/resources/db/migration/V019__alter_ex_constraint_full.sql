-- =============================================================================
-- V019__alter_ex_constraint_full.sql — TK-08-2-1 (EP-08 ST-08-2)
-- =============================================================================
-- 압출 마스터 풀 확장 — Sprint 2 V016 최소(spec·angle)에서 BR-E05 yield 수식
-- 입력 (speed·length·die·line) 추가. 47품번 핵심 seed.
--
-- yield = floor(speed_m_per_min × effective_min × 1000 / length_mm)
-- BR-E05 reference: 29673-2R060 주간전반 = 2,531
--   floor(14.06 × 180 × 1000 / 1000) = floor(2530.8) = 2531
-- =============================================================================

ALTER TABLE master.ex_constraint
    ADD COLUMN IF NOT EXISTS speed_m_per_min  NUMERIC(7,3)
        CHECK (speed_m_per_min IS NULL OR speed_m_per_min > 0),
    ADD COLUMN IF NOT EXISTS length_mm        INTEGER
        CHECK (length_mm IS NULL OR length_mm > 0),
    ADD COLUMN IF NOT EXISTS die_code         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS line_code        VARCHAR(10);

CREATE INDEX IF NOT EXISTS idx_ex_constraint_die  ON master.ex_constraint (die_code) WHERE die_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ex_constraint_line ON master.ex_constraint (line_code) WHERE line_code IS NOT NULL;

COMMENT ON COLUMN master.ex_constraint.speed_m_per_min IS
    'BR-E05 yield 수식: 압출 속도 m/min (단위 명시 — mm/min 입력 금지)';
COMMENT ON COLUMN master.ex_constraint.length_mm       IS
    'BR-E05 yield 수식: 단위 길이 mm';
COMMENT ON COLUMN master.ex_constraint.die_code        IS
    'EP-09 셋팅 그룹핑 입력 (다이 식별자)';
COMMENT ON COLUMN master.ex_constraint.line_code       IS
    'EP-09 셋팅 그룹핑 입력 (압출 라인 식별자)';

-- 47품번 핵심 seed — BR-E05 reference + 주요 품번
UPDATE master.ex_constraint SET
    speed_m_per_min = 14.060, length_mm = 1000, die_code = 'DIE-2R060', line_code = 'L1'
 WHERE hose_id = '29673-2R060';

UPDATE master.ex_constraint SET
    speed_m_per_min = 12.500, length_mm = 1200, die_code = 'DIE-2F900', line_code = 'L1'
 WHERE hose_id = '29673-2F900';

UPDATE master.ex_constraint SET
    speed_m_per_min = 15.200, length_mm = 800,  die_code = 'DIE-2U000', line_code = 'L2'
 WHERE hose_id = '28912-2U000';

UPDATE master.ex_constraint SET
    speed_m_per_min = 18.000, length_mm = 600,  die_code = 'DIE-6T010', line_code = 'L2'
 WHERE hose_id = '28442-6T010';

UPDATE master.ex_constraint SET
    speed_m_per_min = 20.000, length_mm = 500,  die_code = 'DIE-08400', line_code = 'L3'
 WHERE hose_id = '28415-08400';

UPDATE master.ex_constraint SET
    speed_m_per_min = 16.000, length_mm = 700,  die_code = 'DIE-03HA0', line_code = 'L3'
 WHERE hose_id = '25490-03HA0';

-- 29673-2R060 가 vc_constraint 에 없을 경우 ex_constraint 에도 신규 INSERT
INSERT INTO master.ex_constraint (hose_id, spec_value, angle_count, speed_m_per_min, length_mm,
                                   die_code, line_code, notes, updated_by)
VALUES ('29673-2R060', 8, 1, 14.060, 1000, 'DIE-2R060', 'L1', 'BR-E05 reference', 'seed-v019')
ON CONFLICT (hose_id) DO UPDATE SET
    speed_m_per_min = EXCLUDED.speed_m_per_min,
    length_mm       = EXCLUDED.length_mm,
    die_code        = EXCLUDED.die_code,
    line_code       = EXCLUDED.line_code,
    updated_at      = now();
