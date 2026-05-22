-- =============================================================================
-- V016__create_ex_constraint_and_product_spec_view.sql — TK-21-5-1 (EP-21 ST-21-5)
-- =============================================================================
-- 압출(EX) 마스터 최소 신규 — 규격값(spec) + 앵글 수(angle_count) 만.
-- 본 마이그레이션은 Sprint 2 의 BR-V17 (규격<7 가류기당 ≤4) 한정 진입 —
-- Sprint 3 EP-07 압출에서 EX_CONSTRAINT 풀 확장 (압출 속도·다이·라인 등).
--
-- ADR-017 cross-master VIEW 패턴: v_product_with_spec = VC + EX JOIN.
-- LISTEN/NOTIFY 트리거로 캐시 무효화 (TK-21-5-2).
-- =============================================================================

CREATE TABLE IF NOT EXISTS master.ex_constraint (
    hose_id      VARCHAR(40) PRIMARY KEY,
    spec_value   INTEGER,                               -- 압출공정_제약조건.xlsx B열 규격값
    angle_count  INTEGER     NOT NULL DEFAULT 1
        CHECK (angle_count BETWEEN 1 AND 99),
    notes        TEXT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   VARCHAR(40) NOT NULL DEFAULT 'seed'
);

CREATE INDEX IF NOT EXISTS idx_ex_constraint_spec_lt7
    ON master.ex_constraint (hose_id) WHERE spec_value < 7;

COMMENT ON TABLE  master.ex_constraint              IS
    'EP-21 ST-21-5 압출 마스터 최소 (BR-V17, Sprint 3 EP-07 에서 풀 확장)';
COMMENT ON COLUMN master.ex_constraint.spec_value   IS
    'B열 규격값 — < 7 시 가류기당 ≤4 앵글 제약 (BR-V17)';
COMMENT ON COLUMN master.ex_constraint.angle_count  IS
    '슬롯당 점유 앵글 수 — spec<7 누계 검증';

-- LISTEN/NOTIFY 트리거 — TK-21-5-2 캐시 무효화
CREATE OR REPLACE FUNCTION master.notify_ex_constraint_change()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('ex_constraint_changed',
        COALESCE(NEW.hose_id, OLD.hose_id));
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ex_constraint_notify ON master.ex_constraint;
CREATE TRIGGER trg_ex_constraint_notify
    AFTER INSERT OR UPDATE OR DELETE ON master.ex_constraint
    FOR EACH ROW EXECUTE FUNCTION master.notify_ex_constraint_change();

-- =============================================================================
-- v_product_with_spec VIEW — ADR-017 cross-master
-- =============================================================================
CREATE OR REPLACE VIEW master.v_product_with_spec AS
SELECT
    vc.hose_id,
    ex.spec_value                    AS spec,
    vc.composite_count               AS composite_count,
    vc.lp_left_setting               AS lp_left_setting,
    vc.lp_right_setting              AS lp_right_setting,
    COALESCE(ex.angle_count, 1)      AS angle_count,
    (ex.spec_value IS NOT NULL AND ex.spec_value < 7) AS is_spec_lt7
FROM master.vc_constraint vc
LEFT JOIN master.ex_constraint ex
    ON vc.hose_id = ex.hose_id;

COMMENT ON VIEW master.v_product_with_spec IS
    'EP-21 ADR-017 cross-master: VC + EX 결합. BR-V17 spec<7 검증용.';

-- =============================================================================
-- 47품번 seed — spec<7 (BR-V17 활성) 핵심 + 안전한 default
-- =============================================================================
-- v1.4 마스터 분석 결과 (가정 — ST-99-2 export 기반) spec<7 품번 예시 3건 +
-- 그 외 47품번 default spec=7 (룰 미적용, 안전)
INSERT INTO master.ex_constraint (hose_id, spec_value, angle_count, notes, updated_by) VALUES
  -- spec<7 핵심 (BR-V17 적용)
  ('28442-6T010', 6, 2, 'BR-V17 spec<7 예시',     'seed-v016'),
  ('28415-08400', 5, 1, 'BR-V17 spec<7 예시',     'seed-v016'),
  ('25490-03HA0', 6, 2, 'BR-V17 spec<7 예시',     'seed-v016'),
  -- spec=7+ (룰 미적용)
  ('29673-2F900', 8, 1, 'spec≥7 룰 미적용',       'seed-v016'),
  ('28421-2M800', 9, 2, 'spec≥7 룰 미적용',       'seed-v016'),
  ('28422-2M800', 9, 2, 'spec≥7 룰 미적용',       'seed-v016'),
  ('28422-08HA0', 8, 1, 'spec≥7 룰 미적용',       'seed-v016')
ON CONFLICT (hose_id) DO UPDATE SET
    spec_value  = EXCLUDED.spec_value,
    angle_count = EXCLUDED.angle_count,
    notes       = EXCLUDED.notes,
    updated_at  = now(),
    updated_by  = EXCLUDED.updated_by;
