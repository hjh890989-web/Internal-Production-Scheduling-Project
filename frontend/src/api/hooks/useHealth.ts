import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../client'

interface HealthResponse {
  status: 'UP' | 'DOWN'
  components?: Record<string, { status: string }>
}

/**
 * Spring Boot Actuator health 표본 useQuery hook.
 * 30초마다 자동 갱신. EP-31 (관측성) 통합 후 dashboard 컴포넌트에서 활용.
 */
export function useHealth() {
  return useQuery({
    queryKey: ['actuator', 'health'],
    queryFn: () => apiFetch<HealthResponse>('/api/actuator/health'),
    refetchInterval: 30_000,
  })
}
