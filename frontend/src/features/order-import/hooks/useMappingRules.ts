import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchMappingRule, updateMappingRule } from '../api/mappingApi'
import type { MappingRule, SourceType } from '../types/mapping'

/**
 * 매핑 룰셋 조회 + 갱신 hook — TK-01-2-2.
 *
 * sourceType undefined / UNRECOGNIZED 면 비활성 (fetch 안 함).
 */
export function useMappingRule(sourceType: SourceType | undefined) {
  return useQuery<MappingRule>({
    queryKey: ['mappingRule', sourceType],
    queryFn: () => fetchMappingRule(sourceType!),
    enabled: !!sourceType && sourceType !== 'UNRECOGNIZED',
    staleTime: 60_000,
  })
}

export function useUpdateMappingRule(sourceType: SourceType | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (rule: MappingRule) => updateMappingRule(sourceType!, rule),
    onSuccess: (data) => {
      qc.setQueryData(['mappingRule', sourceType], data)
    },
  })
}
