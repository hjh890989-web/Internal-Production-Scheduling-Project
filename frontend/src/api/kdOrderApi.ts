import { apiFetch } from './client'

export type KdStatus = 'OPEN' | 'PARTIAL' | 'FILLED' | 'CANCELLED'

export interface KdOrderSummary {
  kdOrderId: string
  hoseId: string
  orderQty: number
  remainingQty: number
  orderDate: string
  customerCode: string | null
  status: KdStatus
  updatedAt: string
  updatedBy: string
}

export interface KdOrderPayload {
  hoseId: string
  orderQty: number
  remainingQty: number
  orderDate: string
  customerCode?: string
  status?: KdStatus
}

export const kdOrderApi = {
  list: () => apiFetch<KdOrderSummary[]>('/api/v1/master/kd-order'),
  create: (p: KdOrderPayload) =>
    apiFetch<KdOrderSummary>('/api/v1/master/kd-order', {
      method: 'POST', body: JSON.stringify(p),
    }),
  update: (id: string, p: KdOrderPayload) =>
    apiFetch<KdOrderSummary>(`/api/v1/master/kd-order/${id}`, {
      method: 'PUT', body: JSON.stringify(p),
    }),
  delete: (id: string) =>
    apiFetch<void>(`/api/v1/master/kd-order/${id}`, { method: 'DELETE' }),
}
