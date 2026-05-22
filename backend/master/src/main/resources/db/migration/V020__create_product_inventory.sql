-- =============================================================================
-- V020__create_product_inventory.sql — TK-08-3-1 (EP-08 ST-08-3)
-- =============================================================================
-- 압출 Q_ext 계산 입력 — target_stock (안전재고) + current_stock (현재고).
-- Q_ext = max(0, Q_vc + target - current).
--
-- 운영: MES 동기화 (Phase 2+ — 현재는 IT_OPS 수동 갱신 또는 cron job).
-- =============================================================================

CREATE TABLE IF NOT EXISTS master.product_inventory (
    hose_id        VARCHAR(40) PRIMARY KEY,
    target_stock   INTEGER     NOT NULL DEFAULT 0 CHECK (target_stock  >= 0),
    current_stock  INTEGER     NOT NULL DEFAULT 0 CHECK (current_stock >= 0),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by     VARCHAR(40) NOT NULL DEFAULT 'seed'
);

CREATE INDEX IF NOT EXISTS idx_inventory_shortage
    ON master.product_inventory (hose_id) WHERE current_stock < target_stock;

COMMENT ON TABLE  master.product_inventory               IS
    'EP-08 ST-08-3 압출 Q_ext = max(0, Q_vc + target - current)';
COMMENT ON COLUMN master.product_inventory.target_stock  IS
    '안전재고 (목표 stock)';
COMMENT ON COLUMN master.product_inventory.current_stock IS
    '현재고 (MES 동기화 또는 IT_OPS 수동 갱신)';

-- LISTEN/NOTIFY 트리거 — Sprint 4 MES 동기화 시 캐시 무효화
CREATE OR REPLACE FUNCTION master.notify_inventory_change()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('product_inventory_changed', COALESCE(NEW.hose_id, OLD.hose_id));
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_inventory_notify ON master.product_inventory;
CREATE TRIGGER trg_inventory_notify
    AFTER INSERT OR UPDATE OR DELETE ON master.product_inventory
    FOR EACH ROW EXECUTE FUNCTION master.notify_inventory_change();

-- 47품번 핵심 seed (4 케이스 — 충분/target 도달/부족/0)
INSERT INTO master.product_inventory (hose_id, target_stock, current_stock, updated_by) VALUES
  ('29673-2R060', 500, 100,  'seed-v020'),  -- 부족 (-400 → Q_ext = Q_vc + 400)
  ('29673-2F900', 300, 300,  'seed-v020'),  -- target 도달 (Q_ext = Q_vc)
  ('28912-2U000', 200, 250,  'seed-v020'),  -- 충분 (+50 → Q_ext = max(0, Q_vc − 50))
  ('28442-6T010', 100, 50,   'seed-v020'),  -- 약간 부족 (Q_ext = Q_vc + 50)
  ('28415-08400', 0,   0,    'seed-v020'),  -- 모두 0 (Q_ext = Q_vc)
  ('25490-03HA0', 150, 150,  'seed-v020')   -- target 도달
ON CONFLICT (hose_id) DO UPDATE SET
    target_stock  = EXCLUDED.target_stock,
    current_stock = EXCLUDED.current_stock,
    updated_at    = now(),
    updated_by    = EXCLUDED.updated_by;
