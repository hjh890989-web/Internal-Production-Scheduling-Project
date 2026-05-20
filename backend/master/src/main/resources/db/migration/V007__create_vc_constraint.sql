-- =============================================================================
-- V007__create_vc_constraint.sql — TK-04-1-1 (SAD §6.2.2)
-- =============================================================================
-- EP-04 ST-04-1 — 47품번 성형 슬롯 적합성 (REF-09 G~J·M~O 컬럼 매핑).
--
-- schema: master (마스터 데이터 — PDD v1.7 ADR-010 정본).
-- FK master.product(hose_id) 는 후속 V008+ master.product 도입 후 별도 추가.
--
-- 슬롯 구성:
--   - 저압 가류기 (LP) 4 슬롯: TOP / UPMID / LOWMID / BOT
--   - IC  가류기 (IC) 3 슬롯: TOP / MID / BOT
--   - 총 7 BOOLEAN O/X 컬럼 → SlotPosition enum 7 값
--
-- composite_count CHECK (1, 2, 3, 6) — REF-11 도메인 명시 (합금형 1·2·3·6).
--
-- LISTEN/NOTIFY 트리거 — TK-04-1-2 캐시 무효화 (ADR-017).
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS master;

CREATE TABLE IF NOT EXISTS master.vc_constraint (
    hose_id              VARCHAR(40) PRIMARY KEY,
    mold_qty             INTEGER     NOT NULL CHECK (mold_qty >= 0),
    composite_count      SMALLINT    NOT NULL CHECK (composite_count IN (1, 2, 3, 6)),

    -- 저압 가류기 (LP) 4 슬롯 — REF-09 G·H·I·J
    lp_molds_per_angle   SMALLINT    CHECK (lp_molds_per_angle IS NULL OR lp_molds_per_angle > 0),
    lp_angle_qty         SMALLINT    CHECK (lp_angle_qty IS NULL OR lp_angle_qty >= 0),
    lp_slot_top          BOOLEAN     NOT NULL,
    lp_slot_upmid        BOOLEAN     NOT NULL,
    lp_slot_lowmid       BOOLEAN     NOT NULL,
    lp_slot_bot          BOOLEAN     NOT NULL,

    -- IC 가류기 3 슬롯 — REF-09 M·N·O
    ic_molds_per_angle   SMALLINT    CHECK (ic_molds_per_angle IS NULL OR ic_molds_per_angle > 0),
    ic_angle_qty         SMALLINT    CHECK (ic_angle_qty IS NULL OR ic_angle_qty >= 0),
    ic_slot_top          BOOLEAN     NOT NULL,
    ic_slot_mid          BOOLEAN     NOT NULL,
    ic_slot_bot          BOOLEAN     NOT NULL,

    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           VARCHAR(40) NOT NULL DEFAULT 'system:seed'
);

-- 슬롯 조합 빠른 조회 (TK-04-1-2 Matrix 빌드용)
CREATE INDEX IF NOT EXISTS idx_vc_constraint_lp_slots
    ON master.vc_constraint (lp_slot_top, lp_slot_upmid, lp_slot_lowmid, lp_slot_bot);

CREATE INDEX IF NOT EXISTS idx_vc_constraint_ic_slots
    ON master.vc_constraint (ic_slot_top, ic_slot_mid, ic_slot_bot);

-- Unschedulable 품번 부분 인덱스 (BR-V11 — 모든 슬롯 X)
CREATE INDEX IF NOT EXISTS idx_vc_constraint_unschedulable
    ON master.vc_constraint (hose_id)
    WHERE NOT lp_slot_top AND NOT lp_slot_upmid AND NOT lp_slot_lowmid AND NOT lp_slot_bot
      AND NOT ic_slot_top AND NOT ic_slot_mid AND NOT ic_slot_bot;

-- LISTEN/NOTIFY — TK-04-1-2 캐시 무효화 (ADR-017)
CREATE OR REPLACE FUNCTION master.notify_vc_constraint_change() RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('vc_constraint_changed',
        COALESCE(NEW.hose_id, OLD.hose_id));
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vc_constraint_notify ON master.vc_constraint;
CREATE TRIGGER trg_vc_constraint_notify
    AFTER INSERT OR UPDATE OR DELETE ON master.vc_constraint
    FOR EACH ROW EXECUTE FUNCTION master.notify_vc_constraint_change();

COMMENT ON TABLE  master.vc_constraint                   IS 'TK-04-1-1 47품번 성형 슬롯 적합성 (REF-09 G~J·M~O)';
COMMENT ON COLUMN master.vc_constraint.composite_count   IS 'BR-V14 합금형 — 1·2·3·6 (CHECK)';
COMMENT ON COLUMN master.vc_constraint.lp_slot_top       IS 'BR-V13/15 — 저압 TOP 슬롯 적합성 O/X';
COMMENT ON COLUMN master.vc_constraint.ic_slot_top       IS 'IC TOP 슬롯 적합성 O/X (TK-04-1-2 Matrix 입력)';
