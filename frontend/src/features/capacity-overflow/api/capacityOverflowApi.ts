import { apiFetch } from '@/api/client'

/**
 * BR-V12 SplitResult — 백엔드 CapacityOverflowQueueService.SplitResult 1:1.
 */
export interface SplitResult {
  accepted: Record<string, number>        // hose_id → 자동 채택 qty
  requestQueue: Record<string, number>    // hose_id → 승인 대기 qty
  totalAccepted: number
  totalQueued: number
}

/** BR-V13 SupplementResult — 백엔드 KdSupplementService.SupplementResult 1:1. */
export interface SupplementResult {
  hoseId: string
  shortage: number
  supplemented: number
  consumed: ConsumedEntry[]
}

export interface ConsumedEntry {
  kdOrderId: string
  fromHose: string
  qty: number
}

export async function splitCapacity(
  required: Record<string, number>,
  dailyCapa: number,
): Promise<SplitResult> {
  return apiFetch<SplitResult>('/api/v1/schedule/vc/capacity-overflow/split', {
    method: 'POST',
    body: JSON.stringify({ required, dailyCapa }),
  })
}

export async function supplementKd(
  hoseId: string,
  shortage: number,
): Promise<SupplementResult> {
  return apiFetch<SupplementResult>('/api/v1/schedule/vc/capacity-overflow/supplement', {
    method: 'POST',
    body: JSON.stringify({ hoseId, shortage }),
  })
}
