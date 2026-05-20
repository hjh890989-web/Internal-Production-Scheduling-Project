// TK-01-2-2 — 매핑 보정 UI 백엔드 API 래퍼.
// 본 파일은 fetch wrapper (`@/api/client`) 위에 endpoint 별 함수 export.

import { apiFetch } from '@/api/client'
import type {
  ImportStatusResponse,
  MappingResult,
  MappingRule,
  SourceType,
} from '../types/mapping'

/** GET /api/v1/orders/import/{trackingId} — 추적 상태 (status + classifications). */
export function fetchImportStatus(trackingId: string): Promise<ImportStatusResponse> {
  return apiFetch<ImportStatusResponse>(`/api/v1/orders/import/${trackingId}`)
}

/** GET /api/v1/orders/import/{trackingId}/mapping-result — 매핑 보정 UI 데이터 소스 (TK-01-2-2). */
export function fetchMappingResult(trackingId: string): Promise<MappingResult> {
  return apiFetch<MappingResult>(`/api/v1/orders/import/${trackingId}/mapping-result`)
}

/**
 * POST /api/v1/orders/import/{trackingId}/retry — 룰 보정 후 라운드트립 재매핑.
 * 캐시 TTL 24h 만료 시 HTTP 410.
 */
export function retryMapping(trackingId: string): Promise<unknown> {
  return apiFetch(`/api/v1/orders/import/${trackingId}/retry`, { method: 'POST' })
}

/** GET /api/v1/master/mapping-rule/{sourceType} — 현재 룰셋 조회. */
export function fetchMappingRule(sourceType: SourceType): Promise<MappingRule> {
  return apiFetch<MappingRule>(`/api/v1/master/mapping-rule/${sourceType}`)
}

/** PUT /api/v1/master/mapping-rule/{sourceType} — 룰셋 갱신 (별칭 추가 등). */
export function updateMappingRule(
  sourceType: SourceType,
  rule: MappingRule,
): Promise<MappingRule> {
  return apiFetch<MappingRule>(`/api/v1/master/mapping-rule/${sourceType}`, {
    method: 'PUT',
    body: JSON.stringify(rule),
  })
}
