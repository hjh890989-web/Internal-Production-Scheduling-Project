-- =============================================================================
-- V024__create_line_routing.sql — TK-14-1-1 (EP-14 ST-14-1)
-- =============================================================================
-- 신규 라인 우선 라우팅 정책 (NEW 90%↑) + 포드 fallback. 포드 전용 품번은 신규 시도
-- 0건 (호환성 제약, BR-E08).
-- =============================================================================

CREATE TABLE IF NOT EXISTS master.line_type (
    line_id      VARCHAR(10) PRIMARY KEY,
    line_type    VARCHAR(10) NOT NULL CHECK (line_type IN ('NEW','FORD')),
    priority     SMALLINT    NOT NULL CHECK (priority BETWEEN 1 AND 99),
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    description  TEXT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   VARCHAR(40) NOT NULL DEFAULT 'seed'
);

CREATE INDEX IF NOT EXISTS idx_line_type_priority
    ON master.line_type (line_type, priority) WHERE is_active = TRUE;

COMMENT ON TABLE master.line_type IS
    'EP-14 ST-14-1 압출 라인 타입 (NEW priority 90%↑, FORD fallback) — BR-E08';

-- 4 라인 seed
INSERT INTO master.line_type (line_id, line_type, priority, description, updated_by) VALUES
  ('L1',       'NEW',  1, 'EX-A 신규 라인 (1순위)',     'seed-v024'),
  ('L2',       'NEW',  2, 'EX-B 신규 라인 (2순위)',     'seed-v024'),
  ('L3',       'NEW',  3, 'EX-C 신규 라인 (3순위)',     'seed-v024'),
  ('L-FORD',   'FORD', 90, '포드 노후 라인 (fallback)', 'seed-v024')
ON CONFLICT (line_id) DO UPDATE SET
    line_type   = EXCLUDED.line_type,
    priority    = EXCLUDED.priority,
    is_active   = EXCLUDED.is_active,
    description = EXCLUDED.description,
    updated_at  = now(),
    updated_by  = EXCLUDED.updated_by;

-- =============================================================================
-- 포드 전용 품번 호환성 — line_id pinned (신규 시도 0건)
-- =============================================================================
CREATE TABLE IF NOT EXISTS master.line_product_compatibility (
    hose_id        VARCHAR(40)  NOT NULL,
    line_id        VARCHAR(10)  NOT NULL REFERENCES master.line_type(line_id),
    ford_only      BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     VARCHAR(40)  NOT NULL DEFAULT 'seed',
    PRIMARY KEY (hose_id, line_id)
);

CREATE INDEX IF NOT EXISTS idx_line_product_ford
    ON master.line_product_compatibility (hose_id) WHERE ford_only = TRUE;

COMMENT ON TABLE master.line_product_compatibility IS
    'EP-14 포드 전용 품번 (호환성 제약 — 신규 시도 차단)';

-- 포드 전용 품번 seed (예시 — 운영팀 정합 후 확장)
INSERT INTO master.line_product_compatibility (hose_id, line_id, ford_only, updated_by) VALUES
  ('25490-03HA0', 'L-FORD', TRUE, 'seed-v024'),
  ('28415-08400', 'L-FORD', TRUE, 'seed-v024')
ON CONFLICT (hose_id, line_id) DO UPDATE SET
    ford_only  = EXCLUDED.ford_only,
    updated_at = now(),
    updated_by = EXCLUDED.updated_by;
