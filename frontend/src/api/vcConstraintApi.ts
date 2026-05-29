import { apiFetch } from './client'

/** BR-V14 — compositeCount 허용값 (1·2·3·6 전용) */
export const COMPOSITE_COUNT_OPTIONS = [1, 2, 3, 6] as const
export type CompositeCount = (typeof COMPOSITE_COUNT_OPTIONS)[number]

export interface VcConstraintSummary {
  hoseId: string
  compositeCount: CompositeCount
  lpMoldQty: number
  icMoldQty: number
  slot1: boolean
  slot2: boolean
  slot3: boolean
  slot4: boolean
  slot5: boolean
  slot6: boolean
  slot7: boolean
}

export interface VcConstraintPayload {
  hoseId: string
  compositeCount: CompositeCount
  lpMoldQty: number
  icMoldQty: number
  slot1: boolean
  slot2: boolean
  slot3: boolean
  slot4: boolean
  slot5: boolean
  slot6: boolean
  slot7: boolean
}

/**
 * Sprint 21 ST-CRUD-3 — VC 제약 마스터 CRUD.
 *
 * compositeCount IN (1,2,3,6) — BR-V14 합금형 제약.
 * slotEligibility 7 컬럼은 slot1~slot7 boolean flat 전송.
 */
export const vcConstraintApi = {
  list: () => apiFetch<VcConstraintSummary[]>('/api/v1/master/vc-constraints'),
  create: (p: VcConstraintPayload) =>
    apiFetch<VcConstraintSummary>('/api/v1/master/vc-constraints', {
      method: 'POST',
      body: JSON.stringify(p),
    }),
  update: (hoseId: string, p: VcConstraintPayload) =>
    apiFetch<VcConstraintSummary>(
      `/api/v1/master/vc-constraints/${encodeURIComponent(hoseId)}`,
      { method: 'PUT', body: JSON.stringify(p) },
    ),
}
