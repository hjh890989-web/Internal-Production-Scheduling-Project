package com.scheduling.vc.mes;

/**
 * Sprint 17 BR-X06 MES shift event source — 자동 수신 vs 수동 폴백 구분.
 */
public enum MesShiftSource {
    /** MES 자동 수신 (Sprint 17 baseline 미연동, Phase 5+ 통합). */
    MES,
    /** Excel 수동 입력 폴백 — PLANNER 또는 IT_OPS RBAC. */
    EXCEL_FALLBACK
}
