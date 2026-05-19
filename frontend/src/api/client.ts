import { useAuthStore } from '@/stores/authStore'

/**
 * 표준 API 에러 — TanStack Query retry 로직과 결합.
 * status >= 400 < 500 → 재시도 안 함.
 */
export class HttpError extends Error {
  constructor(
    public status: number,
    public body: unknown,
    message?: string,
  ) {
    super(message ?? `HTTP ${status}`)
    this.name = 'HttpError'
  }
}

/**
 * fetch wrapper — Bearer 토큰 자동 부착.
 * EP-30 (Keycloak) 통합 시 401 → 토큰 갱신 후 재시도 로직 추가.
 */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = useAuthStore.getState().token
  const headers = new Headers(init?.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(path, { ...init, headers })
  if (!res.ok) {
    const body: unknown = await res.json().catch(() => null)
    throw new HttpError(res.status, body)
  }
  return (await res.json()) as T
}
