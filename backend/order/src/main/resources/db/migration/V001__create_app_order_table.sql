-- =============================================================================
-- V001__create_app_order_table.sql — Order 마스터 테이블 생성 (TK-02-1-1)
-- =============================================================================
-- SAD §6.1.1a 데이터 의미 기반 schema 분리 — app 스키마 (운영 데이터).
-- SRS §6.2.4 ORDER 엔티티 정합.
-- BR-X04 — timestamp KST (postgresql.conf timezone=Asia/Seoul).
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;

CREATE TABLE IF NOT EXISTS app."order" (
    order_id        UUID         PRIMARY KEY,
    hose_id         VARCHAR(40)  NOT NULL,
    delivery_date   DATE         NOT NULL,
    qty             INTEGER      NOT NULL CHECK (qty >= 1),
    order_type      VARCHAR(20)  NOT NULL CHECK (order_type IN ('FORECAST','WEEKLY','KD','CONFIRMED')),
    customer        VARCHAR(100) NOT NULL,
    master_version  INTEGER      NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 빈번 조회 인덱스 — TK-02-1-3 batch query (hose_id, delivery_date) + status='ACTIVE' 필터
CREATE INDEX IF NOT EXISTS idx_order_hose_delivery
    ON app."order" (hose_id, delivery_date)
    WHERE status = 'ACTIVE';

-- master_version 별 조회 보조 인덱스
CREATE INDEX IF NOT EXISTS idx_order_master_version
    ON app."order" (master_version);

COMMENT ON TABLE  app."order"               IS 'SRS §6.2.4 ORDER 마스터 — TK-02-1-1';
COMMENT ON COLUMN app."order".order_id      IS 'UUID 발급 (commit 시점)';
COMMENT ON COLUMN app."order".hose_id       IS '품번 — 47품번 마스터 (master.PRODUCT.hose_id) FK 후보 (Sprint 1+)';
COMMENT ON COLUMN app."order".delivery_date IS '납기 — KST 일자 (BR-X04)';
COMMENT ON COLUMN app."order".master_version IS '마스터 버전 — 같은 (hose_id, delivery_date) 도 다른 version 허용 (진화)';
COMMENT ON COLUMN app."order".status        IS 'ACTIVE / ARCHIVED / DELETED (soft-delete)';
