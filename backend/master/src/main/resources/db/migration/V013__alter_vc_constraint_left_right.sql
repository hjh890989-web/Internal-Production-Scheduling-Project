-- =============================================================================
-- V013__alter_vc_constraint_left_right.sql — TK-21-1-1 (EP-21 ST-21-1)
-- =============================================================================
-- v1.4 신규 — REF-09 K열(좌) / L열(우) 셋팅 호환성. BR-V15·V16 hard 제약.
-- 잘못된 좌/우 셋팅 슬롯에 배치 시 가류 불량 (1주 평균 6건 차단).
--
-- DEFAULT 'X' (보수적) — 명시적 'O' 등록 시에만 허용. 기존 47품번 row 안전 적용.
-- 47품번 K/L seed 는 DS-VC-CONSTRAINT-47/master_seed.sql 갱신.
-- =============================================================================

ALTER TABLE master.vc_constraint
    ADD COLUMN IF NOT EXISTS lp_left_setting  VARCHAR(1) NOT NULL DEFAULT 'X'
        CHECK (lp_left_setting  IN ('O','X')),
    ADD COLUMN IF NOT EXISTS lp_right_setting VARCHAR(1) NOT NULL DEFAULT 'X'
        CHECK (lp_right_setting IN ('O','X'));

-- 부분 인덱스 — RuleEngine 좌/우 검증 hot path (TK-21-1-2)
CREATE INDEX IF NOT EXISTS idx_vc_constraint_left
    ON master.vc_constraint (hose_id) WHERE lp_left_setting  = 'O';
CREATE INDEX IF NOT EXISTS idx_vc_constraint_right
    ON master.vc_constraint (hose_id) WHERE lp_right_setting = 'O';

COMMENT ON COLUMN master.vc_constraint.lp_left_setting  IS
    'BR-V15: 성형공정_제약조건.xlsx K열 좌측 셋팅 호환성 (O=가능, X=불가)';
COMMENT ON COLUMN master.vc_constraint.lp_right_setting IS
    'BR-V16: 성형공정_제약조건.xlsx L열 우측 셋팅 호환성 (O=가능, X=불가)';

-- 47품번 K/L seed — REF-09 v1.4 분석 결과 (28421-2M800 좌 only, 28422-2M800 우 only,
-- 28422-08HA0 양쪽 가능 + 그 외 47품번은 양쪽 가능으로 기본 전환 — 보수적이지만 회귀 안전)
UPDATE master.vc_constraint SET lp_left_setting='O', lp_right_setting='X'
 WHERE hose_id='28421-2M800';

UPDATE master.vc_constraint SET lp_left_setting='X', lp_right_setting='O'
 WHERE hose_id='28422-2M800';

UPDATE master.vc_constraint SET lp_left_setting='O', lp_right_setting='O'
 WHERE hose_id='28422-08HA0';

-- 그 외 LP slot 가용 품번 — 기본 양쪽 가능 (구체 분석 전까지 호환성 유지)
UPDATE master.vc_constraint SET lp_left_setting='O', lp_right_setting='O'
 WHERE hose_id NOT IN ('28421-2M800','28422-2M800','28422-08HA0')
   AND (lp_slot_top OR lp_slot_upmid OR lp_slot_lowmid OR lp_slot_bot);
