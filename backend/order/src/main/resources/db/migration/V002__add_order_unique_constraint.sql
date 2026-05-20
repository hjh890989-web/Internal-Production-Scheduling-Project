-- =============================================================================
-- V002__add_order_unique_constraint.sql — Order 중복 차단 UNIQUE (TK-02-1-1)
-- =============================================================================
-- REQ-FUNC-OC-005 — (hose_id, delivery_date, master_version) 복합 UNIQUE.
-- Order entity 의 @Table(uniqueConstraints) 와 동일 이름.
-- =============================================================================

ALTER TABLE app."order"
    ADD CONSTRAINT uq_order_hose_delivery_version
    UNIQUE (hose_id, delivery_date, master_version);

COMMENT ON CONSTRAINT uq_order_hose_delivery_version ON app."order"
    IS 'REQ-FUNC-OC-005 — (품번, 납기, 버전) 중복 차단. 같은 (hose, delivery) 도 새 master_version 으로 진화 허용.';

ANALYZE app."order";
