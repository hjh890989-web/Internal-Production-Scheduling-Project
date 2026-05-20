import { useMutation, useQueryClient } from '@tanstack/react-query'
import { retryMapping } from '../api/mappingApi'

/**
 * 라운드트립 재매핑 mutation — TK-01-2-2 + TK-01-2-3.
 *
 * 성공 시 importStatus + mappingResult 쿼리 무효화 → 자동 refetch.
 * 실패 시 HttpError 전파 (호출자가 message.error 로 표시).
 */
export function useRetryMapping(trackingId: string | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => retryMapping(trackingId!),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['importStatus', trackingId] })
      qc.invalidateQueries({ queryKey: ['mappingResult', trackingId] })
    },
  })
}
