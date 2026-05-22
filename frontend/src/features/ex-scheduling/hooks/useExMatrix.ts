import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { fetchExMatrix, type ExMatrixRow } from '../api/exMatrixApi'
import { useExUpdates } from './useExUpdates'

/**
 * EP-17 매트릭스 fetch + EP-EX14 cascade auto-refetch — TK-17-1-2.
 *
 * <p>STOMP /topic/extrusion-updates 수신 시 invalidateQueries → 자동 재조회 (UI 갱신).
 */
export function useExMatrix(from: string, to: string): {
  data: ExMatrixRow[] | undefined
  isLoading: boolean
  error: unknown
  connected: boolean
  lastUpdate: ReturnType<typeof useExUpdates>['lastUpdate']
} {
  const qc = useQueryClient()
  const query = useQuery<ExMatrixRow[]>({
    queryKey: ['ex-matrix', from, to],
    queryFn: () => fetchExMatrix(from, to),
    staleTime: 30_000,
  })

  const updates = useExUpdates()

  useEffect(() => {
    if (updates.lastUpdate) {
      void qc.invalidateQueries({ queryKey: ['ex-matrix'] })
    }
  }, [updates.lastUpdate, qc])

  return {
    data: query.data,
    isLoading: query.isLoading,
    error: query.error,
    connected: updates.connected,
    lastUpdate: updates.lastUpdate,
  }
}
