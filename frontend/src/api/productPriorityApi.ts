import { apiFetch } from './client'

export interface PrioritySummary {
  hoseId: string
  priorityRank: number
  rationale: string | null
  effectiveFrom: string
  effectiveTo: string | null
  updatedAt: string
  updatedBy: string
}

export interface PriorityPayload {
  hoseId: string
  priorityRank: number
  rationale?: string
  effectiveFrom: string
  effectiveTo?: string
}

export const productPriorityApi = {
  list: () => apiFetch<PrioritySummary[]>('/api/v1/master/product-priority'),
  create: (p: PriorityPayload) =>
    apiFetch<PrioritySummary>('/api/v1/master/product-priority', {
      method: 'POST', body: JSON.stringify(p),
    }),
  update: (hoseId: string, p: PriorityPayload) =>
    apiFetch<PrioritySummary>(`/api/v1/master/product-priority/${encodeURIComponent(hoseId)}`, {
      method: 'PUT', body: JSON.stringify(p),
    }),
  delete: (hoseId: string) =>
    apiFetch<void>(`/api/v1/master/product-priority/${encodeURIComponent(hoseId)}`, {
      method: 'DELETE',
    }),
}
