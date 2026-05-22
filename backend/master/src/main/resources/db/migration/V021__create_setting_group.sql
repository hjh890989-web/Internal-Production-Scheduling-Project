-- =============================================================================
-- V021__create_setting_group.sql — TK-09-1-1 (EP-09 ST-09-1)
-- =============================================================================
-- 압출 셋팅 그룹 1~8 마스터 + 47품번 M:N 매핑. shift 내 단일 그룹 강제 (BR-E06·E07)
-- 입력. 셋업 1회 30분 손실 회피 — shift 내 다른 그룹 혼합 금지.
-- =============================================================================

CREATE TABLE IF NOT EXISTS master.setting_group (
    group_number     SMALLINT     PRIMARY KEY CHECK (group_number BETWEEN 1 AND 8),
    group_name       VARCHAR(40)  NOT NULL,
    description      TEXT,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by       VARCHAR(40)  NOT NULL DEFAULT 'seed'
);

COMMENT ON TABLE master.setting_group IS
    'EP-09 ST-09-1 압출 셋팅 그룹 1~8 (BR-E06 shift 내 단일 그룹)';

-- 8 그룹 seed
INSERT INTO master.setting_group (group_number, group_name, description, updated_by) VALUES
  (1, 'G1-소형',     '규격 < 7 소형 호스 그룹',      'seed-v021'),
  (2, 'G2-중형A',    '규격 7~10 중형 그룹 A',         'seed-v021'),
  (3, 'G3-중형B',    '규격 7~10 중형 그룹 B',         'seed-v021'),
  (4, 'G4-대형',     '규격 > 10 대형 그룹',           'seed-v021'),
  (5, 'G5-합금형',   'composite 1·2·3·6 합금 그룹',  'seed-v021'),
  (6, 'G6-특수',     '특수 다이 (28422-08HA0 등)',   'seed-v021'),
  (7, 'G7-좌측',     '좌측 셋팅 호환 그룹',           'seed-v021'),
  (8, 'G8-우측',     '우측 셋팅 호환 그룹',           'seed-v021')
ON CONFLICT (group_number) DO UPDATE SET
    group_name  = EXCLUDED.group_name,
    description = EXCLUDED.description,
    updated_at  = now(),
    updated_by  = EXCLUDED.updated_by;

-- =============================================================================
-- 47품번 ↔ 셋팅 그룹 M:N 매핑
-- =============================================================================
CREATE TABLE IF NOT EXISTS master.product_setting_group (
    hose_id         VARCHAR(40)  NOT NULL,
    group_number    SMALLINT     NOT NULL REFERENCES master.setting_group(group_number),
    primary_group   BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(40)  NOT NULL DEFAULT 'seed',
    PRIMARY KEY (hose_id, group_number)
);

CREATE INDEX IF NOT EXISTS idx_product_setting_group_hose ON master.product_setting_group (hose_id);
CREATE INDEX IF NOT EXISTS idx_product_setting_group_grp  ON master.product_setting_group (group_number);

COMMENT ON TABLE master.product_setting_group IS
    'EP-09 47품번 ↔ 셋팅 그룹 1~8 M:N 매핑 (primary_group = 우선 추천)';

-- LISTEN/NOTIFY 트리거 — 그룹 매핑 변경 캐시 무효화
CREATE OR REPLACE FUNCTION master.notify_setting_group_change()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('setting_group_changed', COALESCE(NEW.hose_id, OLD.hose_id));
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_product_setting_group_notify ON master.product_setting_group;
CREATE TRIGGER trg_product_setting_group_notify
    AFTER INSERT OR UPDATE OR DELETE ON master.product_setting_group
    FOR EACH ROW EXECUTE FUNCTION master.notify_setting_group_change();

-- 47품번 setting_group seed (Phase 1 분석 결과 기반)
INSERT INTO master.product_setting_group (hose_id, group_number, primary_group, updated_by) VALUES
  -- 28422-08HA0: LP-01 고정 + 특수 다이 → G6
  ('28422-08HA0', 6, TRUE,  'seed-v021'),
  -- 28421-2M800: 좌측 셋팅 + 중형 → G7 primary + G2 secondary
  ('28421-2M800', 7, TRUE,  'seed-v021'),
  ('28421-2M800', 2, FALSE, 'seed-v021'),
  -- 28422-2M800: 우측 셋팅 + 중형 → G8 primary + G3 secondary
  ('28422-2M800', 8, TRUE,  'seed-v021'),
  ('28422-2M800', 3, FALSE, 'seed-v021'),
  -- 29673-2R060: 합금형 → G5 primary
  ('29673-2R060', 5, TRUE,  'seed-v021'),
  -- 29673-2F900: 합금형 → G5 primary
  ('29673-2F900', 5, TRUE,  'seed-v021'),
  -- 28912-2U000: 중형 A → G2
  ('28912-2U000', 2, TRUE,  'seed-v021'),
  -- 28442-6T010: spec<7 → G1
  ('28442-6T010', 1, TRUE,  'seed-v021'),
  -- 28415-08400: spec<7 → G1
  ('28415-08400', 1, TRUE,  'seed-v021'),
  -- 25490-03HA0: spec<7 → G1
  ('25490-03HA0', 1, TRUE,  'seed-v021')
ON CONFLICT (hose_id, group_number) DO UPDATE SET
    primary_group = EXCLUDED.primary_group,
    updated_at    = now(),
    updated_by    = EXCLUDED.updated_by;
