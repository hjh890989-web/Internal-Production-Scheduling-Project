import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuthStore } from '@/stores/authStore'

/**
 * Sprint 10 EP-AUTH ProtectedRoute (TK-AUTH-5-4, NFR-SEC-007).
 *
 * <p>{@link useAuthStore#isAuthenticated} 체크 — 미인증 시 /login 으로 redirect (state.from 보존).
 * 토큰 만료 (expiresAt < now) 도 미인증으로 처리.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated())
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
