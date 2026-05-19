package com.scheduling.common.enums;

/**
 * 변경 심각도 — 수주·마스터·일정 변경 이벤트의 영향 정도.
 *
 * BR-O02 (수주 변경 분류): D-Day 임박 / 다른 일정 cascade 영향 / 수량 ±10% 초과 시 CRITICAL.
 * NORMAL = 정보 전파만, 자동 reprocess.
 * CRITICAL = 사용자 알림 + 수동 confirm 필요.
 */
public enum ChangeSeverity {
    NORMAL,
    CRITICAL
}
