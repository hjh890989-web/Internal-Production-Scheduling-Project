// TK-04-3-2 — 위반 검증 + override audit 타입

import type { SlotPosition } from './slotCompat'

export interface ValidationResult {
  ok: boolean
  reason?: string
  alternatives?: SlotPosition[]
}

export interface ViolationInfo {
  hoseId: string
  qty: number
  slot: SlotPosition
  reason: string
  alternatives: SlotPosition[]
}

/** POST /api/v1/audit/override payload — REQ-FUNC-CO-010. */
export interface OverrideAuditRequest {
  hoseId: string
  slotPosition: SlotPosition
  justification: string // ≥ 10자 강제
}

export interface OverrideAuditResponse {
  auditId: string
  recordedAt: string
}

/** 한국어 슬롯 디스플레이 이름 (모달용). */
export function slotDisplayName(slot: SlotPosition): string {
  const map: Record<SlotPosition, string> = {
    LP_TOP: 'LP 상단',
    LP_UPMID: 'LP 중상',
    LP_LOWMID: 'LP 중하',
    LP_BOT: 'LP 하단',
    IC_TOP: 'IC 상단',
    IC_MID: 'IC 중단',
    IC_BOT: 'IC 하단',
  }
  return map[slot] ?? slot
}
