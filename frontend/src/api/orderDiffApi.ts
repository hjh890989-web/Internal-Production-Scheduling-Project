import { apiFetch } from './client'

export type DiffType = 'NEW' | 'MODIFIED' | 'DELETED' | 'UNCHANGED'
export type Severity = 'CRITICAL' | 'IMPORTANT' | 'STANDARD'

export interface RowDiffSummary {
  changeId: string
  diffType: DiffType
  hoseId: string
  deliveryDate: string
  newOrderId: string | null
  oldOrderId: string | null
  fieldDiffs: string  // JSON string (Sprint 14 에서 parsed object 변환 예정)
  previousVersion: number
  newVersion: number
  severity: Severity | null
  changedAt: string
}

export interface DiffSummaryResponse {
  trackingId: string
  totalRows: number
  criticalCount: number
  importantCount: number
  standardCount: number
  unclassifiedCount: number
  rows: RowDiffSummary[]
}

export interface CommitResponse {
  trackingId: string
  decidedBy: string
  decidedAt: string
  affectedRows: number
  reason: string
}

export const orderDiffApi = {
  get: (trackingId: string) =>
    apiFetch<DiffSummaryResponse>(`/api/v1/orders/${trackingId}/diff`),

  commit: (trackingId: string, reason: string) =>
    apiFetch<CommitResponse>(`/api/v1/orders/${trackingId}/commit`, {
      method: 'POST', body: JSON.stringify({ reason }),
    }),

  reject: (trackingId: string, reason: string) =>
    apiFetch<CommitResponse>(`/api/v1/orders/${trackingId}/reject`, {
      method: 'POST', body: JSON.stringify({ reason }),
    }),
}
