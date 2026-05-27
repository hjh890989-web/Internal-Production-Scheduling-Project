import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore, type AuthUser } from '../authStore'

/**
 * Sprint 10 EP-AUTH useAuthStore 단위 test (TK-AUTH-5-6).
 *
 * <p>setSession / logout / isAuthenticated (토큰 만료 가드) 3 가지 동작.
 */
describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.getState().logout()
    localStorage.clear()
  })

  const futureUser: AuthUser = {
    employeeId: '12345678',
    role: 'PLANNER',
    expiresAt: new Date(Date.now() + 8 * 3600 * 1000).toISOString(),
  }

  const pastUser: AuthUser = {
    employeeId: '12345678',
    role: 'PLANNER',
    expiresAt: new Date(Date.now() - 1000).toISOString(),
  }

  it('초기 상태 — user/token null + isAuthenticated false', () => {
    const state = useAuthStore.getState()
    expect(state.user).toBeNull()
    expect(state.token).toBeNull()
    expect(state.isAuthenticated()).toBe(false)
  })

  it('setSession — user/token 저장 + isAuthenticated true (exp 미래)', () => {
    useAuthStore.getState().setSession('jwt-token-xyz', futureUser)
    const state = useAuthStore.getState()
    expect(state.token).toBe('jwt-token-xyz')
    expect(state.user?.employeeId).toBe('12345678')
    expect(state.user?.role).toBe('PLANNER')
    expect(state.isAuthenticated()).toBe(true)
  })

  it('isAuthenticated — 토큰 만료 (exp 과거) 시 false (interceptor 401 전 1차 가드)', () => {
    useAuthStore.getState().setSession('jwt-expired', pastUser)
    expect(useAuthStore.getState().isAuthenticated()).toBe(false)
  })

  it('logout — user/token reset + isAuthenticated false', () => {
    useAuthStore.getState().setSession('jwt-token', futureUser)
    useAuthStore.getState().logout()
    const state = useAuthStore.getState()
    expect(state.user).toBeNull()
    expect(state.token).toBeNull()
    expect(state.isAuthenticated()).toBe(false)
  })

  // Sprint 11 ST-RBAC-3 — hasRole / hasAnyRole 추가 검증
  it('hasRole — 미인증 false / 일치 true / 불일치 false', () => {
    expect(useAuthStore.getState().hasRole('PLANNER')).toBe(false)
    useAuthStore.getState().setSession('jwt', futureUser)   // PLANNER
    expect(useAuthStore.getState().hasRole('PLANNER')).toBe(true)
    expect(useAuthStore.getState().hasRole('IT_OPS')).toBe(false)
  })

  it('hasAnyRole — 빈 배열 true / 포함 true / 미포함 false / 미인증 false (빈 배열 제외)', () => {
    expect(useAuthStore.getState().hasAnyRole([])).toBe(true)
    expect(useAuthStore.getState().hasAnyRole(['PLANNER'])).toBe(false)
    useAuthStore.getState().setSession('jwt', futureUser)   // PLANNER
    expect(useAuthStore.getState().hasAnyRole(['PLANNER', 'IT_OPS'])).toBe(true)
    expect(useAuthStore.getState().hasAnyRole(['IT_OPS', 'READ_ONLY'])).toBe(false)
  })
})
