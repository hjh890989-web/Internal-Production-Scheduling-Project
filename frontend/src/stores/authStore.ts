import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/**
 * 인증 store — 로그인된 사용자 + JWT.
 * EP-30 (Keycloak) 통합 시 OIDC 토큰 갱신 로직 추가.
 */
export type Role = 'planner' | 'floor_supervisor' | 'it_operator' | 'read_only'

export interface AuthUser {
  id: string
  name: string
  role: Role
}

interface AuthState {
  user: AuthUser | null
  token: string | null
  setUser: (user: AuthUser, token: string) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      setUser: (user, token) => set({ user, token }),
      logout: () => set({ user: null, token: null }),
    }),
    {
      name: 'scheduling-auth',
      partialize: (state) => ({ user: state.user, token: state.token }),
    },
  ),
)
