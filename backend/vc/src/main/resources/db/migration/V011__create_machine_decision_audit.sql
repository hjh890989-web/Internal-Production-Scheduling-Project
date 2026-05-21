-- =============================================================================
-- V011__create_machine_decision_audit.sql — TK-05-4-2 (BR-X02 / K-V06)
-- =============================================================================
-- 라우팅 결정 audit — 사후 "왜 IC가 사용됐는가" 추적성 확보.
-- K-V06 가류기 사용률 KPI 데이터 소스.
--
-- schema: audit (BR-X02 정합 — REQ-NF-SEC-004 ≥3년 보존).
-- =============================================================================

CREATE TABLE IF NOT EXISTS audit.machine_decision (
    decision_id        UUID         PRIMARY KEY,
    decided_at         TIMESTAMPTZ  NOT NULL,
    hose_id            VARCHAR(40)  NOT NULL,
    production_date    DATE         NOT NULL,
    machine_id         VARCHAR(10)  NOT NULL,
    machine_type       VARCHAR(2)   NOT NULL CHECK (machine_type IN ('LP', 'IC')),
    decision_type      VARCHAR(20)  NOT NULL CHECK (decision_type IN
        ('LP_PRIMARY', 'LP_FALLBACK', 'IC_PRIMARY', 'IC_FALLBACK')),
    policy_id          VARCHAR(20)  NOT NULL,
    reason             TEXT,
    actor              VARCHAR(40)  NOT NULL DEFAULT 'system:allocator'
);

CREATE INDEX IF NOT EXISTS idx_machine_decision_date
    ON audit.machine_decision (production_date);

CREATE INDEX IF NOT EXISTS idx_machine_decision_type
    ON audit.machine_decision (decision_type, decided_at);

CREATE INDEX IF NOT EXISTS idx_machine_decision_hose
    ON audit.machine_decision (hose_id);

COMMENT ON TABLE  audit.machine_decision               IS 'TK-05-4-2 라우팅 결정 audit (BR-X02 + K-V06)';
COMMENT ON COLUMN audit.machine_decision.decision_type IS 'LP_PRIMARY / LP_FALLBACK / IC_PRIMARY / IC_FALLBACK';
COMMENT ON COLUMN audit.machine_decision.policy_id     IS '활성 라우팅 정책 ID (LP_FIRST / IC_FIRST / ...)';
COMMENT ON COLUMN audit.machine_decision.actor         IS 'system:allocator (자동) 또는 사용자 ID (수동 override)';

-- BR-X02 — INSERT only 정책. UPDATE/DELETE 트리거 + role 정책은 Sprint 2 EP-11 강화 (Sprint 1 baseline V003 동일 패턴).
