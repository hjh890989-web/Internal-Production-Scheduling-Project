// TK-04-3-1 — 슬롯 적합성 타입 (백엔드 com.scheduling.master.vc.* 정합)

export type MachineType = 'LP' | 'IC'

export type SlotPosition =
  | 'LP_TOP'
  | 'LP_UPMID'
  | 'LP_LOWMID'
  | 'LP_BOT'
  | 'IC_TOP'
  | 'IC_MID'
  | 'IC_BOT'

export const ALL_SLOTS: readonly SlotPosition[] = [
  'LP_TOP',
  'LP_UPMID',
  'LP_LOWMID',
  'LP_BOT',
  'IC_TOP',
  'IC_MID',
  'IC_BOT',
] as const

export function slotMachineType(slot: SlotPosition): MachineType {
  return slot.startsWith('LP_') ? 'LP' : 'IC'
}

/** GET /api/v1/master/compat 응답 (백엔드 CompatibilityResponse 정합). */
export interface CompatibilityResponse {
  version: number
  builtAt: string // ISO-8601
  byHose: Record<string, Record<SlotPosition, boolean>>
  bySlot: Record<SlotPosition, string[]>
  unschedulableHoseIds: string[]
}

/** 단일 (품번, 슬롯) 조회 응답. */
export interface PointCheckResponse {
  hoseId: string
  slotPosition: SlotPosition
  eligible: boolean
}

/** dnd-kit data payload (DraggableHose → DroppableSlot). */
export interface DragData {
  hoseId: string
  qty: number
}

/** 매트릭스 client-side 조회 — 미로딩·미존재 시 적합 fallback (UX). */
export function isEligible(
  compat: CompatibilityResponse | undefined,
  hoseId: string | undefined,
  slot: SlotPosition,
): boolean {
  if (!compat || !hoseId) return true
  const hoseSlots = compat.byHose[hoseId]
  if (!hoseSlots) return true // 매트릭스 미적재 품번 — Optimistic OK
  return hoseSlots[slot] === true
}
