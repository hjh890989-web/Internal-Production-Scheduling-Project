import { useQuery } from '@tanstack/react-query'
import { fetchImportStatus, fetchMappingResult } from '../api/mappingApi'
import type { ImportStatusResponse, MappingResult } from '../types/mapping'

/**
 * Import 추적 상태 — TK-01-2-2.
 *
 * 5초 폴링 (status 가 terminal — MAPPED·FAILED·REVIEW_REQUIRED — 도달 후 중단).
 */
export function useImportStatus(trackingId: string | undefined, enabled = true) {
  return useQuery<ImportStatusResponse>({
    queryKey: ['importStatus', trackingId],
    queryFn: () => fetchImportStatus(trackingId!),
    enabled: enabled && !!trackingId,
    refetchInterval: (q) => {
      const s = q.state.data?.status
      if (s === 'MAPPED' || s === 'FAILED' || s === 'REVIEW_REQUIRED') return false
      return 5_000
    },
  })
}

/** TK-01-2-2 — 매핑 결과 (successes + failures + sourceType). status 가 MAPPED/REVIEW_REQUIRED 일 때만 fetch. */
export function useMappingResult(trackingId: string | undefined, enabled = true) {
  return useQuery<MappingResult>({
    queryKey: ['mappingResult', trackingId],
    queryFn: () => fetchMappingResult(trackingId!),
    enabled: enabled && !!trackingId,
    staleTime: 30_000,
  })
}
