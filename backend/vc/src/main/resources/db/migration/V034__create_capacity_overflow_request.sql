-- =============================================================================
-- V034 — Sprint 8 EP-V12-승인 워크플로우 (BR-V12 추가 요청 큐 승인/거절)
-- =============================================================================
-- Sprint 7 carry-over BR-V12 split() 의 requestQueue 를 영속화 + Planner 1클릭
-- 승인/거절 워크플로우 진입점. CapacityOverflowApprovalService 의 백엔드 자산.
--
-- 활성 조건 — Sprint 7 V033 PRODUCT_PRIORITY 마스터 입력 후 split() 호출 시
-- 추가 요청 큐 발생 → 본 테이블에 PENDING 으로 영속 → Planner 결정 (ACCEPT|REJECT).
-- =============================================================================

CREATE TABLE app.capacity_overflow_request (
    request_id      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    hose_id         VARCHAR(50)  NOT NULL,
    requested_qty   INTEGER      NOT NULL CHECK (requested_qty > 0),
    priority_rank   SMALLINT     NOT NULL,            -- split() 시점의 rank 보존 (audit trail)
    requested_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    requested_by    VARCHAR(50)  NOT NULL,            -- Planner 또는 system
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                 CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    decided_at      TIMESTAMPTZ,
    decided_by      VARCHAR(50),
    decision_reason VARCHAR(500),                     -- REJECT 시 필수, ACCEPT 시 선택
    CONSTRAINT chk_decided_consistency
        CHECK ((status = 'PENDING' AND decided_at IS NULL AND decided_by IS NULL)
               OR (status != 'PENDING' AND decided_at IS NOT NULL AND decided_by IS NOT NULL)),
    CONSTRAINT chk_reject_reason
        CHECK (status != 'REJECTED' OR decision_reason IS NOT NULL)
);

COMMENT ON TABLE app.capacity_overflow_request IS
    'Sprint 8 BR-V12 capa 초과 추가 요청 큐 — Planner 1클릭 승인/거절 워크플로우 (REQ-FUNC-VC-022)';
COMMENT ON COLUMN app.capacity_overflow_request.status IS
    'PENDING → ACCEPTED|REJECTED 상태 머신 (한 번만 전이)';
COMMENT ON COLUMN app.capacity_overflow_request.priority_rank IS
    'split() 시점의 rank — 결정 후에도 audit trail 유지 (마스터 변경 영향 0)';

CREATE INDEX idx_capacity_overflow_request_status_hose
    ON app.capacity_overflow_request (status, hose_id, requested_at);

-- =============================================================================
-- 상태 머신 trigger — PENDING → ACCEPTED|REJECTED 만 허용, 중복 결정 차단
-- (Sprint 5 V028 swap proposal 의 enforce_swap_proposal_transition 동일 패턴)
-- =============================================================================
CREATE OR REPLACE FUNCTION app.enforce_capacity_overflow_request_transition()
RETURNS TRIGGER AS $$
BEGIN
    -- PENDING 만 변경 가능 (ACCEPTED/REJECTED 후 immutable)
    IF OLD.status != 'PENDING' THEN
        RAISE EXCEPTION 'BR-V12 capacity_overflow_request 결정 후 변경 불가 (OLD.status=%, NEW.status=%)',
            OLD.status, NEW.status
            USING ERRCODE = 'P0001';
    END IF;
    -- 전이 검증 — PENDING → ACCEPTED|REJECTED 만 허용
    IF NEW.status NOT IN ('ACCEPTED', 'REJECTED') THEN
        RAISE EXCEPTION 'BR-V12 capacity_overflow_request 잘못된 전이 PENDING → %', NEW.status
            USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enforce_capacity_overflow_request_transition
    BEFORE UPDATE OF status ON app.capacity_overflow_request
    FOR EACH ROW
    EXECUTE FUNCTION app.enforce_capacity_overflow_request_transition();
