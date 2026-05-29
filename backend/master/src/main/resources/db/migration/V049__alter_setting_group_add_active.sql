-- =============================================================================
-- V034__alter_setting_group_add_active.sql — Sprint 21 ST-CRUD-2
-- =============================================================================
-- setting_group 비활성 토글 지원 (DELETE = soft-delete, BR-V12·V13 cross-ref).
-- =============================================================================

ALTER TABLE master.setting_group
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN master.setting_group.active IS
    'Sprint 21 ST-CRUD-2 — soft-delete 플래그 (active=false → 스케줄 엔진 미사용)';
