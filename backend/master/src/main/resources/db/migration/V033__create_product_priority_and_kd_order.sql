-- =============================================================================
-- V033__create_product_priority_and_kd_order.sql — Sprint 7 carry-over
-- =============================================================================
-- BR-V12 (capa 초과 시 PRODUCT_PRIORITY 추가요청 큐) +
-- BR-V13 (capa 부족 시 KD_ORDER 잔량 보충) — REQ-FUNC-VC-022·023.
-- 활성 조건: 수주정보 통합 작업 완료 후 (PDD-02 v1.1 deferred).
-- =============================================================================

-- =============================================================================
-- 1) master.product_priority — BR-V12 capa 초과 시 우선순위 큐 입력 (DI-07)
-- =============================================================================
CREATE TABLE IF NOT EXISTS master.product_priority (
    hose_id        VARCHAR(40)  PRIMARY KEY,
    priority_rank  SMALLINT     NOT NULL CHECK (priority_rank BETWEEN 1 AND 99),
    rationale      TEXT,                                  -- 사유 (예: 'VIP 고객', '긴급 LOT')
    effective_from DATE         NOT NULL DEFAULT CURRENT_DATE,
    effective_to   DATE,                                  -- NULL = 무기한
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     VARCHAR(40)  NOT NULL DEFAULT 'seed',

    CONSTRAINT chk_priority_effective_range
        CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

-- partial index predicate IMMUTABLE 요건 — CURRENT_DATE 비-IMMUTABLE → 단순 복합 인덱스
CREATE INDEX IF NOT EXISTS idx_product_priority_rank
    ON master.product_priority (priority_rank, effective_from, effective_to);

COMMENT ON TABLE master.product_priority IS
    'BR-V12 capa 초과 시 우선순위 큐 (Sprint 7, REQ-FUNC-VC-022) — 수주통합 후 활성';
COMMENT ON COLUMN master.product_priority.priority_rank IS
    '1=최우선, 99=후순위. CapacityOverflowQueueService 가 ASC 정렬 채택';

-- =============================================================================
-- 2) master.kd_order — BR-V13 capa 부족 시 KD 발주 잔량 (DI-08)
-- =============================================================================
CREATE TABLE IF NOT EXISTS master.kd_order (
    kd_order_id     UUID         PRIMARY KEY,
    hose_id         VARCHAR(40)  NOT NULL,
    order_qty       INTEGER      NOT NULL CHECK (order_qty > 0),
    remaining_qty   INTEGER      NOT NULL CHECK (remaining_qty >= 0),
    order_date      DATE         NOT NULL,
    customer_code   VARCHAR(40),
    status          VARCHAR(10)  NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN','PARTIAL','FILLED','CANCELLED')),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(40)  NOT NULL DEFAULT 'seed',

    CONSTRAINT chk_kd_remaining_le_order
        CHECK (remaining_qty <= order_qty)
);

CREATE INDEX IF NOT EXISTS idx_kd_order_open
    ON master.kd_order (hose_id, order_date)
    WHERE status IN ('OPEN','PARTIAL');
CREATE INDEX IF NOT EXISTS idx_kd_order_status
    ON master.kd_order (status, updated_at DESC);

COMMENT ON TABLE master.kd_order IS
    'BR-V13 KD 발주 잔량 (Sprint 7, REQ-FUNC-VC-023) — 수주통합 후 활성. KdSupplementService 가 동일 hose → 동일 셋팅 그룹 순 참조';
COMMENT ON COLUMN master.kd_order.status IS
    'OPEN=잔량 100%, PARTIAL=일부 소진, FILLED=잔량 0 → status auto-update by supplement';
