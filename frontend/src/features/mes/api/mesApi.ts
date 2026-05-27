import { apiFetch } from '@/api/client'

/**
 * Sprint 17 EP-DAY-LOCK MES API client — TK-DAY-LOCK-4 응답.
 *
 * <p>Backend MesController 의 ProblemDetail/Response record 1:1.
 */

export type MesShiftSource = 'MES' | 'EXCEL_FALLBACK'

export interface MachineStatus {
  machineId: string
  degraded: boolean
  lastReceivedAt: string | null
  lastSource: MesShiftSource | null
}

export interface DegradedSnapshot {
  anyDegraded: boolean
  checkedAt: string
  summary: string
  machines: MachineStatus[]
}

export interface ShiftFallbackPayload {
  machineId: string
  shiftDate: string // YYYY-MM-DD
  shiftNo: number  // 1~4
  plannedQty: number
  actualQty?: number | null
  note?: string | null
}

export interface ShiftFallbackResponse {
  shiftEventId: string
  machineId: string
  shiftDate: string
  shiftNo: number
  plannedQty: number
  actualQty: number | null
  source: MesShiftSource
  reportedBy: string
}

export async function fetchDegradedStatus(): Promise<DegradedSnapshot> {
  return apiFetch<DegradedSnapshot>('/api/v1/mes/degraded/status')
}

export async function postShiftFallback(
  payload: ShiftFallbackPayload,
): Promise<ShiftFallbackResponse> {
  return apiFetch<ShiftFallbackResponse>('/api/v1/mes/shift/fallback', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
