import { apiFetch } from './client'

export interface SpecSummary {
  hoseId: string
  spec: number | null
  compositeCount: number | null
  lpLeftSetting: string | null
  lpRightSetting: string | null
  angleCount: number
  isSpecLt7: boolean
}

export const productSpecApi = {
  list: () => apiFetch<SpecSummary[]>('/api/v1/master/product-spec'),
  get: (hoseId: string) =>
    apiFetch<SpecSummary>(`/api/v1/master/product-spec/${encodeURIComponent(hoseId)}`),
}
