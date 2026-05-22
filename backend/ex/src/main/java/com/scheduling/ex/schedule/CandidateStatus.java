package com.scheduling.ex.schedule;

/**
 * 압출 후보 상태 — TK-07-1-2.
 *
 * <ul>
 *   <li>{@code PENDING} — EP-07 생성 직후 (deadline 만 산출)</li>
 *   <li>{@code READY} — EP-08 yield + Q_ext 계산 완료</li>
 *   <li>{@code SCHEDULED} — EP-09 셋팅 그룹 + shift 배정 완료</li>
 *   <li>{@code CONFIRMED} — 사용자 확정 (Sprint 4 EP-10)</li>
 *   <li>{@code FAILED} — EP-EX11 검증 게이트 fail</li>
 * </ul>
 */
public enum CandidateStatus {
    PENDING, READY, SCHEDULED, CONFIRMED, FAILED
}
