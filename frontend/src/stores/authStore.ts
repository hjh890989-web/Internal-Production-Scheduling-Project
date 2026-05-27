import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/**
 * Sprint 10 EP-AUTH 인증 store — 로그인된 사용자 + JWT.
 *
 * Role enum 은 backend RoleConstants 정합 (대문자) — PLANNER/STK_USER/IT_OPS/READ_ONLY.
 * persist — localStorage 영속 (탭 닫고 8h 토큰 유효기간 내 재진입 시 자동 로그인).
 */
export type Role = 'PLANNER' | 'STK_USER' | 'IT_OPS' | 'READ_ONLY'

export interface AuthUser {
  employeeId: string
  role: Role
  expiresAt: string   // ISO Instant
}

interface AuthState {
  user: AuthUser | null
  token: string | null
  setSession: (token: string, user: AuthUser) => void
  logout: () => void
  isAuthenticated: () => boolean
  /** Sprint 11 ST-RBAC-3 — 현재 사용자가 주어진 role 보유 여부. 미인증 시 false. */
  hasRole: (role: Role) => boolean
  /** Sprint 11 ST-RBAC-3 — 주어진 role 중 하나라도 보유 시 true. 빈 배열 → true (제한 없음). */
  hasAnyRole: (roles: Role[]) => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      setSession: (token, user) => set({ token, user }),
      logout: () => set({ user: null, token: null }),
      isAuthenticated: () => {
        const state = get()
        if (!state.token || !state.user) return false
        // exp 시점 지나면 인증 무효 — interceptor 401 처리 전 1차 가드
        return new Date(state.user.expiresAt).getTime() > Date.now()
      },
      hasRole: (role) => {
        const state = get()
        return state.user?.role === role
      },
      hasAnyRole: (roles) => {
        if (roles.length === 0) return true
        const state = get()
        return state.user != null && roles.includes(state.user.role)
      },
    }),
    {
      name: 'scheduling-auth',
      partialize: (state) => ({ user: state.user, token: state.token }),
    },
  ),
)
