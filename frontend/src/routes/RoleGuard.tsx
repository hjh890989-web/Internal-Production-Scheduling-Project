import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuthStore, type Role } from '@/stores/authStore'

/**
 * Sprint 11 EP-RBAC RoleGuard (TK-RBAC-3-3, NFR-SEC-003).
 *
 * <p>{@link useAuthStore#hasAnyRole} 체크 — 미충족 시 /forbidden redirect (replace).
 * 미인증 (token 없음) 은 상위 {@link ProtectedRoute} 가 /login 처리 — 본 Guard 는 인증된 사용자만 도달.
 *
 * <p>예: {@code <RoleGuard roles={['IT_OPS']}><MasterPage /></RoleGuard>} —
 * STK_USER/PLANNER/READ_ONLY 진입 시 /forbidden.
 */
export function RoleGuard({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const hasAnyRole = useAuthStore((s) => s.hasAnyRole(roles))

  if (!hasAnyRole) {
    return <Navigate to="/forbidden" replace />
  }

  return <>{children}</>
}
