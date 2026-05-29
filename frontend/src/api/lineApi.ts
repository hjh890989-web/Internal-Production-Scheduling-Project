import { apiFetch } from './client'

export type LineType = 'LP' | 'IC' | 'EX'

export interface LineSummary {
  lineCode: string
  lineName: string
  lineType: LineType
  active: boolean
  productCompatibility: string[]
}

export interface LineCreatePayload {
  lineCode: string
  lineName: string
  lineType: LineType
}

export interface LineUpdatePayload {
  lineName: string
}

export const lineApi = {
  list: () => apiFetch<LineSummary[]>('/api/v1/master/lines'),
  create: (p: LineCreatePayload) =>
    apiFetch<LineSummary>('/api/v1/master/lines', {
      method: 'POST', body: JSON.stringify(p),
    }),
  update: (lineCode: string, p: LineUpdatePayload) =>
    apiFetch<LineSummary>(`/api/v1/master/lines/${encodeURIComponent(lineCode)}`, {
      method: 'PUT', body: JSON.stringify(p),
    }),
  remove: (lineCode: string) =>
    apiFetch<void>(`/api/v1/master/lines/${encodeURIComponent(lineCode)}`, {
      method: 'DELETE',
    }),
  updateProducts: (lineCode: string, hoseIds: string[]) =>
    apiFetch<LineSummary>(`/api/v1/master/lines/${encodeURIComponent(lineCode)}/products`, {
      method: 'PUT', body: JSON.stringify(hoseIds),
    }),
}
