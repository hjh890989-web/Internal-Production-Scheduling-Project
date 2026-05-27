import { apiFetch } from './client'
import type { Role } from '@/stores/authStore'

/**
 * Sprint 12 EP-MASTER-UI 사용자 관리 API client (TK-MASTER-2-4, IT_OPS 권한).
 *
 * <p>모든 endpoint `/api/v1/master/user` — IT_OPS 만 호출. PLANNER/STK/READ_ONLY → 403 응답.
 */
export interface UserSummary {
  employeeId: string
  role: Role
  failedAttempts: number
  lockedUntil: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  employeeId: string
  pin: string
  role: Role
}

export const userAdminApi = {
  list: () => apiFetch<UserSummary[]>('/api/v1/master/user'),

  create: (payload: CreateUserRequest) =>
    apiFetch<UserSummary>('/api/v1/master/user', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),

  resetPin: (employeeId: string, newPin: string) =>
    apiFetch<void>(`/api/v1/master/user/${employeeId}/reset-pin`, {
      method: 'POST',
      body: JSON.stringify({ newPin }),
    }),

  unlock: (employeeId: string) =>
    apiFetch<void>(`/api/v1/master/user/${employeeId}/unlock`, {
      method: 'POST',
    }),

  delete: (employeeId: string) =>
    apiFetch<void>(`/api/v1/master/user/${employeeId}`, {
      method: 'DELETE',
    }),
}
