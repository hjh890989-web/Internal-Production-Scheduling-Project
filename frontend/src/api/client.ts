import { useAuthStore, type Role } from '@/stores/authStore'

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
 * fetch wrapper — Bearer 토큰 자동 부착 + 401 처리 (logout + /login redirect).
 *
 * Sprint 10 EP-AUTH (NFR-SEC-007) — 토큰 만료/잘못된 토큰 → 401 → 강제 로그아웃 + 로그인 화면 이동.
 * 로그인 화면 자체에서의 401 은 redirect 무시 (정상 실패 응답).
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
    if (res.status === 401 && !window.location.pathname.startsWith('/login')) {
      useAuthStore.getState().logout()
      window.location.assign('/login')
    }
    throw new HttpError(res.status, body)
  }
  return (await res.json()) as T
}

/**
 * Sprint 10 ST-AUTH-4 로그인 — POST /api/v1/auth/login (사번 + PIN → JWT).
 *
 * apiFetch 우회 — 401 시 redirect 하면 안 됨 (정상 실패 응답으로 화면에 에러 표시).
 * 성공 시 호출자가 {@link useAuthStore#setSession} 호출.
 */
export interface LoginResponse {
  token: string
  employeeId: string
  role: Role
  expiresAt: string
}

export async function login(employeeId: string, pin: string): Promise<LoginResponse> {
  const res = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ employeeId, pin }),
  })
  const body: unknown = await res.json().catch(() => null)
  if (!res.ok) {
    throw new HttpError(res.status, body)
  }
  return body as LoginResponse
}
