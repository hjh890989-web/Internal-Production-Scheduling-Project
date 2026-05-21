import { useQuery } from '@tanstack/react-query'
import { fetchCompat } from '../api/compatApi'
import type { CompatibilityResponse } from '../types/slotCompat'

/**
 * 매트릭스 클라이언트 캐시 — TK-04-3-1.
 *
 * - staleTime 5분 (백엔드 Cache-Control max-age=300 정합)
 * - gcTime 30분 — 메모리 보존
 * - refetchOnWindowFocus false — 사용자 작업 중 불필요 refetch 방지
 *
 * ETag 처리는 fetch wrapper (apiFetch) 가 If-None-Match 자동 송신 — 304 시
 * TanStack Query 가 stale 데이터 재사용 (네트워크 비용 ↓).
 */
export function useSlotCompatibilityMatrix() {
  return useQuery<CompatibilityResponse>({
    queryKey: ['vc', 'compat'],
    queryFn: fetchCompat,
    staleTime: 5 * 60 * 1000,
    gcTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
  })
}
