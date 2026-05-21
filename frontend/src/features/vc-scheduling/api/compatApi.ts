// TK-04-3-1 — VC compat 백엔드 API 래퍼

import { apiFetch } from '@/api/client'
import type {
  CompatibilityResponse,
  PointCheckResponse,
  SlotPosition,
} from '../types/slotCompat'

/** GET /api/v1/master/compat — 매트릭스 전체 조회 (ETag 캐싱). */
export function fetchCompat(): Promise<CompatibilityResponse> {
  return apiFetch<CompatibilityResponse>('/api/v1/master/compat')
}

/** GET /api/v1/master/compat/{hose}/{slot} — Point check. */
export function fetchPointCheck(
  hoseId: string,
  slotPosition: SlotPosition,
): Promise<PointCheckResponse> {
  return apiFetch<PointCheckResponse>(
    `/api/v1/master/compat/${encodeURIComponent(hoseId)}/${slotPosition}`,
  )
}
