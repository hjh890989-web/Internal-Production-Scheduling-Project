import { apiFetch } from '@/api/client'

/**
 * VC schedule slot row — 백엔드 VcScheduleQueryController.SlotRow record 1:1.
 */
export interface VcSlotRow {
  vcScheduleId: string
  hoseId: string
  machineId: string
  slotPosition: number
  productionDate: string // YYYY-MM-DD
  rotationNo: number    // 1~18 (BR-V04)
  angleId: string
  plannedQty: number
  status: 'CANDIDATE' | 'CONFIRMED' | 'DONE'
}

/** EP-15 시뮬뷰 fetch — TK-15-1-1. */
export async function fetchVcSlots(from: string, to: string): Promise<VcSlotRow[]> {
  return apiFetch<VcSlotRow[]>(`/api/v1/schedule/vc/slots?from=${from}&to=${to}`)
}
