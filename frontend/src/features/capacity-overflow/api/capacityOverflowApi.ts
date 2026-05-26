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

/**
 * Sprint 8 BR-V12 CapacityOverflowRequest — 백엔드 entity 1:1.
 */
export interface CapacityOverflowRequest {
  requestId: string
  hoseId: string
  requestedQty: number
  priorityRank: number
  requestedAt: string
  requestedBy: string
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  decidedAt: string | null
  decidedBy: string | null
  decisionReason: string | null
}

export interface EnqueueResponse {
  requestIds: string[]
}

/** Sprint 8 BR-V12 — split() requestQueue 영속화 (Planner 명시 등록). */
export async function enqueueRequests(
  queue: Record<string, number>,
): Promise<EnqueueResponse> {
  return apiFetch<EnqueueResponse>('/api/v1/schedule/vc/capacity-overflow/enqueue', {
    method: 'POST',
    body: JSON.stringify({ queue }),
  })
}

/** Sprint 8 BR-V12 — Planner 1클릭 승인 (note 선택). */
export async function acceptRequest(
  requestId: string,
  reason?: string,
): Promise<CapacityOverflowRequest> {
  return apiFetch<CapacityOverflowRequest>(
    `/api/v1/schedule/vc/capacity-overflow/queue/${requestId}/accept`,
    {
      method: 'POST',
      body: JSON.stringify(reason ? { reason } : {}),
    },
  )
}

/** Sprint 8 BR-V12 — Planner 1클릭 거절 (reason 필수). */
export async function rejectRequest(
  requestId: string,
  reason: string,
): Promise<CapacityOverflowRequest> {
  return apiFetch<CapacityOverflowRequest>(
    `/api/v1/schedule/vc/capacity-overflow/queue/${requestId}/reject`,
    {
      method: 'POST',
      body: JSON.stringify({ reason }),
    },
  )
}

/** Sprint 8 BR-V12 — 상태별 큐 조회 (기본 PENDING). */
export async function listQueueRequests(
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' = 'PENDING',
): Promise<CapacityOverflowRequest[]> {
  return apiFetch<CapacityOverflowRequest[]>(
    `/api/v1/schedule/vc/capacity-overflow/queue?status=${status}`,
  )
}
