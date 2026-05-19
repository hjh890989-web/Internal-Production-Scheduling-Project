import { QueryClient } from '@tanstack/react-query'

/**
 * TanStack Query 5 client — 표준 설정.
 *
 * - staleTime 60s: 1분 fresh (자동 refetch 안 함)
 * - retry: 4xx는 재시도 안 함 (서버 입력 검증 실패), 5xx만 최대 3회
 * - refetchOnWindowFocus: false — 사용자 편집 중 갑작스러운 갱신 회피
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      gcTime: 5 * 60_000,
      retry: (failureCount, error) => {
        const status = (error as { status?: number })?.status
        if (status && status >= 400 && status < 500) return false
        return failureCount < 3
      },
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
})
